package com.edusphere.repository;

import com.edusphere.entity.UserRoles;
import com.edusphere.entity.UserRolesPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho bảng user_roles.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRoles, UserRolesPK> {

    /** Lấy tất cả role của một user */
    List<UserRoles> findByUserRolesPK_UserId(int userId);
}
