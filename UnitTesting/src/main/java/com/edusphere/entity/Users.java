package com.edusphere.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

/**
 * Entity ánh xạ bảng "users".
 * status: PENDING | ACTIVE | LOCKED
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class Users implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    // NULL nếu tài khoản đăng nhập bằng Google OAuth
    @Size(max = 255)
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Size(max = 20)
    @Column(name = "phone", length = 20)
    private String phone;

    @Size(max = 500)
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // PENDING / ACTIVE / LOCKED
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "userId", fetch = FetchType.LAZY)
    private Collection<OauthAccounts> oauthAccountsCollection;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "userId", fetch = FetchType.LAZY)
    private Collection<PasswordResetTokens> passwordResetTokensCollection;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "userId", fetch = FetchType.LAZY)
    private Collection<EmailVerificationTokens> emailVerificationTokensCollection;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "users", fetch = FetchType.LAZY)
    private Collection<UserRoles> userRolesCollection;

    public Users(Integer userId) {
        this.userId = userId;
    }

    public Users(Integer userId, String fullName, String email, String status, Date createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Users other)) return false;
        return (this.userId != null && this.userId.equals(other.userId));
    }

    @Override
    public String toString() {
        return "Users[ userId=" + userId + " ]";
    }
}
