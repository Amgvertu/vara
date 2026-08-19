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

    @Value("${rustore.push.project-id:}")
    private String projectId;

    @Value("${rustore.push.url:https://push-cloud.vk.com/api/v1/messages}")
    private String pushUrl;

    @Override
    public void sendWakeUpNotification(UUID userId) {
        List<String> tokens = tokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.warn("Нет активных RuStore токенов для пользователя {}", userId);
            return;
        }

        for (String token : tokens) {
            sendRuStorePush(token, null, null, "WAKE_UP");
        }
    }

    @Override
    public void sendNotification(UUID userId, String title, String body) {
        List<String> tokens = tokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.warn("Нет активных RuStore токенов для пользователя {}", userId);
            return;
        }

        for (String token : tokens) {
            sendRuStorePush(token, title, body, "REAL");
        }
    }

    private void sendRuStorePush(String token, String title, String body, String type) {
        try {
            // URL с projectId
            String url = String.format("https://vkpns.rustore.ru/v1/projects/%s/messages:send", projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);  // Bearer-токен

            // Формируем payload как в документации
            Map<String, Object> message = new HashMap<>();
            message.put("token", token);

            // Data
            Map<String, String> data = new HashMap<>();
            data.put("type", type);
            if (title != null) {
                data.put("title", title);
            }
            if (body != null) {
                data.put("body", body);
            }
            message.put("data", data);

            // Notification (только если есть title и body)
            if (title != null && body != null) {
                Map<String, String> notification = new HashMap<>();
                notification.put("title", title);
                notification.put("body", body);
                message.put("notification", notification);
            }

            // Оборачиваем в "message"
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", message);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ RuStore Push успешно отправлен: {}", response.getBody());
            } else {
                log.error("❌ RuStore Push ошибка: status={}, body={}",
                        response.getStatusCode().value(), response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Ошибка отправки RuStore Push: {}", e.getMessage());
        }
    }
}