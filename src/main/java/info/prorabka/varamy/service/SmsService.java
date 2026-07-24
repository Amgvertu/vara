// SmsService.java
package info.prorabka.varamy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    @Value("${sms.mock-mode:true}")  // В разработке true, в продакшене false
    private boolean mockMode;

    private static final SecureRandom random = new SecureRandom();

    /**
     * Генерация 6-значного кода подтверждения
     */
    public String generateVerificationCode() {
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * Отправка SMS с кодом подтверждения
     */
    public void sendVerificationCode(String phone, String code, String purpose) {
        if (mockMode) {
            // Режим разработки: выводим в лог и можно отправить через push-уведомление
            log.info("=== SMS MOCK MODE ===");
            log.info("Телефон: {}", phone);
            log.info("Код подтверждения: {}", code);
            log.info("Назначение: {}", purpose);
            log.info("=== END SMS MOCK ===");

            // Здесь можно добавить отправку push-уведомления
            // notificationService.sendPush(phone, "Ваш код подтверждения: " + code);
        } else {
            // Режим продакшена: реальная отправка SMS через провайдера
            // Пример для интеграции с SMS-провайдером (SmsRu, Twilio и т.д.)
            try {
                // smsProvider.send(phone, "Код подтверждения: " + code);
                log.info("SMS отправлено на номер {}: {}", phone, code);
            } catch (Exception e) {
                log.error("Ошибка отправки SMS на номер {}: {}", phone, e.getMessage());
                throw new RuntimeException("Не удалось отправить SMS", e);
            }
        }
    }
}