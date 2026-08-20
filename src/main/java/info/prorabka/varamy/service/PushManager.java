package info.prorabka.varamy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushManager {

    private final List<PushService> pushServices;
    private final FcmTokenService fcmTokenService;
    private final HmsTokenService hmsTokenService;
    private final RuStoreTokenService ruStoreTokenService;

    /**
     * Отправка push-уведомления через все доступные каналы
     * @return true если хотя бы один канал сработал
     */
    public boolean sendPush(UUID userId, String title, String body) {
        log.info("📤 Отправка push пользователю {}: title='{}', body='{}'", userId, title, body);

        // Проверяем, есть ли у пользователя хотя бы один токен
        boolean hasAnyToken = false;
        if (!fcmTokenService.getActiveTokensForUser(userId).isEmpty()) hasAnyToken = true;
        if (!hmsTokenService.getActiveTokensForUser(userId).isEmpty()) hasAnyToken = true;
        if (!ruStoreTokenService.getActiveTokensForUser(userId).isEmpty()) hasAnyToken = true;

        if (!hasAnyToken) {
            log.warn("⚠️ У пользователя {} нет активных push-токенов", userId);
            return false;
        }

        int successCount = 0;
        for (PushService pushService : pushServices) {
            try {
                pushService.sendNotification(userId, title, body);
                successCount++;
                log.info("✅ Успешно отправлено через {}", pushService.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("❌ Ошибка через {}: {}",
                        pushService.getClass().getSimpleName(), e.getMessage());
            }
        }

        return successCount > 0;
    }

    /**
     * Отправка WAKE_UP уведомления
     */
    public void sendWakeUp(UUID userId) {
        log.info("⏰ Отправка WAKE_UP пользователю {}", userId);
        for (PushService pushService : pushServices) {
            try {
                pushService.sendWakeUpNotification(userId);
                log.info("✅ WAKE_UP отправлен через {}", pushService.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("❌ Ошибка WAKE_UP через {}: {}",
                        pushService.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}