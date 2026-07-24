package info.prorabka.varamy.service;

import info.prorabka.varamy.entity.HmsToken;
import info.prorabka.varamy.entity.User;
import info.prorabka.varamy.repository.HmsTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HmsTokenService {
    private final HmsTokenRepository hmsTokenRepository;
    private final UserService userService;

    @Transactional
    public void registerToken(UUID userId, String token) {
        User user = userService.getUserById(userId);
        hmsTokenRepository.findByUserIdAndIsActiveTrue(userId)
                .forEach(t -> t.setActive(false));
        hmsTokenRepository.save(new HmsToken(user, token));
        log.info("HMS token registered for user {}", userId);
    }

    @Transactional
    public void unregisterToken(UUID userId, String token) {
        hmsTokenRepository.deactivateToken(userId, token);
        log.info("HMS token deactivated for user {}", userId);
    }

    @Transactional(readOnly = true)
    public List<String> getActiveTokensForUser(UUID userId) {
        return hmsTokenRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(HmsToken::getToken)
                .toList();
    }
}