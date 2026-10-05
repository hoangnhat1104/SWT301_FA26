package com.edusphere.repository;

import com.edusphere.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho bảng roles.
 */
@Repository
public interface RoleRepository extends JpaRepository<Roles, Integer> {

    /** Tìm role theo tên (ví dụ: "STUDENT", "INSTRUCTOR", "ADMINISTRATOR") */
    Optional<Roles> findByRoleName(String roleName);
}
