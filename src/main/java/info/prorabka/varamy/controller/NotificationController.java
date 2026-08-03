package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.request.AddSubscriptionRequest;
import info.prorabka.varamy.dto.request.UpdateNotificationSettingsRequest;
import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.dto.response.NotificationResponse;
import info.prorabka.varamy.dto.response.NotificationSettingsResponse;
import info.prorabka.varamy.security.SecurityUser;
import info.prorabka.varamy.service.NotificationService;
import info.prorabka.varamy.service.PushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Уведомления", description = "Управление настройками и получение уведомлений")
public class NotificationController {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;  // для отладки
    private final List<PushService> pushServices;

    // ========== НАСТРОЙКИ ==========

    @GetMapping("/settings")
    @Operation(summary = "Получить настройки уведомлений текущего пользователя")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getSettings(
            @AuthenticationPrincipal SecurityUser currentUser) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getSettings(currentUser.getId())));
    }

    @PatchMapping("/settings")
    @Operation(summary = "Обновить настройки уведомлений (частично)")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateSettings(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody UpdateNotificationSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.updateSettings(currentUser.getId(), request)));
    }

    // ========== ПОДПИСКИ ==========

    @PostMapping("/subscriptions")
    @Operation(summary = "Добавить подписку на тип/подтип объявления")
    public ResponseEntity<ApiResponse<Void>> addSubscription(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody AddSubscriptionRequest request) {
        notificationService.addSubscription(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Подписка добавлена", null));
    }

    @DeleteMapping("/subscriptions")
    @Operation(summary = "Удалить подписку на тип/подтип объявления")
    public ResponseEntity<ApiResponse<Void>> removeSubscription(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam Integer type,
            @RequestParam Integer subType) {
        notificationService.removeSubscription(currentUser.getId(), type, subType);
        return ResponseEntity.ok(ApiResponse.success("Подписка удалена", null));
    }

    // ========== ПОЛУЧЕНИЕ УВЕДОМЛЕНИЙ ==========

    @GetMapping
    @Operation(summary = "Получить список уведомлений (с пагинацией)")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam(required = false) Boolean onlyUnread,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<NotificationResponse> page = notificationService.getNotifications(currentUser.getId(), pageable, onlyUnread);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Получить количество непрочитанных уведомлений")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal SecurityUser currentUser) {
        long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PatchMapping("/mark-read")
    @Operation(summary = "Отметить уведомления как прочитанные")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestBody List<Long> notificationIds) {
        notificationService.markAsRead(currentUser.getId(), notificationIds);
        return ResponseEntity.ok(ApiResponse.success("Уведомления отмечены прочитанными", null));
    }

    // ========== ТЕСТОВЫЕ ЭНДПОИНТЫ ==========

    @PostMapping("/test-public")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "ТЕСТ: Отправить публичное уведомление в /topic/notifications")
    public ResponseEntity<ApiResponse<Void>> sendTestPublicNotification() {
        messagingTemplate.convertAndSend(
                "/topic/notifications",
                Map.of("type", "TEST_PUBLIC", "content", "Тестовое публичное уведомление от " + LocalDateTime.now())
        );
        return ResponseEntity.ok(ApiResponse.success("Тестовое публичное уведомление отправлено", null));
    }

    @PostMapping("/test-private")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "ТЕСТ: Отправить личное уведомление в /user/queue/notifications")
    public ResponseEntity<ApiResponse<Void>> sendTestPrivateNotification(@AuthenticationPrincipal SecurityUser currentUser) {
        // Проверка активной сессии (для отладки)
        SimpUser simpUser = userRegistry.getUser(currentUser.getId().toString());
        if (simpUser == null || !simpUser.hasSessions()) {
            log.warn("Нет активной WebSocket-сессии для пользователя {}", currentUser.getId());
            // ИСПРАВЛЕНО: убран второй аргумент null
            return ResponseEntity.badRequest().body(ApiResponse.error("Нет активной WebSocket-сессии"));
        }
        log.info("Пользователь {} имеет активную сессию, отправляем уведомление", currentUser.getId());

        messagingTemplate.convertAndSendToUser(
                currentUser.getId().toString(),
                "/queue/notifications",
                Map.of("type", "TEST_PRIVATE", "content", "Тестовое личное уведомление от " + LocalDateTime.now())
        );
        return ResponseEntity.ok(ApiResponse.success("Тестовое личное уведомление отправлено", null));
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<String>>> getActiveSessions() {
        List<String> sessions = userRegistry.getUsers().stream()
                .flatMap(u -> u.getSessions().stream())
                .map(s -> s.getId())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    // ========== ТЕСТОВЫЙ ЭНДПОИНТ ДЛЯ ПРОВЕРКИ PUSH ==========

    @PostMapping("/test-push")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "ТЕСТ: Отправить тестовое push-уведомление")
    public ResponseEntity<ApiResponse<Void>> testPush(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam String title,
            @RequestParam String body) {
        log.info("🔵 Тестовый push для пользователя {}: title={}, body={}", currentUser.getId(), title, body);

        for (PushService pushService : pushServices) {
            try {
                pushService.sendNotification(currentUser.getId(), title, body);
                log.info("✅ Отправлено через {}", pushService.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("❌ Ошибка через {}: {}", pushService.getClass().getSimpleName(), e.getMessage(), e);
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Тестовое уведомление отправлено", null));
    }

}