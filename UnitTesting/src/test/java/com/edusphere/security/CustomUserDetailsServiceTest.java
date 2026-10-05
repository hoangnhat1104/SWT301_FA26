package com.edusphere.security;

import com.edusphere.entity.Users;
import com.edusphere.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ==============================================================
 * Unit Test: CustomUserDetailsService
 * ==============================================================
 *
 * Business Logic được kiểm tra:
 *
 * loadUserByUsername(String email):
 *   - Email không tồn tại → ném UsernameNotFoundException
 *   - Email tồn tại → trả về CustomUserDetails wrapping user
 *   - Email được tra cứu đúng (case-sensitive theo DB)
 *   - Kết quả là instance của CustomUserDetails
 *
 * Đây là entry point của Spring Security khi user đăng nhập bằng form.
 * Spring Security gọi loadUserByUsername với email nhập vào.
 *
 * Framework: JUnit 5 + Mockito
 * ==============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private Users buildUser(String email, String status) {
        Users user = new Users();
        user.setUserId(1);
        user.setEmail(email);
        user.setFullName("Test User");
        user.setStatus(status);
        user.setPasswordHash("HASH");
        user.setCreatedAt(new Date());
        user.setUserRolesCollection(Collections.emptyList());
        return user;
    }

    /**
     * TC-CUDS-01: Email không tồn tại trong DB → ném UsernameNotFoundException
     * Spring Security bắt exception này và chuyển thành lỗi "badCredentials"
     */
    @Test
    @DisplayName("TC-CUDS-01: Email không tồn tại → ném UsernameNotFoundException")
    void tc_cuds_01_emailNotFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("notexist@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                customUserDetailsService.loadUserByUsername("notexist@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("notexist@example.com");
    }

    /**
     * TC-CUDS-02: Email tồn tại → trả về CustomUserDetails hợp lệ
     * Expected: kết quả không null, username = email
     */
    @Test
    @DisplayName("TC-CUDS-02: Email tồn tại → trả về CustomUserDetails")
    void tc_cuds_02_emailFound_returnsUserDetails() {
        Users user = buildUser("user@example.com", "ACTIVE");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("user@example.com");

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(CustomUserDetails.class);
        assertThat(result.getUsername()).isEqualTo("user@example.com");
    }

    /**
     * TC-CUDS-03: Kết quả trả về wrap đúng user entity
     * Expected: CustomUserDetails.getUser() == user entity từ DB
     */
    @Test
    @DisplayName("TC-CUDS-03: UserDetails trả về wrap đúng entity Users")
    void tc_cuds_03_returnedDetailsWrapsCorrectUser() {
        Users user = buildUser("user@example.com", "ACTIVE");
        user.setUserId(42);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("user@example.com");

        CustomUserDetails customDetails = (CustomUserDetails) result;
        assertThat(customDetails.getUserId()).isEqualTo(42);
        assertThat(customDetails.getFullName()).isEqualTo("Test User");
    }

    /**
     * TC-CUDS-04: userRepository.findByEmail được gọi đúng với email nhập vào
     * Kiểm tra service không tự ý transform email trước khi tra cứu
     */
    @Test
    @DisplayName("TC-CUDS-04: findByEmail được gọi với email chính xác")
    void tc_cuds_04_findByEmailCalledWithCorrectEmail() {
        Users user = buildUser("exact@example.com", "ACTIVE");
        when(userRepository.findByEmail("exact@example.com")).thenReturn(Optional.of(user));

        customUserDetailsService.loadUserByUsername("exact@example.com");

        verify(userRepository).findByEmail("exact@example.com");
    }
}
