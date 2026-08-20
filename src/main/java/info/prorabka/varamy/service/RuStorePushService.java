package info.prorabka.varamy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

    // Хранилище для OAuth-токена
    private String cachedAccessToken;
    private long tokenExpiryTime;

    /**
     * Получение OAuth-токена для RuStore (Client Credentials Flow)
     */
    private synchronized String getAccessToken() {
        long now = System.currentTimeMillis();

        // Если токен ещё действителен (с запасом 5 минут)
        if (cachedAccessToken != null && now < tokenExpiryTime - 300000) {
            return cachedAccessToken;
        }

        log.info("🔄 Получение нового OAuth-токена для RuStore...");

        try {
            // URL для получения токена
            String tokenUrl = "https://push-cloud.vk.com/api/v1/oauth/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Параметры для Client Credentials Flow
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("client_id", "rustore_client_id"); // Замените на реальный client_id
            body.add("client_secret", apiKey); // Ваш API-ключ используется как client_secret

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode json = objectMapper.readTree(response.getBody());
                cachedAccessToken = json.get("access_token").asText();
                int expiresIn = json.get("expires_in").asInt();
                tokenExpiryTime = now + (expiresIn * 1000L);
                log.info("✅ RuStore OAuth-токен получен, истекает через {} сек", expiresIn);
                return cachedAccessToken;
            } else {
                log.error("❌ Ошибка получения RuStore OAuth-токена: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("❌ Исключение при получении RuStore OAuth-токена", e);
            return null;
        }
    }

    @Override
    public void sendWakeUpNotification(UUID userId) {
        List<String> tokens = tokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.warn("⚠️ Нет активных RuStore токенов для пользователя {}", userId);
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
            log.warn("⚠️ Нет активных RuStore токенов для пользователя {}", userId);
            return;
        }

        for (String token : tokens) {
            sendRuStorePush(token, title, body, "REAL");
        }
    }

    private void sendRuStorePush(String token, String title, String body, String type) {
        try {
            // Получаем OAuth-токен
            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("❌ Не удалось получить OAuth-токен для RuStore");
                return;
            }

            String url = String.format("https://vkpns.rustore.ru/v1/projects/%s/messages:send", projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken); // Используем OAuth-токен!

            // Формируем payload
            Map<String, Object> message = new HashMap<>();
            message.put("token", token);

            // Data (всегда отправляем)
            Map<String, String> data = new HashMap<>();
            data.put("type", type);
            if (title != null) data.put("title", title);
            if (body != null) data.put("body", body);
            message.put("data", data);

            // Notification (только если есть title и body)
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
                    url, HttpMethod.POST, request, String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ RuStore Push успешно отправлен");
            } else {
                log.error("❌ RuStore Push ошибка: status={}, body={}",
                        response.getStatusCode().value(), response.getBody());

                // Если токен недействителен - сбрасываем кеш
                if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    cachedAccessToken = null;
                    log.warn("🔄 OAuth-токен RuStore недействителен, сбрасываем кеш");
                }
            }
        } catch (Exception e) {
            log.error("❌ Ошибка отправки RuStore Push: {}", e.getMessage(), e);
        }
    }
}