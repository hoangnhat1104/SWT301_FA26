package com.edusphere.repository;

import com.edusphere.entity.OauthAccounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho bảng oauth_accounts.
 */
@Repository
public interface OAuthAccountRepository extends JpaRepository<OauthAccounts, Integer> {

    /**
     * Tìm OAuth account theo provider + providerUserId.
     * Dùng khi user login bằng Google để kiểm tra tài khoản đã liên kết chưa.
     */
    Optional<OauthAccounts> findByProviderAndProviderUserId(String provider, String providerUserId);
}
