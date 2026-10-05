package com.edusphere.service.auth;

import com.edusphere.entity.PasswordResetTokens;
import com.edusphere.entity.Users;
import com.edusphere.repository.PasswordResetTokenRepository;
import com.edusphere.repository.UserRepository;
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
 * Unit Test: PasswordResetService
 * ==============================================================
 *
 * Business Logic được kiểm tra:
 *
 * 1. processForgotPassword(String email)
 *    - Email không tồn tại → return silently (không tiết lộ)
 *    - Email tồn tại → xóa OTP cũ, tạo OTP mới, gửi email
 *
 * 2. verifyResetOtp(String email, String otp)
 *    - Email không tồn tại → ném exception
 *    - OTP sai → ném exception
 *    - OTP không thuộc user này → ném exception
 *    - OTP đã được sử dụng (usedAt != null) → ném exception
 *    - OTP hết hạn → ném exception
 *    - OTP hợp lệ → trả về email của user
 *
 * 3. resetPassword(String email, String newPassword, String confirmPassword)
 *    - Confirm password không khớp → ném exception
 *    - Email không tồn tại → ném exception
 *    - Không có token hợp lệ → ném exception "phiên đặt lại hết hạn"
 *    - Hợp lệ → cập nhật passwordHash, đánh dấu token đã dùng
 *
 * 4. resendResetOtp(String email)
 *    - Delegate sang processForgotPassword (kiểm tra không gửi khi email không tồn tại)
 *
 * Framework: JUnit 5 + Mockito
 * ==============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService Unit Tests")
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

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
     * Tạo PasswordResetToken còn hiệu lực (chưa dùng, chưa hết hạn).
     */
    private PasswordResetTokens buildValidResetToken(Users user, String otp) {
        Date expiresAt = new Date(System.currentTimeMillis() + 10 * 60 * 1000);
        return new PasswordResetTokens(otp, expiresAt, user);
    }

    // ================================================================
    // 1. processForgotPassword
    // ================================================================

    @Nested
    @DisplayName("1. processForgotPassword()")
    class ProcessForgotPasswordTests {

        /**
         * TC-FP-01: Email không tồn tại trong DB → không làm gì (không tiết lộ thông tin)
         * Input: email không có trong DB
         * Expected: return bình thường, không gọi emailService, không tạo token
         *
         * Lý do bảo mật: không để attacker biết email nào đã đăng ký.
         */
        @Test
        @DisplayName("TC-FP-01: Email không tồn tại → không gửi email (bảo mật)")
        void tc_fp_01_emailNotFound_returnsSilently() {
            when(userRepository.findByEmail("notexist@example.com")).thenReturn(Optional.empty());

            // Không được ném exception
            assertThatCode(() -> passwordResetService.processForgotPassword("notexist@example.com"))
                    .doesNotThrowAnyException();

            // Không được tương tác với email service hay tạo token
            verifyNoInteractions(emailService, passwordResetTokenRepository);
        }

        /**
         * TC-FP-02: Email tồn tại → xóa OTP cũ, tạo OTP mới, gửi email
         * Input: email hợp lệ có trong DB
         * Expected: deleteByUserId được gọi, token được lưu, email được gửi
         */
        @Test
        @DisplayName("TC-FP-02: Email tồn tại → xóa OTP cũ, tạo OTP mới, gửi email")
        void tc_fp_02_emailFound_createsTokenAndSendsEmail() {
            Users user = buildUser("user@example.com", "ACTIVE");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            passwordResetService.processForgotPassword("user@example.com");

            verify(passwordResetTokenRepository).deleteByUserId(user);
            verify(passwordResetTokenRepository).save(any(PasswordResetTokens.class));
            verify(emailService).sendPasswordResetOtpEmail(
                    eq("user@example.com"), anyString(), anyString());
        }

        /**
         * TC-FP-03: Token được tạo với thời hạn ~10 phút
         * Input: email hợp lệ
         * Expected: expiresAt của token ≈ now + 10 phút (±5 giây để tránh race condition)
         */
        @Test
        @DisplayName("TC-FP-03: OTP token có thời hạn 10 phút")
        void tc_fp_03_tokenHasTenMinuteExpiry() {
            Users user = buildUser("user@example.com", "ACTIVE");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            long beforeCall = System.currentTimeMillis();
            passwordResetService.processForgotPassword("user@example.com");
            long afterCall = System.currentTimeMillis();

            ArgumentCaptor<PasswordResetTokens> captor = ArgumentCaptor.forClass(PasswordResetTokens.class);
            verify(passwordResetTokenRepository).save(captor.capture());

            long expiresAtMs = captor.getValue().getExpiresAt().getTime();
            long expectedMin = beforeCall + 10 * 60 * 1000 - 5000;
            long expectedMax = afterCall  + 10 * 60 * 1000 + 5000;

            assertThat(expiresAtMs).isBetween(expectedMin, expectedMax);
        }
    }

    // ================================================================
    // 2. verifyResetOtp
    // ================================================================

    @Nested
    @DisplayName("2. verifyResetOtp()")
    class VerifyResetOtpTests {

        /**
         * TC-VRO-01: Email không tồn tại → ném IllegalArgumentException
         */
        @Test
        @DisplayName("TC-VRO-01: Email không tồn tại → ném IllegalArgumentException")
        void tc_vro_01_emailNotFound_throwsException() {
            when(userRepository.findByEmail("notexist@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.verifyResetOtp("notexist@example.com", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Không tìm thấy tài khoản");
        }

        /**
         * TC-VRO-02: OTP sai → ném IllegalArgumentException
         * Input: OTP không có trong DB
         * Expected: "Mã OTP không đúng"
         */
        @Test
        @DisplayName("TC-VRO-02: OTP không tồn tại trong DB → ném IllegalArgumentException")
        void tc_vro_02_otpNotFound_throwsException() {
            Users user = buildUser("user@example.com", "ACTIVE");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findByToken("000000")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.verifyResetOtp("user@example.com", "000000"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("không đúng");
        }

        /**
         * TC-VRO-03: OTP thuộc user khác → ném IllegalArgumentException
         */
        @Test
        @DisplayName("TC-VRO-03: OTP không thuộc user này → ném IllegalArgumentException")
        void tc_vro_03_otpBelongsToOtherUser_throwsException() {
            Users user = buildUser("user@example.com", "ACTIVE");
            user.setUserId(1);

            Users otherUser = buildUser("other@example.com", "ACTIVE");
            otherUser.setUserId(99);

            PasswordResetTokens token = buildValidResetToken(otherUser, "123456");

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> passwordResetService.verifyResetOtp("user@example.com", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("không hợp lệ");
        }

        /**
         * TC-VRO-04: OTP đã được sử dụng (usedAt != null) → ném exception
         */
        @Test
        @DisplayName("TC-VRO-04: OTP đã được sử dụng → ném IllegalArgumentException")
        void tc_vro_04_otpAlreadyUsed_throwsException() {
            Users user = buildUser("user@example.com", "ACTIVE");
            user.setUserId(1);

            PasswordResetTokens token = buildValidResetToken(user, "123456");
            token.setUsedAt(new Date()); // Đã dùng

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> passwordResetService.verifyResetOtp("user@example.com", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("đã được sử dụng");
        }

        /**
         * TC-VRO-05: OTP hết hạn → ném IllegalArgumentException
         */
        @Test
        @DisplayName("TC-VRO-05: OTP đã hết hạn → ném IllegalArgumentException")
        void tc_vro_05_otpExpired_throwsException() {
            Users user = buildUser("user@example.com", "ACTIVE");
            user.setUserId(1);

            Date expiredTime = new Date(System.currentTimeMillis() - 60_000); // 1 phút trước
            PasswordResetTokens token = new PasswordResetTokens("123456", expiredTime, user);

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> passwordResetService.verifyResetOtp("user@example.com", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("hết hạn");
        }

        /**
         * TC-VRO-06: OTP hợp lệ → trả về email của user
         * Input: OTP đúng, chưa dùng, chưa hết hạn, đúng user
         * Expected: trả về "user@example.com"
         */
        @Test
        @DisplayName("TC-VRO-06: OTP hợp lệ → trả về email user")
        void tc_vro_06_validOtp_returnsEmail() {
            Users user = buildUser("user@example.com", "ACTIVE");
            user.setUserId(1);

            PasswordResetTokens token = buildValidResetToken(user, "123456");

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

            String result = passwordResetService.verifyResetOtp("user@example.com", "123456");

            assertThat(result).isEqualTo("user@example.com");
        }
    }

    // ================================================================
    // 3. resetPassword
    // ================================================================

    @Nested
    @DisplayName("3. resetPassword()")
    class ResetPasswordTests {

        /**
         * TC-RP-01: Confirm password không khớp → ném IllegalArgumentException
         */
        @Test
        @DisplayName("TC-RP-01: Confirm password không khớp → ném IllegalArgumentException")
        void tc_rp_01_passwordMismatch_throwsException() {
            assertThatThrownBy(() ->
                    passwordResetService.resetPassword("user@example.com", "newPass123", "different"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("không khớp");

            verifyNoInteractions(userRepository);
        }

        /**
         * TC-RP-02: Email không tồn tại → ném IllegalArgumentException
         */
        @Test
        @DisplayName("TC-RP-02: Email không tồn tại → ném IllegalArgumentException")
        void tc_rp_02_emailNotFound_throwsException() {
            when(userRepository.findByEmail("notexist@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    passwordResetService.resetPassword("notexist@example.com", "newPass123", "newPass123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Không tìm thấy tài khoản");
        }

        /**
         * TC-RP-03: Không có token hợp lệ (hết hạn hoặc đã dùng) → ném exception "phiên đặt lại hết hạn"
         */
        @Test
        @DisplayName("TC-RP-03: Không có token hợp lệ → ném IllegalArgumentException")
        void tc_rp_03_noValidToken_throwsException() {
            Users user = buildUser("user@example.com", "ACTIVE");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findLatestValidByUserId(user)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    passwordResetService.resetPassword("user@example.com", "newPass123", "newPass123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("hết hạn");
        }

        /**
         * TC-RP-04: Hợp lệ → cập nhật password hash, đánh dấu token đã dùng
         * Input: dữ liệu hợp lệ đầy đủ
         * Expected: passwordHash được update, token.usedAt != null
         */
        @Test
        @DisplayName("TC-RP-04: Hợp lệ → cập nhật passwordHash và đánh dấu token đã dùng")
        void tc_rp_04_validData_updatesPasswordAndMarksToken() {
            Users user = buildUser("user@example.com", "ACTIVE");
            PasswordResetTokens token = buildValidResetToken(user, "123456");

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findLatestValidByUserId(user)).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("newPass123")).thenReturn("NEW_HASH");
            when(userRepository.save(any(Users.class))).thenReturn(user);
            when(passwordResetTokenRepository.save(any(PasswordResetTokens.class))).thenReturn(token);

            passwordResetService.resetPassword("user@example.com", "newPass123", "newPass123");

            // Kiểm tra password hash được cập nhật
            ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("NEW_HASH");

            // Kiểm tra token được đánh dấu đã dùng
            ArgumentCaptor<PasswordResetTokens> tokenCaptor =
                    ArgumentCaptor.forClass(PasswordResetTokens.class);
            verify(passwordResetTokenRepository).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getUsedAt()).isNotNull();
        }

        /**
         * TC-RP-05: Password mới không được lưu dạng plaintext
         * Input: newPassword="newPass123"
         * Expected: user.passwordHash != "newPass123" (phải là giá trị sau encode)
         */
        @Test
        @DisplayName("TC-RP-05: Password mới được mã hóa, không lưu dạng plaintext")
        void tc_rp_05_newPasswordIsEncoded() {
            Users user = buildUser("user@example.com", "ACTIVE");
            PasswordResetTokens token = buildValidResetToken(user, "123456");

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findLatestValidByUserId(user)).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("newPass123")).thenReturn("BCRYPT_HASH_NEW");
            when(userRepository.save(any(Users.class))).thenReturn(user);
            when(passwordResetTokenRepository.save(any())).thenReturn(token);

            passwordResetService.resetPassword("user@example.com", "newPass123", "newPass123");

            ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("newPass123");
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("BCRYPT_HASH_NEW");
        }
    }

    // ================================================================
    // 4. resendResetOtp
    // ================================================================

    @Nested
    @DisplayName("4. resendResetOtp()")
    class ResendResetOtpTests {

        /**
         * TC-RRO-01: Email không tồn tại → không gửi email (bảo mật)
         * resendResetOtp delegates sang processForgotPassword
         * Expected: emailService không được gọi
         */
        @Test
        @DisplayName("TC-RRO-01: Email không tồn tại → không gửi email")
        void tc_rro_01_emailNotFound_doesNotSendEmail() {
            when(userRepository.findByEmail("notexist@example.com")).thenReturn(Optional.empty());

            assertThatCode(() -> passwordResetService.resendResetOtp("notexist@example.com"))
                    .doesNotThrowAnyException();

            verifyNoInteractions(emailService);
        }

        /**
         * TC-RRO-02: Email tồn tại → gửi OTP mới
         * Expected: emailService.sendPasswordResetOtpEmail được gọi
         */
        @Test
        @DisplayName("TC-RRO-02: Email tồn tại → gửi OTP reset mới")
        void tc_rro_02_emailFound_sendsNewOtp() {
            Users user = buildUser("user@example.com", "ACTIVE");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            passwordResetService.resendResetOtp("user@example.com");

            verify(emailService).sendPasswordResetOtpEmail(
                    eq("user@example.com"), anyString(), anyString());
        }
    }
}
