package info.prorabka.varamy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuStorePushService implements PushService {

    private final RuStoreTokenService tokenService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rustore.push.api-key:}")
    private String apiKey;

    @Value("${rustore.push.url:}")
    private String pushUrl;

    @Override
    public boolean sendWakeUpNotification(UUID userId) {
        List<String> tokens = tokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.warn("RuStore: нет активных токенов у пользователя {}", userId);
            return false;
        }
        boolean atLeastOneSent = false;
        for (String token : tokens) {
            if (sendRuStorePush(userId, token, null, null, "WAKE_UP")) {
                atLeastOneSent = true;
            }
        }
        return atLeastOneSent;
    }

    @Override
    public boolean sendNotification(UUID userId, String title, String body) {
        List<String> tokens = tokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.warn("RuStore: нет активных токенов у пользователя {}", userId);
            return false;
        }

        boolean atLeastOneSent = false;
        for (String token : tokens) {
            if (sendRuStorePush(userId, token, title, body, "REAL")) {
                atLeastOneSent = true;
            }
        }
        return atLeastOneSent;
    }

    /**
     * @return true, если отправка успешна
     */
    private boolean sendRuStorePush(UUID userId, String deviceToken,
                                    String title, String body, String type) {
        if (pushUrl == null || pushUrl.isEmpty()) {
            log.error("RuStore: не задан rustore.push.url");
            return false;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("RuStore: не задан rustore.push.api-key");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey); // сервисный токен из конфига

            Map<String, Object> message = new HashMap<>();
            message.put("token", deviceToken);

            Map<String, String> data = new HashMap<>();
            data.put("type", type);
            if (title != null) data.put("title", title);
            if (body != null) data.put("body", body);
            message.put("data", data);

            // Показываем системное уведомление
            if (title != null && body != null) {
                Map<String, String> notification = new HashMap<>();
                notification.put("title", title);
                notification.put("body", body);
                message.put("notification", notification);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("message", message);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    pushUrl, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("RuStore отправлен пользователю {}", userId);
                return true;
            } else {
                log.error("RuStore ошибка: status={}, body={}",
                        response.getStatusCode().value(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("RuStore исключение при отправке: {}", e.getMessage(), e);
            return false;
        }
    }
}