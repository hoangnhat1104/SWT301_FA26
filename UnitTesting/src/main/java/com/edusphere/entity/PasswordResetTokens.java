package com.edusphere.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Entity ánh xạ bảng "password_reset_tokens".
 * Lưu token dùng để reset mật khẩu.
 * used_at = NULL nghĩa là token chưa được dùng.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetTokens implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Integer tokenId;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expires_at", nullable = false)
    private Date expiresAt;

    // NULL = chưa dùng; có giá trị = đã dùng rồi
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "used_at")
    private Date usedAt;

    // Tên field là "userId" nhưng kiểu Users (JPA relationship)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    @ManyToOne(optional = false)
    private Users userId;

    public PasswordResetTokens(String token, Date expiresAt, Users user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.userId = user;
    }

    @Override
    public int hashCode() {
        return tokenId != null ? tokenId.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PasswordResetTokens other)) return false;
        return (this.tokenId != null && this.tokenId.equals(other.tokenId));
    }

    @Override
    public String toString() {
        return "PasswordResetTokens[ tokenId=" + tokenId + " ]";
    }
}
