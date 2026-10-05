package com.edusphere.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key cho bảng user_roles.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserRolesPK implements Serializable {

    private static final long serialVersionUID = 1L;

    @Basic(optional = false)
    @NotNull
    @Column(name = "user_id")
    private int userId;

    @Basic(optional = false)
    @NotNull
    @Column(name = "role_id")
    private int roleId;

    @Override
    public int hashCode() {
        return userId + roleId;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UserRolesPK other)) return false;
        return this.userId == other.userId && this.roleId == other.roleId;
    }

    @Override
    public String toString() {
        return "UserRolesPK[ userId=" + userId + ", roleId=" + roleId + " ]";
    }
}
