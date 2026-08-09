// src/main/java/info/prorabka/varamy/service/RuStorePushService.java
package info.prorabka.varamy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
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
            log.warn("No active RuStore tokens for user {}", userId);
            return;
        }
        // RuStore не поддерживает data-сообщения, как FCM, поэтому используем обычное уведомление с "тишиной"
        for (String token : tokens) {
            sendRuStorePush(token, null, null, "WAKE_UP");
        }
    }

    @Override
    public void sendNotification(UUID userId, String title, String body) {
        List<String> tokens = tokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.warn("No active RuStore tokens for user {}", userId);
            return;
        }
        for (String token : tokens) {
            sendRuStorePush(token, title, body, "REAL");
        }
    }

    private void sendRuStorePush(String token, String title, String body, String type) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);

            // Формируем payload по документации VK Cloud Push
            var payload = new VkPushPayload();
            payload.setProjectId(projectId);
            payload.setToken(token);
            payload.setTitle(title);
            payload.setBody(body);
            payload.setData("{\"type\":\"" + type + "\"}");

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            ResponseEntity<String> response = restTemplate.exchange(pushUrl, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("RuStore push sent to token {}", token);
            } else {
                log.warn("RuStore push failed: {}, body: {}", response.getStatusCode(), response.getBody());
                // Если токен невалиден (400/401), деактивируем его (но у нас нет userId в этом контексте, можно игнорировать)
            }
        } catch (Exception e) {
            log.error("Error sending RuStore push", e);
        }
    }

    // Вспомогательный DTO для VK Cloud Push
    static class VkPushPayload {
        public String projectId;
        public String token;
        public String title;
        public String body;
        public String data;
        // геттеры/сеттеры (или просто public поля)
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public void setToken(String token) { this.token = token; }
        public void setTitle(String title) { this.title = title; }
        public void setBody(String body) { this.body = body; }
        public void setData(String data) { this.data = data; }
    }
}
