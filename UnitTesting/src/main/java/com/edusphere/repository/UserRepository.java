package com.edusphere.repository;

import com.edusphere.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho bảng users.
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {

    /** Tìm user theo email (dùng cho login, check duplicate) */
    Optional<Users> findByEmail(String email);

    /** Kiểm tra email đã tồn tại chưa (dùng khi đăng ký) */
    boolean existsByEmail(String email);
}
