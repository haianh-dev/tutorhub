package com.tutorhub.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorhub.auth.dto.PasswordResetLinkResponse;
import com.tutorhub.auth.dto.RefreshRequest;
import com.tutorhub.auth.dto.ResetPasswordRequest;
import com.tutorhub.auth.entity.PasswordResetToken;
import com.tutorhub.auth.entity.RefreshToken;
import com.tutorhub.auth.repository.PasswordResetTokenRepository;
import com.tutorhub.auth.repository.RefreshTokenRepository;
import com.tutorhub.auth.service.AuthService;
import com.tutorhub.auth.service.JwtService;
import com.tutorhub.auth.service.PasswordResetService;
import com.tutorhub.common.exception.AppException;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthLifecycleIntegrationTest {

    private static final String ORIGINAL_PASSWORD = "OriginalPass123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordResetService passwordResetService;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM password_reset_tokens");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM parent_students");
        jdbcTemplate.update("DELETE FROM class_enrollments");
        jdbcTemplate.update("DELETE FROM classes");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    @DisplayName("Refresh rotates a token and rejects reuse of the old token")
    void refreshRotatesAndRejectsReuse() throws Exception {
        User user = createUser("refresh@example.com", Role.TUTOR);
        String oldRefreshToken = login(user);

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(oldRefreshToken))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn();

        String newRefreshToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
            .get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);
        assertThat(refreshToken(oldRefreshToken).getRevokedAt()).isNotNull();
        assertThat(refreshToken(newRefreshToken).getRevokedAt()).isNull();

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(oldRefreshToken))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
    }

            @Test
            @DisplayName("Expired refresh token is rejected")
            void expiredRefreshTokenIsRejected() throws Exception {
            User user = createUser("expired-refresh@example.com", Role.TUTOR);
            String rawToken = login(user);
            RefreshToken stored = refreshToken(rawToken);
            stored.setExpiresAt(Instant.now().minusSeconds(1));
            refreshTokenRepository.saveAndFlush(stored);

            mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(rawToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
            }

    @Test
    @DisplayName("Two concurrent requests cannot rotate the same refresh token")
    void concurrentRefreshOnlySucceedsOnce() throws Exception {
        User user = createUser("refresh-race@example.com", Role.TUTOR);
        String rawToken = login(user);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> attemptRefresh(rawToken, ready, start));
            Future<Boolean> second = executor.submit(() -> attemptRefresh(rawToken, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isNotEqualTo(second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Current-user endpoint requires a bearer token and returns only the current user's profile")
    void currentUserRequiresAuthentication() throws Exception {
        User user = createUser("me@example.com", Role.STUDENT);

        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId()))
            .andExpect(jsonPath("$.email").value(user.getEmail()))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Logout requires authentication and revokes only the caller's refresh token")
    void logoutIsAuthenticatedAndScopedToCaller() throws Exception {
        User owner = createUser("logout-owner@example.com", Role.TUTOR);
        User other = createUser("logout-other@example.com", Role.TUTOR);
        String refresh = login(owner);
        String body = objectMapper.writeValueAsString(new com.tutorhub.auth.dto.LogoutRequest(refresh));

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, bearer(other))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
        assertThat(refreshToken(refresh).getRevokedAt()).isNull();

        mockMvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNoContent());
        assertThat(refreshToken(refresh).getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("Changing password verifies current password and revokes all refresh sessions")
    void changePasswordVerifiesAndRevokesRefreshTokens() throws Exception {
        User user = createUser("change-password@example.com", Role.TUTOR);
        String oldRefreshToken = login(user);

        mockMvc.perform(put("/api/v1/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"NewPassword123!\"}"))
            .andExpect(status().isUnauthorized());
        assertThat(refreshToken(oldRefreshToken).getRevokedAt()).isNull();

        mockMvc.perform(put("/api/v1/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + ORIGINAL_PASSWORD
                    + "\",\"newPassword\":\"NewPassword123!\"}"))
            .andExpect(status().isNoContent());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewPassword123!", updated.getPasswordHash())).isTrue();
        assertThat(refreshToken(oldRefreshToken).getRevokedAt()).isNotNull();
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"change-password@example.com\",\"password\":\"" + ORIGINAL_PASSWORD + "\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tutor can reset an enrolled student's password once")
    void tutorResetsOwnedStudentPasswordOnce() throws Exception {
        User tutor = createUser("reset-tutor@example.com", Role.TUTOR);
        User student = createUser("reset-student@example.com", Role.STUDENT);
        enroll(tutor, student);
        String oldRefreshToken = login(student);

        PasswordResetLinkResponse link = createResetLink(tutor, student);
        String rawToken = tokenFromLink(link.link());
        PasswordResetToken storedToken = passwordResetTokenRepository
            .findByTokenHash(AuthService.hashToken(rawToken)).orElseThrow();
        assertThat(storedToken.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(passwordResetTokenRepository.findByTokenHash(rawToken)).isEmpty();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "ResetPass123!"))))
            .andExpect(status().isOk());

        User updated = userRepository.findById(student.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("ResetPass123!", updated.getPasswordHash())).isTrue();
        assertThat(refreshToken(oldRefreshToken).getRevokedAt()).isNotNull();
        assertThat(passwordResetTokenRepository.findByTokenHash(AuthService.hashToken(rawToken))
            .orElseThrow().getUsedAt()).isNotNull();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "AnotherPass123!"))))
            .andExpect(status().isGone());
    }

    @Test
    @DisplayName("Tutor may reset a parent linked to a student in the tutor's active class")
    void tutorResetsOwnedParentPassword() throws Exception {
        User tutor = createUser("parent-reset-tutor@example.com", Role.TUTOR);
        User student = createUser("parent-reset-student@example.com", Role.STUDENT);
        User parent = createUser("parent-reset-parent@example.com", Role.PARENT);
        enroll(tutor, student);
        jdbcTemplate.update("INSERT INTO parent_students(parent_id, student_id) VALUES (?, ?)", parent.getId(), student.getId());

        PasswordResetLinkResponse link = createResetLink(tutor, parent);
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ResetPasswordRequest(tokenFromLink(link.link()), "ParentReset123!"))))
            .andExpect(status().isOk());

        assertThat(passwordEncoder.matches("ParentReset123!", userRepository.findById(parent.getId())
            .orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Two concurrent requests cannot consume the same password-reset token")
    void concurrentResetOnlySucceedsOnce() throws Exception {
        User admin = createUser("reset-race-admin@example.com", Role.ADMIN);
        User student = createUser("reset-race-student@example.com", Role.STUDENT);
        String rawToken = tokenFromLink(createResetLink(admin, student).link());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> attemptPasswordReset(rawToken, ready, start));
            Future<Boolean> second = executor.submit(() -> attemptPasswordReset(rawToken, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isNotEqualTo(second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Tutor cannot create reset links outside owned student and parent relationships")
    void tutorCannotResetUnownedUsers() throws Exception {
        User tutor = createUser("owner-tutor@example.com", Role.TUTOR);
        User otherTutor = createUser("other-tutor@example.com", Role.TUTOR);
        User student = createUser("unowned-student@example.com", Role.STUDENT);
        User parent = createUser("unowned-parent@example.com", Role.PARENT);
        enroll(otherTutor, student);
        jdbcTemplate.update("INSERT INTO parent_students(parent_id, student_id) VALUES (?, ?)", parent.getId(), student.getId());

        mockMvc.perform(post("/api/v1/users/{id}/password-reset-link", student.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(tutor)))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/users/{id}/password-reset-link", parent.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(tutor)))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/users/{id}/password-reset-link", otherTutor.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(tutor)))
            .andExpect(status().isNotFound());
        assertThat(passwordResetTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("Only tutors and admins may create reset links; admins may target any user")
    void resetLinkRolePolicyAllowsAdminAndRejectsStudent() throws Exception {
        User admin = createUser("reset-admin@example.com", Role.ADMIN);
        User student = createUser("admin-reset-student@example.com", Role.STUDENT);
        User otherTutor = createUser("admin-reset-tutor@example.com", Role.TUTOR);

        mockMvc.perform(post("/api/v1/users/{id}/password-reset-link", student.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(student)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

        PasswordResetLinkResponse link = createResetLink(admin, otherTutor);
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ResetPasswordRequest(tokenFromLink(link.link()), "AdminReset123!"))))
            .andExpect(status().isOk());
        assertThat(passwordEncoder.matches("AdminReset123!", userRepository.findById(otherTutor.getId())
            .orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Expired and malformed password-reset tokens are rejected")
    void expiredAndMalformedResetTokensAreRejected() throws Exception {
        User admin = createUser("expired-admin@example.com", Role.ADMIN);
        User student = createUser("expired-student@example.com", Role.STUDENT);
        PasswordResetLinkResponse link = createResetLink(admin, student);
        String rawToken = tokenFromLink(link.link());
        PasswordResetToken token = passwordResetTokenRepository
            .findByTokenHash(AuthService.hashToken(rawToken)).orElseThrow();
        token.setExpiresAt(Instant.now().minusSeconds(1));
        passwordResetTokenRepository.saveAndFlush(token);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "ExpiredPass123!"))))
            .andExpect(status().isGone());
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest("not-a-token", "ExpiredPass123!"))))
            .andExpect(status().isGone());
    }

    private boolean attemptRefresh(String rawToken, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            authService.refresh(new RefreshRequest(rawToken));
            return true;
        } catch (AppException exception) {
            return false;
        }
    }

    private boolean attemptPasswordReset(String rawToken, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            passwordResetService.resetPassword(new ResetPasswordRequest(rawToken, "ConcurrentReset123!"));
            return true;
        } catch (AppException exception) {
            return false;
        }
    }

    private User createUser(String email, Role role) {
        return userRepository.save(User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(ORIGINAL_PASSWORD))
            .fullName(role.name() + " test")
            .role(role)
            .status(UserStatus.ACTIVE)
            .build());
    }

    private String login(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + ORIGINAL_PASSWORD + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("refreshToken").asText();
    }

    private RefreshToken refreshToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(AuthService.hashToken(rawToken)).orElseThrow();
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateAccessToken(user);
    }

    private Long enroll(User tutor, User student) {
        Long classId = jdbcTemplate.queryForObject(
            "INSERT INTO classes(tutor_id, name, subject, class_type) VALUES (?, ?, ?, 'GROUP') RETURNING id",
            Long.class,
            tutor.getId(),
            "Integration class",
            "Math"
        );
        jdbcTemplate.update("INSERT INTO class_enrollments(class_id, student_id) VALUES (?, ?)", classId, student.getId());
        return classId;
    }

    private PasswordResetLinkResponse createResetLink(User actor, User target) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users/{id}/password-reset-link", target.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(actor)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), PasswordResetLinkResponse.class);
    }

    private String tokenFromLink(String link) {
        return UriComponentsBuilder.fromUriString(link).build().getQueryParams().getFirst("token");
    }
}
