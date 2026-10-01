package com.tutorhub.user.repository;

import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration Test cho {@link UserRepository}.
 * Kiểm tra các truy vấn CRUD, existsByEmail, findByRole và ràng buộc Unique email.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Lưu và tìm user theo email thành công")
    void shouldSaveAndFindUserByEmail() {
        User user = User.builder()
                .email("tutor@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .fullName("Nguyễn Văn A")
                .phone("0901234567")
                .role(Role.TUTOR)
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<User> found = userRepository.findByEmail("tutor@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(found.get().getRole()).isEqualTo(Role.TUTOR);
        assertThat(found.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("existsByEmail trả về true khi email đã tồn tại, false khi chưa")
    void shouldCheckEmailExistence() {
        User user = User.builder()
                .email("student@example.com")
                .passwordHash("hashed")
                .fullName("Trần Thị B")
                .role(Role.STUDENT)
                .build();
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("student@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("notfound@example.com")).isFalse();
    }

    @Test
    @DisplayName("findByRole trả về danh sách user theo vai trò")
    void shouldFindUsersByRole() {
        User tutor1 = User.builder()
                .email("tutor1@example.com")
                .passwordHash("pwd")
                .fullName("Gia sư 1")
                .role(Role.TUTOR)
                .build();
        User tutor2 = User.builder()
                .email("tutor2@example.com")
                .passwordHash("pwd")
                .fullName("Gia sư 2")
                .role(Role.TUTOR)
                .build();
        User parent = User.builder()
                .email("parent@example.com")
                .passwordHash("pwd")
                .fullName("Phụ huynh")
                .role(Role.PARENT)
                .build();

        userRepository.saveAll(List.of(tutor1, tutor2, parent));

        List<User> tutors = userRepository.findByRole(Role.TUTOR);
        assertThat(tutors).extracting(User::getEmail)
                .contains("tutor1@example.com", "tutor2@example.com")
                .doesNotContain("parent@example.com");
    }

    @Test
    @DisplayName("Ném DataIntegrityViolationException khi trùng email (Unique constraint)")
    void shouldFailWhenEmailIsDuplicate() {
        User u1 = User.builder()
                .email("dup@example.com")
                .passwordHash("pwd")
                .fullName("User 1")
                .role(Role.STUDENT)
                .build();
        userRepository.saveAndFlush(u1);

        User u2 = User.builder()
                .email("dup@example.com")
                .passwordHash("pwd")
                .fullName("User 2")
                .role(Role.PARENT)
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(u2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
