package com.edusphere.security;

import com.edusphere.entity.Users;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper bọc entity Users thành UserDetails + OidcUser cho Spring Security.
 *
 * Implement cả 3 interface:
 *   - UserDetails     → form login
 *   - OidcUser        → Google OIDC login (bao gồm cả OAuth2User)
 */
public class CustomUserDetails implements UserDetails, OidcUser {

    private final Users user;
    private final Collection<? extends GrantedAuthority> authorities;

    // OIDC fields (chỉ có giá trị khi đăng nhập Google)
    private Map<String, Object> claims = Collections.emptyMap();
    private OidcIdToken idToken;
    private OidcUserInfo userInfo;

    // Constructor cho Form Login
    public CustomUserDetails(Users user) {
        this.user = user;
        this.authorities = buildAuthorities(user);
    }

    // Constructor cho Google OIDC Login
    public CustomUserDetails(Users user,
                             Map<String, Object> claims,
                             OidcIdToken idToken,
                             OidcUserInfo userInfo) {
        this.user = user;
        this.authorities = buildAuthorities(user);
        this.claims   = claims   != null ? claims   : Collections.emptyMap();
        this.idToken  = idToken;
        this.userInfo = userInfo;
    }

    private static Collection<? extends GrantedAuthority> buildAuthorities(Users user) {
        if (user.getUserRolesCollection() == null || user.getUserRolesCollection().isEmpty()) {
            return Collections.emptyList();
        }
        return user.getUserRolesCollection().stream()
                .map(ur -> new SimpleGrantedAuthority("ROLE_" + ur.getRoles().getRoleName()))
                .collect(Collectors.toList());
    }

    // ---- Domain helpers ----
    public Users getUser()       { return user; }
    public Integer getUserId()   { return user.getUserId(); }
    public String getFullName()  { return user.getFullName(); }
    public String getAvatarUrl() { return user.getAvatarUrl(); }

    // ---- UserDetails ----
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()  { return user.getPasswordHash(); }
    @Override public String getUsername()  { return user.getEmail(); }
    @Override public boolean isEnabled()              { return "ACTIVE".equals(user.getStatus()); }
    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return !"LOCKED".equals(user.getStatus()); }
    @Override public boolean isCredentialsNonExpired(){ return true; }

    // ---- OidcUser (bao gồm OAuth2User) ----
    @Override public String getName()               { return user.getEmail(); }
    @Override public Map<String, Object> getAttributes() { return claims; }
    @Override public Map<String, Object> getClaims()     { return claims; }
    @Override public OidcUserInfo getUserInfo()          { return userInfo; }
    @Override public OidcIdToken getIdToken()            { return idToken; }
}
