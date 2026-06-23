package com.placesync.common.security;

import com.placesync.user.entity.User;
import com.placesync.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final boolean enabled;
    private final boolean accountNonLocked;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public static UserPrincipal fromUser(User user) {
        boolean locked = user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(java.time.OffsetDateTime.now());
        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .role(user.getRole())
                .enabled(Boolean.TRUE.equals(user.getIsActive()) && Boolean.TRUE.equals(user.getIsEmailVerified()))
                .accountNonLocked(!locked)
                .build();
    }

    public static UserPrincipal fromClaims(Claims claims) {
        return UserPrincipal.builder()
                .id(UUID.fromString(claims.getSubject()))
                .email(claims.get("email", String.class))
                .role(UserRole.valueOf(claims.get("role", String.class)))
                .enabled(true)
                .accountNonLocked(true)
                .build();
    }
}
