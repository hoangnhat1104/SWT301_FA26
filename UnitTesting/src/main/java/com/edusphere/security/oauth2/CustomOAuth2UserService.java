package com.edusphere.security.oauth2;

import com.edusphere.entity.*;
import com.edusphere.repository.*;
import com.edusphere.security.CustomUserDetails;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

/**
 * Xử lý Google OAuth2 / OIDC login.
 * Dùng OidcUserService vì Google dùng OpenID Connect (scope: openid).
 *
 * Luồng:
 * 1. Nhận thông tin từ Google OIDC
 * 2. Kiểm tra oauth_accounts theo (provider, providerUserId)
 * 3. Nếu đã có → trả về user cũ
 * 4. Nếu chưa có + email tồn tại → link Google vào user đó
 * 5. Nếu chưa có + email mới → tạo user mới, gán STUDENT role
 * 6. Lưu oauth_accounts record
 */
@Service
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final OAuthAccountRepository oAuthAccountRepository;

    public CustomOAuth2UserService(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   UserRoleRepository userRoleRepository,
                                   OAuthAccountRepository oAuthAccountRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Lấy thông tin từ Google OIDC
        OidcUser oidcUser = super.loadUser(userRequest);

        String provider       = userRequest.getClientRegistration().getRegistrationId().toUpperCase(); // "GOOGLE"
        String providerUserId = oidcUser.getSubject();   // Google unique ID (sub claim)
        String email          = oidcUser.getEmail();
        String fullName       = oidcUser.getFullName();

        System.out.println("=== GOOGLE OIDC LOGIN === provider=" + provider + ", email=" + email);

        // 1. Kiểm tra oauth_accounts đã có chưa
        Optional<OauthAccounts> existingOAuth =
                oAuthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);

        Users user;

        if (existingOAuth.isPresent()) {
            System.out.println("=== OAuth account đã tồn tại");
            user = existingOAuth.get().getUserId();
        } else {
            System.out.println("=== OAuth account chưa có, kiểm tra email...");
            Optional<Users> existingUser = userRepository.findByEmail(email);

            if (existingUser.isPresent()) {
                System.out.println("=== Email tồn tại, liên kết Google vào user_id=" + existingUser.get().getUserId());
                user = existingUser.get();
                // Nếu user đang PENDING (chưa verify OTP) → kích hoạt luôn vì Google đã verify
                if ("PENDING".equals(user.getStatus())) {
                    user.setStatus("ACTIVE");
                    user.setUpdatedAt(new Date());
                    userRepository.save(user);
                }
            } else {
                System.out.println("=== Email mới, tạo user mới...");
                user = new Users();
                user.setFullName(fullName != null ? fullName : email);
                user.setEmail(email);
                user.setPasswordHash(null); // Google user không có password
                user.setStatus("ACTIVE");   // Google đã xác minh email
                user.setCreatedAt(new Date());
                user = userRepository.save(user);

                // Gán role STUDENT
                Roles studentRole = roleRepository.findByRoleName("STUDENT")
                        .orElseThrow(() -> new OAuth2AuthenticationException("Role STUDENT không tồn tại"));
                UserRolesPK pk = new UserRolesPK(user.getUserId(), studentRole.getRoleId());
                userRoleRepository.save(new UserRoles(pk, new Date()));
                System.out.println("=== Đã tạo user mới, user_id=" + user.getUserId());
            }

            // Tạo oauth_accounts record
            OauthAccounts oauthAccount = new OauthAccounts(provider, providerUserId, new Date(), user);
            oAuthAccountRepository.save(oauthAccount);
            System.out.println("=== Đã lưu oauth_accounts thành công!");
        }

        // Load lại user với đầy đủ roles
        Users finalUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new OAuth2AuthenticationException("Không tìm được user sau OAuth2"));

        // Trả về CustomUserDetails implement cả OidcUser
        return new CustomUserDetails(finalUser, oidcUser.getClaims(), oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}
