package com.edusphere.repository;

import com.edusphere.entity.EmailVerificationTokens;
import com.edusphere.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokens, Integer> {

    Optional<EmailVerificationTokens> findByToken(String token);

    @Transactional
    void deleteByUserId(Users user);
}
