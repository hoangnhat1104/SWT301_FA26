package com.edusphere.security;

import com.edusphere.entity.Roles;
import com.edusphere.entity.UserRoles;
import com.edusphere.entity.UserRolesPK;
import com.edusphere.entity.Users;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ==============================================================
 * Unit Test: CustomUserDetails
 * ==============================================================
 *
 * Business Logic được kiểm tra:
 *
 * CustomUserDetails bọc entity Users thành UserDetails cho Spring Security.
 * Các trạng thái được map như sau:
 *   - status = "ACTIVE"  → isEnabled() = true,  isAccountNonLocked() = true
 *   - status = "PENDING" → isEnabled() = false, isAccountNonLocked() = true
 *   - status = "LOCKED"  → isEnabled() = false, isAccountNonLocked() = false
 *   - getUsername()      → trả về email (dùng làm username cho Spring Security)
 *   - getPassword()      → trả về passwordHash
 *   - getAuthorities()   → map từ UserRoles → "ROLE_<roleName>"
 *   - isAccountNonExpired(), isCredentialsNonExpired() → luôn true
 *
 * Framework: JUnit 5
 * ==============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetails Unit Tests")
class CustomUserDetailsTest {

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private Users buildUser(String email, String status, String passwordHash) {
        Users user = new Users();
        user.setUserId(1);
        user.setEmail(email);
        user.setFullName("Test User");
        user.setStatus(status);
        user.setPasswordHash(passwordHash);
        user.setCreatedAt(new Date());
        return user;
    }

    private UserRoles buildUserRole(Users user, String roleName) {
        Roles role = new Roles();
        role.setRoleId(1);
        role.setRoleName(roleName);

        UserRoles userRole = new UserRoles();
        userRole.setRoles(role);
        userRole.setUsers(user);
        return userRole;
    }

    // ================================================================
    // Test Cases
    // ================================================================

    /**
     * TC-CUD-01: User ACTIVE → isEnabled() = true
     */
    @Test
    @DisplayName("TC-CUD-01: User ACTIVE → isEnabled() = true")
    void tc_cud_01_activeUser_isEnabled() {
        Users user = buildUser("user@example.com", "ACTIVE", "hash");
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isEnabled()).isTrue();
    }

    /**
     * TC-CUD-02: User PENDING → isEnabled() = false
     */
    @Test
    @DisplayName("TC-CUD-02: User PENDING → isEnabled() = false")
    void tc_cud_02_pendingUser_isNotEnabled() {
        Users user = buildUser("user@example.com", "PENDING", "hash");
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isEnabled()).isFalse();
    }

    /**
     * TC-CUD-03: User LOCKED → isAccountNonLocked() = false
     */
    @Test
    @DisplayName("TC-CUD-03: User LOCKED → isAccountNonLocked() = false")
    void tc_cud_03_lockedUser_isAccountLocked() {
        Users user = buildUser("user@example.com", "LOCKED", "hash");
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isAccountNonLocked()).isFalse();
        assertThat(details.isEnabled()).isFalse();
    }

    /**
     * TC-CUD-04: User ACTIVE → isAccountNonLocked() = true
     */
    @Test
    @DisplayName("TC-CUD-04: User ACTIVE → isAccountNonLocked() = true")
    void tc_cud_04_activeUser_isNotLocked() {
        Users user = buildUser("user@example.com", "ACTIVE", "hash");
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isAccountNonLocked()).isTrue();
    }

    /**
     * TC-CUD-05: getUsername() trả về email (không phải tên)
     * Spring Security dùng username làm key để tra cứu user
     */
    @Test
    @DisplayName("TC-CUD-05: getUsername() trả về email")
    void tc_cud_05_getUsernameReturnsEmail() {
        Users user = buildUser("myemail@example.com", "ACTIVE", "hash");
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getUsername()).isEqualTo("myemail@example.com");
    }

    /**
     * TC-CUD-06: getPassword() trả về passwordHash
     */
    @Test
    @DisplayName("TC-CUD-06: getPassword() trả về passwordHash")
    void tc_cud_06_getPasswordReturnsHash() {
        Users user = buildUser("user@example.com", "ACTIVE", "BCRYPT_HASH");
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getPassword()).isEqualTo("BCRYPT_HASH");
    }

    /**
     * TC-CUD-07: isAccountNonExpired() và isCredentialsNonExpired() luôn true
     * Dự án không implement cơ chế expire tài khoản/credentials
     */
    @Test
    @DisplayName("TC-CUD-07: isAccountNonExpired() và isCredentialsNonExpired() luôn true")
    void tc_cud_07_expiryAlwaysTrue() {
        Users user = buildUser("user@example.com", "ACTIVE", "hash");
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    /**
     * TC-CUD-08: getAuthorities() map đúng sang "ROLE_<roleName>"
     * Input: user có role "STUDENT"
     * Expected: authority = "ROLE_STUDENT"
     */
    @Test
    @DisplayName("TC-CUD-08: Authorities được map thành 'ROLE_<roleName>'")
    void tc_cud_08_authoritiesMappedWithRolePrefix() {
        Users user = buildUser("user@example.com", "ACTIVE", "hash");
        UserRoles role = buildUserRole(user, "STUDENT");
        user.setUserRolesCollection(List.of(role));

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_STUDENT");
    }

    /**
     * TC-CUD-09: User không có role nào → authorities rỗng
     * Input: user.userRolesCollection = null hoặc empty
     * Expected: getAuthorities() trả về collection rỗng (không ném exception)
     */
    @Test
    @DisplayName("TC-CUD-09: User không có role → getAuthorities() trả về rỗng")
    void tc_cud_09_noRoles_emptyAuthorities() {
        Users user = buildUser("user@example.com", "ACTIVE", "hash");
        user.setUserRolesCollection(Collections.emptyList());

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getAuthorities()).isEmpty();
    }

    /**
     * TC-CUD-10: User có nhiều roles → tất cả được map
     * Input: user có 2 roles: "STUDENT" và "INSTRUCTOR"
     * Expected: authorities chứa "ROLE_STUDENT" và "ROLE_INSTRUCTOR"
     */
    @Test
    @DisplayName("TC-CUD-10: User có nhiều roles → tất cả được map sang authorities")
    void tc_cud_10_multipleRoles_allMappedToAuthorities() {
        Users user = buildUser("user@example.com", "ACTIVE", "hash");

        Roles role1 = new Roles(); role1.setRoleId(1); role1.setRoleName("STUDENT");
        Roles role2 = new Roles(); role2.setRoleId(2); role2.setRoleName("INSTRUCTOR");

        UserRoles ur1 = new UserRoles(); ur1.setRoles(role1); ur1.setUsers(user);
        UserRoles ur2 = new UserRoles(); ur2.setRoles(role2); ur2.setUsers(user);
        user.setUserRolesCollection(List.of(ur1, ur2));

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_STUDENT", "ROLE_INSTRUCTOR");
    }

    /**
     * TC-CUD-11: getUserId() trả về userId của entity Users
     */
    @Test
    @DisplayName("TC-CUD-11: getUserId() trả về đúng userId")
    void tc_cud_11_getUserId_returnsCorrectId() {
        Users user = buildUser("user@example.com", "ACTIVE", "hash");
        user.setUserId(42);
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getUserId()).isEqualTo(42);
    }

    /**
     * TC-CUD-12: getFullName() trả về fullName của entity Users
     */
    @Test
    @DisplayName("TC-CUD-12: getFullName() trả về đúng fullName")
    void tc_cud_12_getFullName_returnsCorrectName() {
        Users user = buildUser("user@example.com", "ACTIVE", "hash");
        user.setFullName("Nguyen Van A");
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getFullName()).isEqualTo("Nguyen Van A");
    }
}
