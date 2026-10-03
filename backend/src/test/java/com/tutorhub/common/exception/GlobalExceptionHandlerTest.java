package com.tutorhub.common.exception;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.tutorhub.auth.service.JwtService;
import com.tutorhub.user.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integration test cho {@link GlobalExceptionHandler}.
 *
 * <p>Dùng {@link WebMvcTest} với một controller giả ({@link FakeController})
 * để kiểm tra các loại lỗi trả đúng định dạng ProblemDetail RFC 7807.</p>
 *
 * <p><b>Luồng test:</b></p>
 * <ol>
 *   <li>FakeController ném exception (hoặc nhận DTO không hợp lệ).</li>
 *   <li>GlobalExceptionHandler bắt và chuyển thành ProblemDetail.</li>
 *   <li>Test kiểm tra JSON response đúng format.</li>
 * </ol>
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.FakeController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    // ═══════════════════════════════════════════════════════════════════
    //  Fake controller: chỉ dùng trong test, ném exception theo path
    // ═══════════════════════════════════════════════════════════════════

    @RestController
    @RequestMapping("/test-errors")
    static class FakeController {

        @GetMapping("/not-found")
        String triggerNotFound() {
            throw new ResourceNotFoundException("Class", 999L);
        }

        @GetMapping("/duplicate")
        String triggerDuplicate() {
            throw new DuplicateResourceException("User", "email", "a@example.com");
        }

        @GetMapping("/business-error")
        String triggerBusinessError() {
            throw new AppException(ErrorCode.CLASS_FULL,
                    "Lớp 1:1 đã đủ 1 học sinh.");
        }

        @GetMapping("/unexpected")
        String triggerUnexpected() {
            throw new RuntimeException("something went wrong internally");
        }

        @PostMapping("/validate")
        String triggerValidation(@Valid @RequestBody SampleRequest request) {
            return "ok";
        }
    }

    record SampleRequest(
            @NotBlank(message = "Tên không được để trống")
            @Size(min = 1, max = 100, message = "Tên phải từ 1 đến 100 ký tự")
            String name
    ) {
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Test cases
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("404 — ResourceNotFoundException")
    class NotFoundTests {

        @Test
        @WithMockUser
        @DisplayName("Trả ProblemDetail 404 với code RESOURCE_NOT_FOUND")
        void shouldReturn404WithCorrectFormat() throws Exception {
            mockMvc.perform(get("/test-errors/not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type", is("about:blank")))
                    .andExpect(jsonPath("$.title", is("Resource not found")))
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.detail", is("Class not found with identifier: 999")))
                    .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
        }
    }

    @Nested
    @DisplayName("409 — DuplicateResourceException")
    class DuplicateTests {

        @Test
        @WithMockUser
        @DisplayName("Trả ProblemDetail 409 với code DUPLICATE_RESOURCE")
        void shouldReturn409WithCorrectFormat() throws Exception {
            mockMvc.perform(get("/test-errors/duplicate"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type", is("about:blank")))
                    .andExpect(jsonPath("$.title", is("Resource already exists")))
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.detail", is("User already exists with email: a@example.com")))
                    .andExpect(jsonPath("$.code", is("DUPLICATE_RESOURCE")));
        }
    }

    @Nested
    @DisplayName("422 — AppException (CLASS_FULL)")
    class BusinessErrorTests {

        @Test
        @WithMockUser
        @DisplayName("Trả ProblemDetail 422 với code CLASS_FULL")
        void shouldReturn422WithCorrectFormat() throws Exception {
            mockMvc.perform(get("/test-errors/business-error"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.type", is("about:blank")))
                    .andExpect(jsonPath("$.title", is("Class enrollment limit reached")))
                    .andExpect(jsonPath("$.status", is(422)))
                    .andExpect(jsonPath("$.detail", is("Lớp 1:1 đã đủ 1 học sinh.")))
                    .andExpect(jsonPath("$.code", is("CLASS_FULL")));
        }
    }

    @Nested
    @DisplayName("400 — Validation Error")
    class ValidationTests {

        @Test
        @WithMockUser
        @DisplayName("Trả ProblemDetail 400 với code VALIDATION_ERROR + danh sách errors")
        void shouldReturn400WithFieldErrors() throws Exception {
            String invalidJson = """
                    {"name": ""}
                    """;

            mockMvc.perform(post("/test-errors/validate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type", is("about:blank")))
                    .andExpect(jsonPath("$.title", is("Validation failed")))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors").isNotEmpty())
                    .andExpect(jsonPath("$.errors[0].field").isString())
                    .andExpect(jsonPath("$.errors[0].message").isString());
        }

        @Test
        @WithMockUser
        @DisplayName("Gửi body rỗng cũng trả 400")
        void shouldReturn400ForMissingBody() throws Exception {
            String nullNameJson = """
                    {}
                    """;

            mockMvc.perform(post("/test-errors/validate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(nullNameJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }
    }

    @Nested
    @DisplayName("500 — Unexpected Error")
    class GenericErrorTests {

        @Test
        @WithMockUser
        @DisplayName("Trả ProblemDetail 500, ẩn stack trace, code INTERNAL_ERROR")
        void shouldReturn500AndHideStackTrace() throws Exception {
            mockMvc.perform(get("/test-errors/unexpected"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.type", is("about:blank")))
                    .andExpect(jsonPath("$.title", is("Internal server error")))
                    .andExpect(jsonPath("$.status", is(500)))
                    .andExpect(jsonPath("$.code", is("INTERNAL_ERROR")))
                    // Không lộ stack trace hoặc message gốc
                    .andExpect(jsonPath("$.detail",
                            is("Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.")));
        }
    }
}
