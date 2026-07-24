package info.prorabka.varamy.service;

import info.prorabka.varamy.entity.FcmToken;
import info.prorabka.varamy.entity.User;
import info.prorabka.varamy.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenService {
    private final FcmTokenRepository fcmTokenRepository;
    private final UserService userService;

    @Transactional
    public void registerToken(UUID userId, String token) {
        User user = userService.getUserById(userId);
        // Деактивируем старые токены для этого пользователя (опционально)
        fcmTokenRepository.findByUserIdAndIsActiveTrue(userId)
                .forEach(t -> t.setActive(false));
        // Сохраняем новый токен
        FcmToken fcmToken = new FcmToken(user, token);
        fcmTokenRepository.save(fcmToken);
        log.info("✅ FCM токен зарегистрирован для пользователя {}: {}", userId, token);
    }

    @Transactional
    public void unregisterToken(UUID userId, String token) {
        fcmTokenRepository.deactivateToken(userId, token);
        log.info("FCM token deactivated for user {}: {}", userId, token);
    }

    @Transactional(readOnly = true)
    public List<String> getActiveTokensForUser(UUID userId) {
        return fcmTokenRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(FcmToken::getToken)
                .toList();
    }
}