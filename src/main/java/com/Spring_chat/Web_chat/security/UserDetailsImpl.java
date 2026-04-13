package com.Spring_chat.Web_chat.security;

import com.Spring_chat.Web_chat.enums.UserStatus;
import com.Spring_chat.Web_chat.entity.Role;
import com.Spring_chat.Web_chat.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Adapter between the JPA {@link User} entity and Spring Security's {@link UserDetails} contract.
 * Used exclusively during authentication (login). Subsequent requests use {@link AuthenticatedUser}
 * populated from JWT claims — no per-request database hit.
 */
public class UserDetailsImpl implements UserDetails {

    @Getter
    private final Long id;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.enabled = user.getStatus() == UserStatus.ACTIVE;
        this.authorities = buildAuthorities(user.getRoles());
    }

    /**
     * Builds a flat authority set from the user's roles AND each role's permissions.
     * Format: {@code ROLE_ADMIN}, {@code USER_BAN}, {@code MESSAGE_DELETE_ANY}, …
     */
    private static Set<GrantedAuthority> buildAuthorities(Set<Role> roles) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority(role.getName().name()));
            role.getPermissions().forEach(p ->
                    authorities.add(new SimpleGrantedAuthority(p.getName().name()))
            );
        }
        return authorities;
    }

    /** Returns role names as strings (e.g. "ROLE_ADMIN") for embedding in JWT claims. */
    public Set<String> getRoleNames() {
        Set<String> roleNames = new HashSet<>();
        authorities.forEach(a -> {
            String auth = a.getAuthority();
            if (auth.startsWith("ROLE_")) {
                roleNames.add(auth);
            }
        });
        return roleNames;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
