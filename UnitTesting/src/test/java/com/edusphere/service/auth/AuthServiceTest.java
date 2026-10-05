package com.edusphere.service.auth;

import com.edusphere.dto.RegisterRequest;
import com.edusphere.entity.*;
import com.edusphere.repository.*;
import com.edusphere.service.EmailService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ==============================================================
 * Unit Test: AuthService
 * ==============================================================
 *
 * Business Logic được kiểm tra:
 *
 * 1. initiateRegister(RegisterRequest)
 *    - Validate confirm password phải khớp với password
 *    - Email trùng + status ACTIVE → từ chối đăng ký
 *    - Email trùng + status PENDING → xóa user cũ, tạo lại (re-register)
 *    - Email chưa tồn tại → tạo user PENDING, gán role STUDENT, gửi OTP
 *    - User được lưu với status = "PENDING"
 *    - OTP được tạo 6 chữ số và có thời hạn 10 phút
 *    - Trả về email đã lowercase+trim
 *
 * 2. verifyOtp(String email, String otpInput)
 *    - Email không tồn tại → ném exception
 *    - OTP sai (không tìm thấy trong DB) → ném exception
 *    - OTP đúng nhưng không thuộc user này → ném exception
 *    - OTP đã được sử dụng (verifiedAt != null) → ném exception
 *    - OTP hết hạn (expiresAt < now) → ném exception
 *    - OTP hợp lệ → kích hoạt user (status = ACTIVE), đánh dấu OTP đã dùng
 *
 * 3. resendOtp(String email)
 *    - Email không tồn tại → ném exception
 *    - User đã ACTIVE → ném exception (không cần gửi lại)
 *    - User PENDING → xóa OTP cũ, tạo OTP mới, gửi email
 *
 * Framework: JUnit 5 + Mockito
 * ==============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    // ----------------------------------------------------------------
    // Mocks
    // ----------------------------------------------------------------
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /**
     * Tạo RegisterRequest hợp lệ dùng cho nhiều test case.
     */
    private RegisterRequest validRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Nguyen Van A");
        req.setEmail("test@example.com");
        req.setPassword("password123");
        req.setConfirmPassword("password123");
        req.setPhone("0912345678");
        return req;
    }

    /**
     * Tạo Users entity với trạng thái cho trước.
     */
    private Users buildUser(String email, String status) {
        Users user = new Users();
        user.setUserId(1);
        user.setEmail(email);
        user.setFullName("Test User");
        user.setStatus(status);
        user.setCreatedAt(new Date());
        return user;
    }

    /**
     * Tạo EmailVerificationTokens còn hiệu lực (chưa hết hạn, chưa dùng).
     */
    private EmailVerificationTokens buildValidOtpToken(Users user, String otp) {
        Date expiresAt = new Date(System.currentTimeMillis() + 10 * 60 * 1000); // 10 phút tới
        EmailVerificationTokens token = new EmailVerificationTokens(otp, expiresAt, user);
        return token;
    }

    // ================================================================
    // 1. initiateRegister
    // ================================================================

    @Nested
    @DisplayName("1. initiateRegister()")
    class InitiateRegisterTests {

        /**
         * TC-REG-01: Confirm password không khớp password
         * Input: password="abc12345", confirmPassword="different"
         * Expected: ném IllegalArgumentException với thông báo chứa "không khớp"
         */
        @Test
        @DisplayName("TC-REG-01: Confirm password không khớp → ném IllegalArgumentException")
        void tc_reg_01_passwordMismatch_throwsException() {
            RegisterRequest req = validRequest();
            req.setConfirmPassword("differentPassword");

            assertThatThrownBy(() -> authService.initiateRegister(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("không khớp");

            // Không được lưu gì vào DB
            verifyNoInteractions(userRepository, emailVerificationTokenRepository, emailService);
        }

        /**
         * TC-REG-02: Email đã tồn tại với status ACTIVE
         * Input: email đã có trong DB với status="ACTIVE"
         * Expected: ném IllegalArgumentException với thông báo chứa "đã được đăng ký"
         */
        @Test
        @DisplayName("TC-REG-02: Email đã tồn tại (ACTIVE) → ném IllegalArgumentException")
        void tc_reg_02_emailExistsActive_throwsException() {
            RegisterRequest req = validRequest();
            Users existingUser = buildUser(req.getEmail(), "ACTIVE");

            when(userRepository.existsByEmail(req.getEmail().toLowerCase())).thenReturn(true);
            when(userRepository.findByEmail(req.getEmail().toLowerCase())).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> authService.initiateRegister(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("đã được đăng ký");

            verify(userRepository, never()).save(any(Users.class));
        }

        /**
         * TC-REG-03: Email trùng nhưng status PENDING → xóa user cũ, tạo lại
         * Input: email tồn tại với status="PENDING" (chưa verify OTP)
         * Expected: xóa user cũ, tạo user mới, gửi OTP, trả về email
         */
        @Test
        @DisplayName("TC-REG-03: Email tồn tại (PENDING) → xóa và tạo lại user")
        void tc_reg_03_emailExistsPending_deletesOldAndCreatesNew() {
            RegisterRequest req = validRequest();
            Users pendingUser = buildUser(req.getEmail().toLowerCase(), "PENDING");

            when(userRepository.existsByEmail(req.getEmail().toLowerCase())).thenReturn(true);
            when(userRepository.findByEmail(req.getEmail().toLowerCase())).thenReturn(Optional.of(pendingUser));
            when(userRepository.save(any(Users.class))).thenAnswer(inv -> {
                Users u = inv.getArgument(0);
                u.setUserId(2);
                return u;
            });
            when(passwordEncoder.encode(anyString())).thenReturn("hashedPwd");
            Roles studentRole = new Roles();
            studentRole.setRoleId(1);
            studentRole.setRoleName("STUDENT");
            when(roleRepository.findByRoleName("STUDENT")).thenReturn(Optional.of(studentRole));

            String result = authService.initiateRegister(req);

            verify(emailVerificationTokenRepository).deleteByUserId(pendingUser);
            verify(userRepository).delete(pendingUser);
            verify(userRepository).flush();
            verify(emailService).sendOtpEmail(anyString(), anyString(), anyString());
            assertThat(result).isEqualTo(req.getEmail().toLowerCase());
        }

        /**
         * TC-REG-04: Đăng ký thành công với dữ liệu hợp lệ
         * Input: email mới, fullName, password hợp lệ
         * Expected: tạo user status=PENDING, gán role STUDENT, tạo OTP, gửi email, trả về email
         */
        @Test
        @DisplayName("TC-REG-04: Đăng ký hợp lệ → tạo user PENDING và gửi OTP")
        void tc_reg_04_validRequest_createsUserAndSendsOtp() {
            RegisterRequest req = validRequest();

            when(userRepository.existsByEmail(req.getEmail().toLowerCase())).thenReturn(false);
            when(userRepository.save(any(Users.class))).thenAnswer(inv -> {
                Users u = inv.getArgument(0);
                u.setUserId(1);
                return u;
            });
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            Roles studentRole = new Roles();
            studentRole.setRoleId(1);
            studentRole.setRoleName("STUDENT");
            when(roleRepository.findByRoleName("STUDENT")).thenReturn(Optional.of(studentRole));

            String returnedEmail = authService.initiateRegister(req);

            // Kiểm tra user được lưu với status PENDING
            ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getStatus()).isEqualTo("PENDING");

            // Kiểm tra OTP được lưu và email được gửi
            verify(emailVerificationTokenRepository).save(any(EmailVerificationTokens.class));
            verify(emailService).sendOtpEmail(eq(req.getEmail().toLowerCase()), anyString(), anyString());

            // Kiểm tra trả về email đúng (lowercase)
            assertThat(returnedEmail).isEqualTo(req.getEmail().toLowerCase());
        }

        /**
         * TC-REG-05: User được tạo với password đã hash (không lưu plaintext)
         * Input: password="myPassword123"
         * Expected: user.passwordHash != "myPassword123" (phải là kết quả encode)
         */
        @Test
        @DisplayName("TC-REG-05: Password được mã hóa trước khi lưu DB")
        void tc_reg_05_passwordIsEncoded() {
            RegisterRequest req = validRequest();
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("BCRYPT_HASH");
            when(userRepository.save(any(Users.class))).thenAnswer(inv -> {
                Users u = inv.getArgument(0);
                u.setUserId(1);
                return u;
            });
            Roles role = new Roles();
            role.setRoleId(1);
            role.setRoleName("STUDENT");
            when(roleRepository.findByRoleName("STUDENT")).thenReturn(Optional.of(role));

            authService.initiateRegister(req);

            ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("BCRYPT_HASH");
            assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("password123");
        }

        /**
         * TC-REG-06: Email được chuẩn hóa về lowercase khi lưu DB
         * Input: email = "TEST@EXAMPLE.COM"
         * Expected: user.email được lưu là "test@example.com"
         */
        @Test
        @DisplayName("TC-REG-06: Email được chuẩn hóa về lowercase")
        void tc_reg_06_emailNormalizedToLowercase() {
            RegisterRequest req = validRequest();
            req.setEmail("TEST@EXAMPLE.COM");

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(Users.class))).thenAnswer(inv -> {
                Users u = inv.getArgument(0);
                u.setUserId(1);
                return u;
            });
            Roles role = new Roles();
            role.setRoleId(1);
            role.setRoleName("STUDENT");
            when(roleRepository.findByRoleName("STUDENT")).thenReturn(Optional.of(role));

            String result = authService.initiateRegister(req);

            assertThat(result).isEqualTo("test@example.com");
        }

        /**
         * TC-REG-07: Role STUDENT không tồn tại → ném RuntimeException
         * Input: DB không có role "STUDENT"
         * Expected: ném RuntimeException
         */
        @Test
        @DisplayName("TC-REG-07: Role STUDENT không tồn tại → ném RuntimeException")
        void tc_reg_07_studentRoleNotFound_throwsRuntimeException() {
            RegisterRequest req = validRequest();
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(Users.class))).thenAnswer(inv -> {
                Users u = inv.getArgument(0);
                u.setUserId(1);
                return u;
            });
            when(roleRepository.findByRoleName("STUDENT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.initiateRegister(req))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ================================================================
    // 2. verifyOtp
    // ================================================================

    @Nested
    @DisplayName("2. verifyOtp()")
    class VerifyOtpTests {

        /**
         * TC-OTP-01: Email không tồn tại trong DB
         * Input: email="notexist@example.com"
         * Expected: ném IllegalArgumentException "Không tìm thấy tài khoản"
         */
        @Test
        @DisplayName("TC-OTP-01: Email không tồn tại → ném IllegalArgumentException")
        void tc_otp_01_emailNotFound_throwsException() {
            when(userRepository.findByEmail("notexist@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyOtp("notexist@example.com", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Không tìm thấy tài khoản");
        }

        /**
         * TC-OTP-02: OTP sai (không có trong DB)
         * Input: OTP = "000000" không tồn tại trong DB
         * Expected: ném IllegalArgumentException "không đúng"
         */
        @Test
        @DisplayName("TC-OTP-02: OTP không tồn tại trong DB → ném IllegalArgumentException")
        void tc_otp_02_otpNotFound_throwsException() {
            Users user = buildUser("test@example.com", "PENDING");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(emailVerificationTokenRepository.findByToken("000000")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyOtp("test@example.com", "000000"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("không đúng");
        }

        /**
         * TC-OTP-03: OTP thuộc user khác
         * Input: OTP hợp lệ nhưng được tạo cho user khác (userId = 99)
         * Expected: ném IllegalArgumentException "không hợp lệ"
         */
        @Test
        @DisplayName("TC-OTP-03: OTP không thuộc user này → ném IllegalArgumentException")
        void tc_otp_03_otpBelongsToOtherUser_throwsException() {
            Users user = buildUser("test@example.com", "PENDING");
            user.setUserId(1);

            Users otherUser = buildUser("other@example.com", "PENDING");
            otherUser.setUserId(99);

            EmailVerificationTokens token = buildValidOtpToken(otherUser, "123456");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(emailVerificationTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.verifyOtp("test@example.com", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("không hợp lệ");
        }

        /**
         * TC-OTP-04: OTP đã được sử dụng trước đó (verifiedAt != null)
         * Input: OTP hợp lệ nhưng verifiedAt đã có giá trị
         * Expected: ném IllegalArgumentException "đã được sử dụng"
         */
        @Test
        @DisplayName("TC-OTP-04: OTP đã được sử dụng → ném IllegalArgumentException")
        void tc_otp_04_otpAlreadyUsed_throwsException() {
            Users user = buildUser("test@example.com", "PENDING");
            user.setUserId(1);

            EmailVerificationTokens token = buildValidOtpToken(user, "123456");
            token.setVerifiedAt(new Date()); // Đã sử dụng

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(emailVerificationTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.verifyOtp("test@example.com", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("đã được sử dụng");
        }

        /**
         * TC-OTP-05: OTP hết hạn (expiresAt < now)
         * Input: OTP có expiresAt = 5 phút trước
         * Expected: ném IllegalArgumentException "hết hạn"
         */
        @Test
        @DisplayName("TC-OTP-05: OTP đã hết hạn → ném IllegalArgumentException")
        void tc_otp_05_otpExpired_throwsException() {
            Users user = buildUser("test@example.com", "PENDING");
            user.setUserId(1);

            Date expiredTime = new Date(System.currentTimeMillis() - 5 * 60 * 1000); // 5 phút trước
            EmailVerificationTokens token = new EmailVerificationTokens("123456", expiredTime, user);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(emailVerificationTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.verifyOtp("test@example.com", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("hết hạn");
        }

        /**
         * TC-OTP-06: OTP hợp lệ → kích hoạt tài khoản
         * Input: OTP đúng, chưa dùng, chưa hết hạn, đúng user
         * Expected: user.status = "ACTIVE", OTP.verifiedAt != null, user được lưu
         */
        @Test
        @DisplayName("TC-OTP-06: OTP hợp lệ → kích hoạt tài khoản (status = ACTIVE)")
        void tc_otp_06_validOtp_activatesUser() {
            Users user = buildUser("test@example.com", "PENDING");
            user.setUserId(1);

            EmailVerificationTokens token = buildValidOtpToken(user, "123456");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(emailVerificationTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));
            when(userRepository.save(any(Users.class))).thenReturn(user);
            when(emailVerificationTokenRepository.save(any(EmailVerificationTokens.class))).thenReturn(token);

            authService.verifyOtp("test@example.com", "123456");

            // User phải được set ACTIVE
            ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getStatus()).isEqualTo("ACTIVE");

            // OTP phải được đánh dấu đã dùng
            ArgumentCaptor<EmailVerificationTokens> tokenCaptor =
                    ArgumentCaptor.forClass(EmailVerificationTokens.class);
            verify(emailVerificationTokenRepository).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getVerifiedAt()).isNotNull();
        }
    }

    // ================================================================
    // 3. resendOtp
    // ================================================================

    @Nested
    @DisplayName("3. resendOtp()")
    class ResendOtpTests {

        /**
         * TC-RESEND-01: Email không tồn tại
         * Expected: ném IllegalArgumentException "Không tìm thấy tài khoản"
         */
        @Test
        @DisplayName("TC-RESEND-01: Email không tồn tại → ném IllegalArgumentException")
        void tc_resend_01_emailNotFound_throwsException() {
            when(userRepository.findByEmail("notexist@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resendOtp("notexist@example.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Không tìm thấy tài khoản");
        }

        /**
         * TC-RESEND-02: User đã ACTIVE → không cần gửi lại OTP
         * Expected: ném IllegalArgumentException "đã được xác minh"
         */
        @Test
        @DisplayName("TC-RESEND-02: User đã ACTIVE → ném IllegalArgumentException")
        void tc_resend_02_userAlreadyActive_throwsException() {
            Users user = buildUser("active@example.com", "ACTIVE");
            when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resendOtp("active@example.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("đã được xác minh");

            verifyNoInteractions(emailService);
        }

        /**
         * TC-RESEND-03: User PENDING → xóa OTP cũ, tạo OTP mới, gửi email
         * Expected: deleteByUserId được gọi, OTP mới được lưu, email được gửi
         */
        @Test
        @DisplayName("TC-RESEND-03: User PENDING → xóa OTP cũ, tạo OTP mới, gửi email")
        void tc_resend_03_pendingUser_resendSuccessfully() {
            Users user = buildUser("pending@example.com", "PENDING");
            when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user));

            authService.resendOtp("pending@example.com");

            verify(emailVerificationTokenRepository).deleteByUserId(user);
            verify(emailVerificationTokenRepository).save(any(EmailVerificationTokens.class));
            verify(emailService).sendOtpEmail(eq("pending@example.com"), anyString(), anyString());
        }
    }
}
