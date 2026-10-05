package com.edusphere.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collection;

/**
 * Entity ánh xạ bảng "roles".
 * Các role có sẵn: STUDENT, INSTRUCTOR, ADMINISTRATOR
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Roles implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "roles", fetch = FetchType.LAZY)
    private Collection<UserRoles> userRolesCollection;

    public Roles(Integer roleId) {
        this.roleId = roleId;
    }

    public Roles(Integer roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
    }

    @Override
    public int hashCode() {
        return roleId != null ? roleId.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Roles other)) return false;
        return (this.roleId != null && this.roleId.equals(other.roleId));
    }

    @Override
    public String toString() {
        return "Roles[ roleId=" + roleId + " ]";
    }
}
