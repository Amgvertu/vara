package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.request.SendVerificationCodeRequest;
import info.prorabka.varamy.dto.request.VerifyCodeRequest;
import info.prorabka.varamy.entity.VerificationCode;
import info.prorabka.varamy.exception.BadRequestException;
import info.prorabka.varamy.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final SmsService smsService;
    private final UserService userService;
    private final SmsGatewayService smsGatewayService;
    private final SmsRuService smsRuService;

    @Value("${sms.gateway.enabled:false}")
    private boolean gatewayEnabled;

    @Value("${sms.mock-mode:true}")
    private boolean mockMode; // уже есть

    @Value("${verification.code.expiry-minutes:5}")
    private int codeExpiryMinutes;

    // Возвращает сгенерированный код
    @Transactional
    public String sendVerificationCode(String phone, VerificationCode.VerificationPurpose purpose) {
        // Проверка для регистрации: телефон не должен быть занят
        if (purpose == VerificationCode.VerificationPurpose.REGISTRATION) {
            if (userService.isPhoneExists(phone)) {
                throw new BadRequestException("Пользователь с таким телефоном уже существует");
            }
        }

        // Проверка для смены телефона: новый телефон не должен быть занят
        if (purpose == VerificationCode.VerificationPurpose.PHONE_CHANGE) {
            if (userService.isPhoneExists(phone)) {
                throw new BadRequestException("Телефон уже используется другим пользователем");
            }
        }

        // Генерируем код
        String code = smsService.generateVerificationCode();

        // Инвалидируем все старые неподтверждённые коды для этого телефона и цели
        verificationCodeRepository.invalidateAllCodesForPhone(phone, purpose);

        // Создаём новую запись
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setPhone(phone);
        verificationCode.setCode(code);
        verificationCode.setPurpose(purpose);
        verificationCode.setExpiryDate(LocalDateTime.now().plusMinutes(codeExpiryMinutes));
        verificationCode.setUsed(false);
        verificationCodeRepository.save(verificationCode);

        // ----- ОТПРАВКА ЧЕРЕЗ SMS-ШЛЮЗ С ТРЕМЯ ПОПЫТКАМИ -----
        if (!gatewayEnabled) {
            throw new RuntimeException("SMS-шлюз отключён. Невозможно отправить сообщение.");
        }

        int maxAttempts = 3;
        long delayMs = 3000;
        String requestId = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            log.info("Попытка отправки SMS через шлюз: {} из {}, телефон: {}, код: {}", attempt, maxAttempts, phone, code);
            try {
                requestId = smsGatewayService.sendSmsViaGateway(phone, code, purpose.name());
                if (requestId != null) {
                    log.info("SMS успешно отправлено через шлюз, requestId: {}", requestId);
                    break;
                } else {
                    log.warn("Попытка {} отправки через шлюз вернула null", attempt);
                }
            } catch (Exception e) {
                log.error("Ошибка при попытке отправки через шлюз, попытка {}", attempt, e);
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Прервано ожидание между попытками отправки SMS", e);
                }
            }
        }

        if (requestId == null) {
            log.error("Не удалось отправить SMS после {} попыток для телефона {}", maxAttempts, phone);
            throw new RuntimeException("Не удалось отправить SMS. Попробуйте позже.");
        }

        // Возвращаем код (может понадобиться для тестов, но в продакшене не используется)
        return code;
    }

    @Transactional
    public boolean verifyCode(String phone, String code, VerificationCode.VerificationPurpose purpose) {
        VerificationCode verificationCode = verificationCodeRepository
                .findByPhoneAndCodeAndPurposeAndUsedFalse(phone, code, purpose)
                .orElseThrow(() -> new BadRequestException("Неверный код подтверждения"));

        if (verificationCode.isExpired()) {
            throw new BadRequestException("Код подтверждения истёк. Запросите новый код");
        }

        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        return true;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        verificationCodeRepository.deleteExpiredCodes(now);
        log.info("Очистка просроченных кодов подтверждения выполнена");
    }
}