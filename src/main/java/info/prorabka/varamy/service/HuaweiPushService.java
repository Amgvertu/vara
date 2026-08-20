package info.prorabka.varamy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.prorabka.varamy.config.HuaweiPushProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class HuaweiPushService implements PushService {

    private final HmsTokenService hmsTokenService;
    private final HuaweiPushProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedAccessToken;
    private long tokenExpiryTime;

    private synchronized String getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedAccessToken != null && now < tokenExpiryTime - 60000) {
            return cachedAccessToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());



        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getTokenUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
            );
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode json = objectMapper.readTree(response.getBody());
                cachedAccessToken = json.get("access_token").asText();
                int expiresIn = json.get("expires_in").asInt();
                tokenExpiryTime = now + expiresIn * 1000L;
                log.info("Huawei access token obtained, expires in {} sec", expiresIn);
                return cachedAccessToken;
            } else {
                log.error("Failed to get Huawei access token: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error obtaining Huawei access token", e);
            return null;
        }
    }

    @Override
    public void sendWakeUpNotification(UUID userId) {
        List<String> tokens = hmsTokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.warn("No active HMS tokens for user {}", userId);
            return;
        }
        for (String token : tokens) {
            sendHuaweiPush(userId, token, null, null, "WAKE_UP");
        }
    }

    @Override
    public void sendNotification(UUID userId, String title, String body) {
        List<String> tokens = hmsTokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.warn("No active HMS tokens for user {}", userId);
            return;
        }
        for (String token : tokens) {
            sendHuaweiPush(userId, token, title, body, "REAL");
        }
    }

    private void sendHuaweiPush(UUID userId, String token, String title, String body, String type) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("❌ Не удалось получить Huawei access token");
                return;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("token", new String[]{token});

            Map<String, String> data = new HashMap<>();
            data.put("type", type);
            if (title != null) data.put("title", title);
            if (body != null) data.put("body", body);
            message.put("data", data);

            if (title != null && body != null) {
                Map<String, String> notification = new HashMap<>();
                notification.put("title", title);
                notification.put("body", body);
                notification.put("clickAction", "OPEN_APP");
                message.put("notification", notification);
            }

            Map<String, Object> androidConfig = new HashMap<>();
            Map<String, Object> collapseKey = new HashMap<>();
            collapseKey.put("key", "msg");
            androidConfig.put("collapseKey", collapseKey);
            message.put("android", androidConfig);

            payload.put("message", message);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            String pushUrl = String.format(properties.getPushUrl(), properties.getAppId());
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    pushUrl, HttpMethod.POST, request, String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // Проверяем тело ответа
                try {
                    JsonNode json = objectMapper.readTree(response.getBody());
                    String code = json.has("code") ? json.get("code").asText() : null;
                    if ("80000000".equals(code)) {
                        log.info("✅ Huawei Push успешно отправлен пользователю {}", userId);
                    } else if ("80100000".equals(code)) {
                        // Недействительный токен
                        log.warn("🔴 Недействительный HMS токен: {}", token);
                        hmsTokenService.unregisterToken(userId, token);
                    } else {
                        log.error("❌ Huawei Push ошибка: code={}, body={}", code, response.getBody());
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Не удалось распарсить ответ Huawei: {}", response.getBody());
                }
            } else {
                log.error("❌ Huawei Push ошибка: status={}, body={}",
                        response.getStatusCode().value(), response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Ошибка отправки Huawei Push: {}", e.getMessage(), e);
        }
    }


}