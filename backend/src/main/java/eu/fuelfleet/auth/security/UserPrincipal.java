package eu.fuelfleet.auth.security;

import eu.fuelfleet.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class UserPrincipal implements UserDetails {

    private UUID id;
    private UUID companyId;
    private String email;
    private String password;
    private Collection<GrantedAuthority> authorities;

    public static UserPrincipal fromUser(User user) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
        return new UserPrincipal(
                user.getId(),
                user.getCompany().getId(),
                user.getEmail(),
                user.getPasswordHash(),
                authorities
        );
    }

    @Override
    public String getUsername() {
        return email;
    }
}
