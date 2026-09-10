package info.prorabka.varamy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Отдельный сервис для асинхронной отправки push.
 * Живёт отдельно от NotificationService, чтобы @Async работал корректно
 * (Spring не умеет асинхронно вызывать методы внутри того же класса).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushSender {

    private final FcmTokenService fcmTokenService;
    private final HmsTokenService hmsTokenService;
    private final RuStoreTokenService ruStoreTokenService;
    private final FcmPushService fcmPushService;
    private final HuaweiPushService huaweiPushService;
    private final RuStorePushService ruStorePushService;

    @Async
    public void sendToUser(UUID userId, String title, String body) {
        try {
            // 1. RuStore
            if (!ruStoreTokenService.getActiveTokensForUser(userId).isEmpty()) {
                if (ruStorePushService.sendNotification(userId, title, body)) {
                    log.info("📱 Push отправлен через RuStore пользователю {}", userId);
                    return;
                }
                log.warn("⚠️ RuStore не сработал, пробуем FCM");
            }

            // 2. FCM
            if (!fcmTokenService.getActiveTokensForUser(userId).isEmpty()) {
                if (fcmPushService.sendNotification(userId, title, body)) {
                    log.info("📱 Push отправлен через FCM пользователю {}", userId);
                    return;
                }
                log.warn("⚠️ FCM не сработал, пробуем HMS");
            }

            // 3. HMS
            if (!hmsTokenService.getActiveTokensForUser(userId).isEmpty()) {
                if (huaweiPushService.sendNotification(userId, title, body)) {
                    log.info("📱 Push отправлен через HMS пользователю {}", userId);
                    return;
                }
                log.warn("⚠️ HMS не сработал");
            }

            log.warn("❌ Ни один push-канал не сработал для пользователя {}", userId);
        } catch (Exception e) {
            log.error("❌ Ошибка асинхронной отправки push пользователю {}: {}",
                    userId, e.getMessage(), e);
        }
    }

    @Async
    public void sendWakeUp(UUID userId) {
        try {
            // 1. RuStore
            if (!ruStoreTokenService.getActiveTokensForUser(userId).isEmpty()) {
                boolean ok = false;
                try {
                    ok = ruStorePushService.sendWakeUpNotification(userId);
                } catch (Exception e) {
                    log.warn("RuStore WAKE_UP ошибка: {}", e.getMessage());
                }
                if (ok) {
                    log.info("📱 WAKE_UP отправлен через RuStore пользователю {}", userId);
                    return;
                }
                log.warn("⚠️ RuStore не сработал, пробуем FCM");
            }

            // 2. FCM
            if (!fcmTokenService.getActiveTokensForUser(userId).isEmpty()) {
                boolean ok = false;
                try {
                    ok = fcmPushService.sendWakeUpNotification(userId);
                } catch (Exception e) {
                    log.warn("FCM WAKE_UP ошибка: {}", e.getMessage());
                }
                if (ok) {
                    log.info("📱 WAKE_UP отправлен через FCM пользователю {}", userId);
                    return;
                }
                log.warn("⚠️ FCM не сработал, пробуем HMS");
            }

            // 3. HMS
            if (!hmsTokenService.getActiveTokensForUser(userId).isEmpty()) {
                boolean ok = false;
                try {
                    ok = huaweiPushService.sendWakeUpNotification(userId);
                } catch (Exception e) {
                    log.warn("HMS WAKE_UP ошибка: {}", e.getMessage());
                }
                if (ok) {
                    log.info("📱 WAKE_UP отправлен через HMS пользователю {}", userId);
                    return;
                }
            }

            log.warn("❌ Ни один канал не смог доставить WAKE_UP пользователю {}", userId);
        } catch (Exception e) {
            log.error("❌ Ошибка WAKE_UP для пользователя {}: {}", userId, e.getMessage(), e);
        }
    }


}