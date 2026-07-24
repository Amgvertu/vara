package info.prorabka.varamy.security;

import info.prorabka.varamy.entity.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
public class SecurityUser implements UserDetails {

    private UUID id;
    private String phone;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    public static SecurityUser build(User user) {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(user.getId());
        securityUser.setPhone(user.getPhone());
        securityUser.setPassword(user.getPasswordHash());

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
        securityUser.setAuthorities(authorities);

        return securityUser;
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
        return id.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}