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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsGatewayService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;
    private final PushSender pushSender;

    private final Map<String, CompletableFuture<Boolean>> pendingRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Value("${sms.gateway.user-id:}")
    private String gatewayUserId;

    @Value("${sms.gateway.enabled:false}")
    private boolean gatewayEnabled;

    /**
     * Асинхронная отправка SMS через шлюз.
     * Если WebSocket шлюза не активен — сначала отправляется WAKE_UP
     * (RuStore → FCM → HMS), затем ожидается подключение до 25 секунд,
     * и только потом отправляется команда.
     */
    public CompletableFuture<Boolean> sendSmsViaGatewayAsync(String phone, String code, String purpose) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (!gatewayEnabled) {
            log.error("SMS-шлюз отключён (sms.gateway.enabled=false)");
            future.complete(false);
            return future;
        }
        if (gatewayUserId == null || gatewayUserId.isEmpty()) {
            log.error("Не задан sms.gateway.user-id");
            future.complete(false);
            return future;
        }

        String requestId = UUID.randomUUID().toString();
        pendingRequests.put(requestId, future);

        // Вся работа — в фоне, чтобы не блокировать вызывающий поток
        scheduler.execute(() -> {
            try {
                // 1. Убеждаемся, что шлюз подключён (иначе — wake-up + ожидание)
                boolean ready = ensureGatewayConnected();
                if (!ready) {
                    pendingRequests.remove(requestId);
                    future.complete(false);
                    return;
                }

                // 2. Отправляем команду
                SmsCommand command = new SmsCommand(requestId, phone, code, purpose);
                messagingTemplate.convertAndSendToUser(
                        gatewayUserId, "/queue/sms-commands", command);
                log.info("SMS-команда отправлена шлюзу (requestId={}, phone={})",
                        requestId, phone);

                // 3. Ждём ответа от шлюза 15 секунд
                scheduler.schedule(() -> {
                    CompletableFuture<Boolean> p = pendingRequests.remove(requestId);
                    if (p != null && !p.isDone()) {
                        p.complete(false);
                        log.warn("Таймаут ответа от шлюза (requestId={})", requestId);
                    }
                }, 15, TimeUnit.SECONDS);

            } catch (Exception e) {
                log.error("Ошибка отправки SMS через шлюз", e);
                pendingRequests.remove(requestId);
                future.complete(false);
            }
        });

        return future;
    }

    /**
     * Проверяет активную WebSocket-сессию шлюза.
     * Если её нет — отправляет WAKE_UP и ждёт подключения до 25 секунд.
     */
    private boolean ensureGatewayConnected() {
        if (hasSession()) {
            log.info("WebSocket шлюза уже активен");
            return true;
        }

        log.warn("WebSocket шлюза не активен — отправляем WAKE_UP");
        try {
            pushSender.sendWakeUp(UUID.fromString(gatewayUserId));
        } catch (Exception e) {
            log.error("Ошибка отправки WAKE_UP", e);
        }

        long deadline = System.currentTimeMillis() + 25_000L;
        while (System.currentTimeMillis() < deadline) {
            if (hasSession()) {
                log.info("Шлюз подключился после WAKE_UP");
                // Небольшая пауза, чтобы клиент успел подписаться на очередь
                try { Thread.sleep(500); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
            try { Thread.sleep(500); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.error("Шлюз не подключился за 25 секунд после WAKE_UP");
        return false;
    }

    private boolean hasSession() {
        SimpUser user = userRegistry.getUser(gatewayUserId);
        return user != null && user.hasSessions();
    }

    /**
     * Обработка ответа от шлюза.
     */
    @MessageMapping("/sms-response")
    public void handleSmsResponse(SmsResponse response) {
        CompletableFuture<Boolean> future = pendingRequests.remove(response.getRequestId());
        if (future != null) {
            future.complete(response.isSuccess());
            log.info("Ответ от шлюза: requestId={}, success={}",
                    response.getRequestId(), response.isSuccess());
        } else {
            log.warn("Получен ответ с неизвестным requestId={}", response.getRequestId());
        }
    }
}