package info.prorabka.varamy.service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CompletableFuture;
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
        // ----- ОТПРАВКА ЧЕРЕЗ SMS-ШЛЮЗ С АСИНХРОННЫМ ОЖИДАНИЕМ -----
        if (!gatewayEnabled) {
            throw new RuntimeException("SMS-шлюз отключён. Невозможно отправить сообщение.");
        }

        try {
            CompletableFuture<Boolean> future = smsGatewayService.sendSmsViaGatewayAsync(phone, code, purpose.name());
            Boolean success = future.get(30, TimeUnit.SECONDS);
            if (!success) {
                throw new RuntimeException("Не удалось отправить SMS через шлюз (получен ответ false)");
            }
            log.info("SMS успешно отправлено через шлюз (асинхронно)");
        } catch (TimeoutException e) {
            log.error("Таймаут ожидания ответа от шлюза для телефона {}", phone);
            throw new RuntimeException("Превышено время ожидания ответа от SMS-шлюза");
        } catch (Exception e) {
            log.error("Ошибка при отправке через шлюз", e);
            throw new RuntimeException("Не удалось отправить SMS: " + e.getMessage());
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