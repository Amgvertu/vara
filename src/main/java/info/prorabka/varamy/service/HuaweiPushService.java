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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HuaweiPushService implements PushService {

    private final HuaweiPushProperties properties;
    private final HmsTokenService hmsTokenService;
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
                throw new RuntimeException("Failed to get Huawei access token");
            }
        } catch (Exception e) {
            log.error("Error obtaining Huawei access token", e);
            throw new RuntimeException("Error obtaining Huawei access token", e);
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
            sendHuaweiPush(token, null, null, "WAKE_UP");
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
            sendHuaweiPush(token, title, body, "REAL");
        }
    }

    private void sendHuaweiPush(String token, String title, String body, String type) {
        try {
            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            StringBuilder payload = new StringBuilder();
            payload.append("{");
            payload.append("\"payload\": {");
            payload.append("\"data\": \"").append(escapeJson("type=" + type)).append("\"");
            if (title != null && body != null) {
                payload.append(", \"notification\": {");
                payload.append("\"title\": \"").append(escapeJson(title)).append("\",");
                payload.append("\"body\": \"").append(escapeJson(body)).append("\"");
                payload.append("}");
            }
            payload.append("},");
            payload.append("\"token\": [\"").append(token).append("\"]");
            payload.append("}");

            HttpEntity<String> request = new HttpEntity<>(payload.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getPushUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Huawei push sent to token {}", token);
            } else {
                log.warn("Huawei push failed: {}, body: {}", response.getStatusCode(), response.getBody());
                // Если токен недействителен, можно деактивировать, но у нас нет userId в этом контексте.
                // Можно оставить как есть или доработать.
            }
        } catch (Exception e) {
            log.error("Error sending Huawei push", e);
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}