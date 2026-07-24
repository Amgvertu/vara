package info.prorabka.varamy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
public class SmsRuService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sms.provider.enabled:false}")
    private boolean enabled;

    @Value("${sms.provider.url:https://sms.ru/sms/send}")
    private String url;

    @Value("${sms.provider.api_id:}")
    private String apiId;

    @Value("${sms.provider.sender:}")
    private String sender;

    public boolean sendSms(String phone, String code, String purpose) {
        if (!enabled || apiId == null || apiId.isEmpty()) {
            log.warn("SMS.ru провайдер отключён или не задан api_id");
            return false;
        }

        try {
            // Убираем '+' из номера
            String cleanPhone = phone.replaceFirst("^\\+", "");
            String message = "Код подтверждения: " + code;

            // Используем UriComponentsBuilder для безопасного построения URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("api_id", apiId)
                    .queryParam("to", cleanPhone)
                    .queryParam("msg", message)   // UriComponentsBuilder автоматически закодирует в UTF-8
                    .queryParam("json", 1)
                    .queryParam("charset", "utf-8"); // явно указываем кодировку

            if (sender != null && !sender.isEmpty()) {
                builder.queryParam("from", sender);
            }

            String fullUrl = builder.build(false).toUriString(); // false – не кодировать повторно
            log.debug("SMS.ru запрос: {}", fullUrl);

            ResponseEntity<Map> response = restTemplate.getForEntity(fullUrl, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("SMS.ru вернул код: {}", response.getStatusCode());
                return false;
            }

            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.error("SMS.ru ответ пустой");
                return false;
            }

            log.debug("SMS.ru ответ: {}", body);

            String status = (String) body.get("status");
            if (!"OK".equals(status)) {
                log.error("SMS.ru статус не OK: {}", body);
                return false;
            }

            Map<String, Object> smsMap = (Map<String, Object>) body.get("sms");
            if (smsMap == null) {
                log.error("SMS.ru не содержит 'sms' в ответе");
                return false;
            }

            // Ищем результат для номера
            Map<String, Object> phoneResult = null;
            for (Map.Entry<String, Object> entry : smsMap.entrySet()) {
                String key = entry.getKey();
                if (key.equals(cleanPhone) || key.equals(phone) || key.equals(phone.replaceFirst("^\\+", ""))) {
                    phoneResult = (Map<String, Object>) entry.getValue();
                    break;
                }
            }

            if (phoneResult == null) {
                log.error("SMS.ru не содержит результат для номера {}", phone);
                return false;
            }

            String resultStatus = (String) phoneResult.get("status");
            Number statusCode = (Number) phoneResult.get("status_code");

            if ("OK".equals(resultStatus) && statusCode != null && statusCode.intValue() == 100) {
                log.info("SMS успешно отправлено через SMS.ru. Номер: {}, код: {}", phone, code);
                return true;
            } else {
                log.error("SMS.ru ошибка: {}", phoneResult);
                return false;
            }

        } catch (Exception e) {
            log.error("Ошибка при отправке SMS через SMS.ru", e);
            return false;
        }
    }
}