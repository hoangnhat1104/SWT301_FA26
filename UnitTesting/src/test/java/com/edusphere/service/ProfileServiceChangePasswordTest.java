package com.edusphere.service;

import com.edusphere.dto.ChangePasswordRequest;
import com.edusphere.entity.Users;
import com.edusphere.repository.UserRepository;
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
 * Unit Test: ProfileService.changePassword()
 * ==============================================================
 *
 * Business Logic được kiểm tra:
 *
 * changePassword(Integer userId, ChangePasswordRequest request)
 *
 *  - User không tồn tại → ném RuntimeException
 *  - User đăng nhập bằng Google (passwordHash = null) → ném IllegalArgumentException
 *    với thông báo hướng dẫn dùng hỗ trợ
 *  - Mật khẩu hiện tại sai → ném IllegalArgumentException "không đúng"
 *  - Confirm password mới không khớp → ném IllegalArgumentException "không khớp"
 *  - Hợp lệ → cập nhật passwordHash, lưu DB
 *  - Password mới phải được mã hóa (không lưu plaintext)
 *
 * Framework: JUnit 5 + Mockito
 * ==============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService.changePassword() Unit Tests")
class ProfileServiceChangePasswordTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileService profileService;

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private Users buildActiveUser(String passwordHash) {
        Users user = new Users();
        user.setUserId(1);
        user.setEmail("user@example.com");
        user.setFullName("Test User");
        user.setStatus("ACTIVE");
        user.setPasswordHash(passwordHash);
        user.setCreatedAt(new Date());
        return user;
    }

    private ChangePasswordRequest buildRequest(String current, String newPwd, String confirm) {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword(current);
        req.setNewPassword(newPwd);
        req.setConfirmPassword(confirm);
        return req;
    }

    // ================================================================
    // Test Cases
    // ================================================================

    /**
     * TC-CP-01: userId không tồn tại trong DB
     * Expected: ném RuntimeException
     */
    @Test
    @DisplayName("TC-CP-01: userId không tồn tại → ném RuntimeException")
    void tc_cp_01_userNotFound_throwsRuntimeException() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                profileService.changePassword(99, buildRequest("old", "new12345", "new12345")))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * TC-CP-02: User đăng nhập bằng Google (passwordHash = null)
     * Expected: ném IllegalArgumentException với thông báo liên quan đến Google
     */
    @Test
    @DisplayName("TC-CP-02: Google user (passwordHash=null) → ném IllegalArgumentException")
    void tc_cp_02_googleUserNoPassword_throwsException() {
        Users user = buildActiveUser(null);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                profileService.changePassword(1, buildRequest("anyPwd", "new12345", "new12345")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Google");
    }

    /**
     * TC-CP-03: User đăng nhập bằng Google (passwordHash = blank string "")
     * Expected: ném IllegalArgumentException
     */
    @Test
    @DisplayName("TC-CP-03: Google user (passwordHash=blank) → ném IllegalArgumentException")
    void tc_cp_03_googleUserBlankPasswordHash_throwsException() {
        Users user = buildActiveUser("   "); // blank
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                profileService.changePassword(1, buildRequest("anyPwd", "new12345", "new12345")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * TC-CP-04: Mật khẩu hiện tại sai
     * Input: currentPassword không match với passwordHash trong DB
     * Expected: ném IllegalArgumentException "không đúng"
     */
    @Test
    @DisplayName("TC-CP-04: Mật khẩu hiện tại sai → ném IllegalArgumentException")
    void tc_cp_04_wrongCurrentPassword_throwsException() {
        Users user = buildActiveUser("HASHED_CURRENT");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPwd", "HASHED_CURRENT")).thenReturn(false);

        assertThatThrownBy(() ->
                profileService.changePassword(1, buildRequest("wrongPwd", "new12345", "new12345")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không đúng");

        verify(userRepository, never()).save(any());
    }

    /**
     * TC-CP-05: Confirm password mới không khớp với new password
     * Input: currentPassword đúng, newPassword != confirmPassword
     * Expected: ném IllegalArgumentException "không khớp"
     */
    @Test
    @DisplayName("TC-CP-05: Confirm password mới không khớp → ném IllegalArgumentException")
    void tc_cp_05_confirmPasswordMismatch_throwsException() {
        Users user = buildActiveUser("HASHED_CURRENT");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPwd", "HASHED_CURRENT")).thenReturn(true);

        assertThatThrownBy(() ->
                profileService.changePassword(1, buildRequest("correctPwd", "newPass1", "newPass2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không khớp");

        verify(userRepository, never()).save(any());
    }

    /**
     * TC-CP-06: Đổi mật khẩu thành công
     * Input: currentPassword đúng, newPassword == confirmPassword
     * Expected: user.passwordHash được cập nhật, userRepository.save được gọi
     */
    @Test
    @DisplayName("TC-CP-06: Dữ liệu hợp lệ → đổi mật khẩu thành công")
    void tc_cp_06_validRequest_updatesPassword() {
        Users user = buildActiveUser("HASHED_CURRENT");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPwd", "HASHED_CURRENT")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("NEW_HASH");
        when(userRepository.save(any(Users.class))).thenReturn(user);

        profileService.changePassword(1, buildRequest("correctPwd", "newPass123", "newPass123"));

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("NEW_HASH");
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    /**
     * TC-CP-07: Password mới được mã hóa, không lưu plaintext
     * Input: newPassword = "myNewPassword"
     * Expected: user.passwordHash != "myNewPassword"
     */
    @Test
    @DisplayName("TC-CP-07: Password mới được mã hóa, không lưu dạng plaintext")
    void tc_cp_07_newPasswordEncoded_notPlaintext() {
        Users user = buildActiveUser("HASHED_CURRENT");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPwd", "HASHED_CURRENT")).thenReturn(true);
        when(passwordEncoder.encode("myNewPassword")).thenReturn("BCRYPT_RESULT");
        when(userRepository.save(any(Users.class))).thenReturn(user);

        profileService.changePassword(1, buildRequest("correctPwd", "myNewPassword", "myNewPassword"));

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("myNewPassword");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("BCRYPT_RESULT");
    }
}
