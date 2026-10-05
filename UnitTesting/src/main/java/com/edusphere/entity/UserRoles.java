package com.edusphere.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Entity ánh xạ bảng "user_roles".
 * Composite PK: user_id + role_id
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_roles")
public class UserRoles implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private UserRolesPK userRolesPK;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "assigned_at", nullable = false)
    private Date assignedAt;

    @JoinColumn(name = "role_id", referencedColumnName = "role_id", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Roles roles;

    @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Users users;

    public UserRoles(UserRolesPK userRolesPK, Date assignedAt) {
        this.userRolesPK = userRolesPK;
        this.assignedAt = assignedAt;
    }

    public UserRoles(int userId, int roleId) {
        this.userRolesPK = new UserRolesPK(userId, roleId);
    }

    @Override
    public int hashCode() {
        return userRolesPK != null ? userRolesPK.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UserRoles other)) return false;
        return (this.userRolesPK != null && this.userRolesPK.equals(other.userRolesPK));
    }

    @Override
    public String toString() {
        return "UserRoles[ userRolesPK=" + userRolesPK + " ]";
    }
}
