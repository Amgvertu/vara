package info.prorabka.varamy.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushService implements PushService {

    private final FcmTokenService fcmTokenService;

    @Override
    public boolean sendWakeUpNotification(UUID userId) {
        List<String> tokens = fcmTokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.warn("FCM: нет активных токенов у пользователя {}", userId);
            return false;
        }
        boolean atLeastOneSent = false;
        for (String token : tokens) {
            Message message = Message.builder()
                    .setToken(token)
                    .putData("type", "WAKE_UP")
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .build();
            try {
                FirebaseMessaging.getInstance().send(message);
                log.info("FCM WAKE_UP отправлен пользователю {}", userId);
                atLeastOneSent = true;
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    fcmTokenService.unregisterToken(userId, token);
                } else {
                    log.error("FCM WAKE_UP ошибка: {}", e.getMessage());
                }
            }
        }
        return atLeastOneSent;
    }

    @Override
    public boolean sendNotification(UUID userId, String title, String body) {
        List<String> tokens = fcmTokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.warn("FCM: нет активных токенов у пользователя {}", userId);
            return false;
        }

        boolean atLeastOneSent = false;

        for (String token : tokens) {
            // ВАЖНО: добавляем и notification, и data.
            // notification — чтобы система показала уведомление на телефоне,
            // data — чтобы приложение могло обработать событие (открыть нужный экран и т.д.).
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", "REAL")
                    .putData("title", title != null ? title : "")
                    .putData("body", body != null ? body : "")
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("default")
                                    .setClickAction("OPEN_APP")
                                    .build())
                            .build())
                    .build();

            try {
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("FCM отправлен пользователю {}, ответ: {}", userId, response);
                atLeastOneSent = true;
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    log.warn("FCM токен недействителен, удаляем: {}", token);
                    fcmTokenService.unregisterToken(userId, token);
                } else {
                    log.error("FCM ошибка: {}", e.getMessage());
                }
            }
        }

        return atLeastOneSent;
    }
}