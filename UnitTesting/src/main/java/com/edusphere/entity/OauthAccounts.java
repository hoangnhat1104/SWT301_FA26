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
 * Entity ánh xạ bảng "oauth_accounts".
 * Lưu tài khoản OAuth (Google, v.v.) liên kết với user.
 * Unique constraint: (provider, provider_user_id)
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "oauth_accounts",
       uniqueConstraints = @UniqueConstraint(
           name = "UQ_oauth_provider_user",
           columnNames = {"provider", "provider_user_id"}
       ))
public class OauthAccounts implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_id")
    private Integer oauthId;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    // Tên field là "userId" nhưng kiểu Users (JPA relationship)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    @ManyToOne(optional = false)
    private Users userId;

    public OauthAccounts(String provider, String providerUserId, Date createdAt, Users user) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.createdAt = createdAt;
        this.userId = user;
    }

    @Override
    public int hashCode() {
        return oauthId != null ? oauthId.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof OauthAccounts other)) return false;
        return (this.oauthId != null && this.oauthId.equals(other.oauthId));
    }

    @Override
    public String toString() {
        return "OauthAccounts[ oauthId=" + oauthId + " ]";
    }
}
