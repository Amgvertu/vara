package info.prorabka.varamy.security;

import info.prorabka.varamy.entity.User;
import info.prorabka.varamy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UsernameNotFoundException("User is not active");
        }

        return SecurityUser.build(user);
    }
}