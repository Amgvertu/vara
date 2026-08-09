package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.sms.SmsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsGatewayService {

    private final SimpMessagingTemplate messagingTemplate;
    private final List<PushService> pushServices;           // <-- добавить
    private final SimpUserRegistry userRegistry;   // <-- добавить

    @Value("${sms.gateway.user-id:}")
    private String gatewayUserId;

    @Value("${sms.gateway.enabled:false}")
    private boolean gatewayEnabled;

    /**
     * Отправляет команду на SMS-шлюз.
     * @param phone номер телефона
     * @param code код подтверждения
     * @param purpose цель (REGISTRATION, PASSWORD_RESET, PHONE_CHANGE)
     * @return requestId или null в случае ошибки
     */
    public String sendSmsViaGateway(String phone, String code, String purpose) {
        if (!gatewayEnabled) {
            log.warn("SMS Gateway is disabled. Enable via sms.gateway.enabled=true");
            return null;
        }
        if (gatewayUserId == null || gatewayUserId.isEmpty()) {
            log.error("SMS Gateway user ID not configured. Set sms.gateway.user-id");
            return null;
        }

        String requestId = UUID.randomUUID().toString();
        SmsCommand command = new SmsCommand(requestId, phone, code, purpose);

        // Проверяем, есть ли активная WebSocket-сессия у шлюза
        SimpUser user = userRegistry.getUser(gatewayUserId);
        boolean hasSession = (user != null && user.hasSessions());

        if (hasSession) {
            // Сессия есть – отправляем через WebSocket
            try {
                messagingTemplate.convertAndSendToUser(gatewayUserId, "/queue/sms-commands", command);
                log.info("SMS command sent to gateway for phone {} (requestId={})", phone, requestId);
                return requestId;
            } catch (Exception e) {
                log.error("Failed to send SMS command via WebSocket", e);
                return null;
            }
        } else {
            // Сессии нет – отправляем пробуждение через все доступные push-каналы
            log.warn("No active WebSocket session for gateway user {}, sending wake-up via all push services", gatewayUserId);
            UUID userId = UUID.fromString(gatewayUserId);
            for (PushService pushService : pushServices) {
                try {
                    pushService.sendWakeUpNotification(userId);
                    log.info("Wake-up sent via {} to gateway user {}", pushService.getClass().getSimpleName(), gatewayUserId);
                } catch (Exception e) {
                    log.error("Failed to send wake-up via {}: {}", pushService.getClass().getSimpleName(), e.getMessage());
                }
            }
            return null;
        }
    }
}