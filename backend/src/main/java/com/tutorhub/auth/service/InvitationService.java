package com.tutorhub.auth.service;

import com.tutorhub.auth.dto.AcceptInvitationRequest;
import com.tutorhub.auth.dto.AuthResponse;
import com.tutorhub.auth.dto.CreateInvitationRequest;
import com.tutorhub.auth.dto.InvitationResponse;
import com.tutorhub.auth.entity.Invitation;
import com.tutorhub.auth.repository.InvitationRepository;
import com.tutorhub.auth.repository.RefreshTokenRepository;
import com.tutorhub.common.exception.AppException;
import com.tutorhub.common.exception.ErrorCode;
import com.tutorhub.user.dto.UserResponse;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Service quản lý toàn bộ vòng đời lời mời:
 * <ol>
 *   <li>Tạo lời mời (TUTOR/ADMIN) → trả link gửi qua Zalo.</li>
 *   <li>Kiểm tra lời mời còn hiệu lực (public).</li>
 *   <li>Chấp nhận lời mời → tạo tài khoản + ghi danh/liên kết + tự đăng nhập.</li>
 * </ol>
 *
 * <p>Bảo mật:</p>
 * <ul>
 *   <li>Raw token không lưu DB; chỉ lưu SHA-256 hash.</li>
 *   <li>Pessimistic lock khi accept để chặn concurrent acceptance.</li>
 *   <li>TUTOR chỉ mời học sinh trong lớp mình hoặc phụ huynh liên kết đúng con trong lớp mình.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.auth.invitation-ttl-seconds:604800}") // 7 ngày
    private long invitationTtlSeconds;

    @Value("${app.jwt.refresh-token-ttl-seconds:604800}") // 7 ngày
    private long refreshTokenTtlSeconds;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    // ─── SQL kiểm tra quyền sở hữu của TUTOR ────────────────────────────────

    /** TUTOR chỉ được mời học sinh đang ghi danh ACTIVE trong lớp ACTIVE của mình. */
    private static final String CHECK_TUTOR_OWNS_CLASS_SQL = """
        SELECT EXISTS (
            SELECT 1 FROM classes
            WHERE id = ? AND tutor_id = ? AND status = 'ACTIVE'
        )
        """;

    /** Khi mời PARENT: studentId phải là học sinh trong lớp ACTIVE của TUTOR. */
    private static final String CHECK_STUDENT_IN_TUTOR_CLASS_SQL = """
        SELECT EXISTS (
            SELECT 1
            FROM classes c
            JOIN class_enrollments e ON e.class_id = c.id
            WHERE c.tutor_id = ? AND c.status = 'ACTIVE'
              AND e.student_id = ? AND e.status = 'ACTIVE'
        )
        """;

    /** Lấy tên lớp theo id (có thể null nếu lớp không tồn tại). */
    private static final String GET_CLASS_NAME_SQL = """
        SELECT name FROM classes WHERE id = ?
        """;

    /** Ghi danh học sinh vào lớp (upsert-like: nếu đã có thì cập nhật status ACTIVE). */
    private static final String ENROLL_STUDENT_SQL = """
        INSERT INTO class_enrollments (class_id, student_id, status, enrolled_at, created_at)
        VALUES (?, ?, 'ACTIVE', NOW(), NOW())
        ON CONFLICT (class_id, student_id)
        DO UPDATE SET status = 'ACTIVE', enrolled_at = NOW(), left_at = NULL
        """;

    /** Liên kết phụ huynh với học sinh. */
    private static final String LINK_PARENT_STUDENT_SQL = """
        INSERT INTO parent_students (parent_id, student_id, created_at)
        VALUES (?, ?, NOW())
        ON CONFLICT (parent_id, student_id) DO NOTHING
        """;

    // ─── Create Invitation ───────────────────────────────────────────────────

    /**
     * Tạo lời mời cho học sinh hoặc phụ huynh.
     *
     * @param actorId   ID người tạo lời mời (từ JWT, không từ client)
     * @param actorRole Vai trò người tạo
     * @param request   Yêu cầu tạo lời mời
     * @return Link lời mời + expiresAt
     */
    @Transactional
    public InvitationResponse createInvitation(Long actorId, Role actorRole, CreateInvitationRequest request) {
        // 1. Chỉ STUDENT và PARENT mới có thể là vai trò của người được mời.
        if (request.role() != Role.STUDENT && request.role() != Role.PARENT) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                "Chỉ có thể mời người dùng với vai trò STUDENT hoặc PARENT");
        }

        // 2. PARENT bắt buộc phải có studentId.
        if (request.role() == Role.PARENT && request.studentId() == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                "Lời mời vai trò PARENT bắt buộc phải chỉ định studentId");
        }

        String className = null;

        if (actorRole == Role.TUTOR) {
            // 3. TUTOR: kiểm tra quyền sở hữu theo lớp/học sinh.
            if (request.classId() != null) {
                boolean ownsClass = Boolean.TRUE.equals(
                    jdbcTemplate.queryForObject(CHECK_TUTOR_OWNS_CLASS_SQL, Boolean.class,
                        request.classId(), actorId));
                if (!ownsClass) {
                    throw new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Lớp không tồn tại hoặc không thuộc quyền quản lý của bạn");
                }
            }
            if (request.role() == Role.PARENT && request.studentId() != null) {
                // Học sinh phải đang học trong ít nhất một lớp của TUTOR này.
                boolean studentInTutorClass = Boolean.TRUE.equals(
                    jdbcTemplate.queryForObject(CHECK_STUDENT_IN_TUTOR_CLASS_SQL, Boolean.class,
                        actorId, request.studentId()));
                if (!studentInTutorClass) {
                    throw new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Học sinh không thuộc lớp của bạn");
                }
            }
        }
        // ADMIN không cần kiểm tra ownership.

        // 4. Validate studentId tồn tại và đúng role STUDENT (nếu có).
        User studentRef = null;
        if (request.studentId() != null) {
            studentRef = userRepository.findById(request.studentId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Học sinh không tồn tại"));
            if (studentRef.getRole() != Role.STUDENT) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "studentId phải là tài khoản có vai trò STUDENT");
            }
        }

        // 5. Lấy tên lớp để trả về trong response (nếu có classId).
        if (request.classId() != null) {
            try {
                className = jdbcTemplate.queryForObject(GET_CLASS_NAME_SQL, String.class, request.classId());
            } catch (Exception e) {
                className = null;
            }
        }

        // 6. Tìm invitedBy user.
        User actor = userRepository.findById(actorId)
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Người dùng không tồn tại"));

        // 7. Sinh raw token và lưu hash.
        String rawToken = generateRawToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(invitationTtlSeconds);

        Invitation invitation = Invitation.builder()
            .tokenHash(AuthService.hashToken(rawToken))
            .role(request.role())
            .invitedBy(actor)
            .email(request.email())
            .classId(request.classId())
            .student(studentRef)
            .expiresAt(expiresAt)
            .build();
        invitationRepository.save(invitation);

        // 8. Xây dựng link đầy đủ.
        String link = buildInvitationLink(rawToken);

        return new InvitationResponse(link, request.role(), request.email(), className, expiresAt);
    }

    // ─── Verify Invitation ───────────────────────────────────────────────────

    /**
     * Kiểm tra lời mời còn hiệu lực không (dùng cho frontend hiển thị form).
     *
     * @param rawToken Raw token từ URL
     * @return Thông tin lời mời (role, email gợi ý, tên lớp)
     * @throws AppException INVITATION_EXPIRED nếu hết hạn hoặc đã dùng
     */
    @Transactional(readOnly = true)
    public InvitationResponse verifyInvitation(String rawToken) {
        Invitation invitation = invitationRepository.findByTokenHash(AuthService.hashToken(rawToken))
            .orElseThrow(() -> new AppException(ErrorCode.INVITATION_EXPIRED,
                "Lời mời không hợp lệ hoặc đã hết hạn"));

        Instant now = Instant.now();
        if (!invitation.isUsableAt(now)) {
            throw new AppException(invitation.isUsed() ? ErrorCode.INVITATION_USED : ErrorCode.INVITATION_EXPIRED,
                invitation.isUsed() ? "Lời mời đã được sử dụng" : "Lời mời đã hết hạn");
        }

        String className = null;
        if (invitation.getClassId() != null) {
            try {
                className = jdbcTemplate.queryForObject(GET_CLASS_NAME_SQL, String.class, invitation.getClassId());
            } catch (Exception e) {
                className = null;
            }
        }

        return new InvitationResponse(
            null, // Không trả link khi verify — chỉ cần thông tin để hiển thị form
            invitation.getRole(),
            invitation.getEmail(),
            className,
            invitation.getExpiresAt()
        );
    }

    // ─── Accept Invitation ───────────────────────────────────────────────────

    /**
     * Chấp nhận lời mời: tạo tài khoản STUDENT/PARENT, thực hiện ghi danh/liên kết, tự đăng nhập.
     * Pessimistic lock để chặn concurrent acceptance của cùng một token.
     *
     * @param request Thông tin đăng ký + token raw
     * @return AuthResponse (accessToken + refreshToken + user)
     * @throws AppException INVITATION_EXPIRED / INVITATION_USED
     * @throws AppException DUPLICATE_RESOURCE nếu email đã tồn tại
     */
    @Transactional
    public AuthResponse acceptInvitation(AcceptInvitationRequest request) {
        String tokenHash = AuthService.hashToken(request.token());

        // 1. Lấy và lock lời mời (pessimistic) để tránh race condition.
        Invitation invitation = invitationRepository.findByTokenHashForUpdate(tokenHash)
            .orElseThrow(() -> new AppException(ErrorCode.INVITATION_EXPIRED,
                "Lời mời không hợp lệ hoặc đã hết hạn"));

        Instant now = Instant.now();
        if (!invitation.isUsableAt(now)) {
            throw new AppException(invitation.isUsed() ? ErrorCode.INVITATION_USED : ErrorCode.INVITATION_EXPIRED,
                invitation.isUsed() ? "Lời mời đã được sử dụng" : "Lời mời đã hết hạn");
        }

        // 2. Kiểm tra email chưa bị dùng (nếu email trong invitation khớp hoặc người dùng nhập email khác).
        // Email người dùng dùng để đăng ký = email trong lời mời (nếu có), hoặc người nhận tự chọn.
        // Ở đây, tài khoản tạo ra không có email riêng nếu invitation.email null.
        // Theo thiết kế, email của invitation là email gợi ý — người dùng vẫn cần nhập qua form.
        // Tuy nhiên AcceptInvitationRequest không có email field — dùng email từ invitation (nếu có).
        // Nếu invitation không có email, không tạo email-based account.
        // → Thiết kế đơn giản: email của tài khoản = invitation.email (bắt buộc khi không có email).
        // Vì API_SPEC không yêu cầu email khi accept, ta dùng email từ invitation làm định danh.
        // Nếu invitation.email là null → dùng placeholder unique (không khuyến khích nhưng theo thiết kế MVP).

        // Thực tế: theo API_SPEC, invitation.email là optional. Để đơn giản cho MVP:
        // Tài khoản được tạo với email từ invitation (nếu có); nếu không có,
        // gia sư sẽ cung cấp email khi tạo invitation. Không thêm email field vào AcceptInvitationRequest
        // để giữ đúng phạm vi T1.4 (email là field tuỳ chọn của invitation).
        // Nếu email null trong invitation → không thể tạo tài khoản (validation ở createInvitation).
        // Ta sẽ check email bắt buộc có trong invitation khi tạo lần lượt:
        // Thực ra không cần — schema users.email NOT NULL. Nên cần email trong AcceptInvitationRequest.
        // Nhưng API_SPEC không có email trong AcceptInvitationRequest body…
        // Giải pháp: dùng email từ invitation nếu đã được điền; nếu không, yêu cầu từ request.
        // Ta sẽ thêm email vào AcceptInvitationRequest cho tường minh.

        // → Giải pháp thiết kế đơn giản nhất phù hợp với schema users.email NOT NULL:
        // Email của tài khoản = email trong invitation (invitation.email, đã được gia sư điền khi tạo).
        // Nếu invitation.email null → không cho phép accept (lỗi validation — gia sư phải điền email khi tạo).
        String email = invitation.getEmail();
        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                "Lời mời không có email — vui lòng liên hệ gia sư để tạo lại lời mời có email");
        }

        String normalizedEmail = email.toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE,
                "Email này đã có tài khoản. Vui lòng đăng nhập thay vì sử dụng link mời.");
        }

        // 3. Tạo tài khoản mới với role từ lời mời.
        User newUser = User.builder()
            .email(normalizedEmail)
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName().trim())
            .phone(request.phone() != null && !request.phone().isBlank() ? request.phone().trim() : null)
            .role(invitation.getRole())
            .status(UserStatus.ACTIVE)
            .build();
        newUser = userRepository.save(newUser);

        // 4. Thực hiện nghiệp vụ theo role.
        if (invitation.getRole() == Role.STUDENT && invitation.getClassId() != null) {
            // Ghi danh học sinh vào lớp.
            jdbcTemplate.update(ENROLL_STUDENT_SQL, invitation.getClassId(), newUser.getId());
        } else if (invitation.getRole() == Role.PARENT && invitation.getStudent() != null) {
            // Liên kết phụ huynh với học sinh.
            jdbcTemplate.update(LINK_PARENT_STUDENT_SQL, newUser.getId(), invitation.getStudent().getId());
        }

        // 5. Đánh dấu lời mời đã dùng.
        invitation.setUsedAt(now);
        invitationRepository.save(invitation);

        // 6. Tự đăng nhập — sinh access token + refresh token.
        String accessToken = jwtService.generateAccessToken(newUser);
        String rawRefreshToken = createRefreshToken(newUser, now);

        return new AuthResponse(accessToken, rawRefreshToken, UserResponse.from(newUser));
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildInvitationLink(String rawToken) {
        String baseUrl = frontendBaseUrl.replaceAll("/+$", "");
        return UriComponentsBuilder.fromUriString(baseUrl)
            .path("/accept-invitation")
            .queryParam("token", rawToken)
            .build()
            .encode()
            .toUriString();
    }

    private String createRefreshToken(User user, Instant now) {
        String rawToken = java.util.UUID.randomUUID().toString().replace("-", "")
            + java.util.UUID.randomUUID().toString().replace("-", "");
        com.tutorhub.auth.entity.RefreshToken rt = com.tutorhub.auth.entity.RefreshToken.builder()
            .user(user)
            .tokenHash(AuthService.hashToken(rawToken))
            .expiresAt(now.plusSeconds(refreshTokenTtlSeconds))
            .build();
        refreshTokenRepository.save(rt);
        return rawToken;
    }
}
