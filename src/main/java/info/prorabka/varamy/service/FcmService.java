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
public class FcmService implements PushService {

    private final FcmTokenService fcmTokenService;

    @Override
    public void sendWakeUpNotification(UUID userId) {
        List<String> tokens = fcmTokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.warn("No active FCM tokens for user {}", userId);
            return;
        }
        for (String token : tokens) {
            Message message = Message.builder()
                    .setToken(token)
                    .putData("type", "WAKE_UP")
                    .build();
            try {
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Wake-up FCM sent to user {} (token: {}), response: {}", userId, token, response);
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    log.warn("FCM token unregistered: {}", token);
                    fcmTokenService.unregisterToken(userId, token);
                } else {
                    log.error("Failed to send FCM: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void sendNotification(UUID userId, String title, String body) {
        List<String> tokens = fcmTokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.warn("No active FCM tokens for user {}", userId);
            return;
        }
        for (String token : tokens) {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", "REAL")
                    .build();
            try {
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("FCM notification sent to user {}, token {}, response: {}", userId, token, response);
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    log.warn("FCM token unregistered: {}", token);
                    fcmTokenService.unregisterToken(userId, token);
                } else {
                    log.error("Failed to send FCM: {}", e.getMessage());
                }
            }
        }
    }


}