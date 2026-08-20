// src/main/java/info/prorabka/varamy/service/RuStoreTokenService.java
package info.prorabka.varamy.service;

import info.prorabka.varamy.entity.RuStoreToken;
import info.prorabka.varamy.entity.User;
import info.prorabka.varamy.repository.RuStoreTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuStoreTokenService {
    private final RuStoreTokenRepository tokenRepository;
    private final UserService userService;

    @Transactional
    public void registerToken(UUID userId, String token) {
        User user = userService.getUserById(userId);
        tokenRepository.findByUserIdAndIsActiveTrue(userId)
                .forEach(t -> t.setActive(false));
        tokenRepository.save(new RuStoreToken(user, token));
        log.info("RuStore token registered for user {}", userId);
    }

    @Transactional
    public void unregisterToken(UUID userId, String token) {
        tokenRepository.deactivateToken(userId, token);
        log.info("RuStore token deactivated for user {}", userId);
    }

    @Transactional(readOnly = true)
    public List<String> getActiveTokensForUser(UUID userId) {
        return tokenRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(RuStoreToken::getToken)
                .toList();
    }

    @Transactional
    public void deleteAllTokensForUser(UUID userId) {
        tokenRepository.deleteByUserId(userId);
        log.info("🗑️ Удалены все RuStore токены для пользователя {}", userId);
    }
}