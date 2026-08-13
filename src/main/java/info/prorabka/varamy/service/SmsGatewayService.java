package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.sms.SmsCommand;
import info.prorabka.varamy.dto.sms.SmsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsGatewayService {

    private final SimpMessagingTemplate messagingTemplate;
    private final List<PushService> pushServices;           // <-- добавить
    private final SimpUserRegistry userRegistry;   // <-- добавить
    private final Map<String, CompletableFuture<Boolean>> pendingRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

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

    public CompletableFuture<Boolean> sendSmsViaGatewayAsync(String phone, String code, String purpose) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        String requestId = UUID.randomUUID().toString();

        // Сохраняем future в мапу
        pendingRequests.put(requestId, future);

        // Создаем команду для шлюза
        SmsCommand command = new SmsCommand(requestId, phone, code, purpose);

        // Отправляем через WebSocket
        messagingTemplate.convertAndSendToUser(
                gatewayUserId,
                "/queue/sms-commands",
                command
        );

        // Таймаут через 30 секунд
        scheduler.schedule(() -> {
            CompletableFuture<Boolean> pending = pendingRequests.remove(requestId);
            if (pending != null && !pending.isDone()) {
                pending.complete(false);
                log.warn("Таймаут ожидания ответа от шлюза для requestId={}", requestId);
            }
        }, 30, TimeUnit.SECONDS);

        return future;
    }

    /**
     * Обработка ответа от шлюза
     */
    @MessageMapping("/sms-response")
    public void handleSmsResponse(SmsResponse response) {
        CompletableFuture<Boolean> future = pendingRequests.remove(response.getRequestId());
        if (future != null) {
            future.complete(response.isSuccess());
            log.info("Получен ответ от шлюза: requestId={}, success={}",
                    response.getRequestId(), response.isSuccess());
        } else {
            log.warn("Получен ответ от шлюза с неизвестным requestId={}", response.getRequestId());
        }
    }
}