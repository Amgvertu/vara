package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.request.AddSubscriptionRequest;
import info.prorabka.varamy.dto.request.UpdateNotificationSettingsRequest;
import info.prorabka.varamy.dto.response.*;
import info.prorabka.varamy.entity.*;
import info.prorabka.varamy.exception.BadRequestException;
import info.prorabka.varamy.exception.ResourceNotFoundException;
import info.prorabka.varamy.mapper.AdMapper;
import info.prorabka.varamy.mapper.ResponseMapper;
import info.prorabka.varamy.repository.*;
import info.prorabka.varamy.service.NotificationRetryScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSettingsRepository settingsRepository;
    private final UserNotificationSubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final CityRepository cityRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;   // для проверки активных сессий
    private final List<PushService> pushServices;
    private final Map<UUID, Long> lastFcmSentTime = new ConcurrentHashMap<>();
    private static final long FCM_COOLDOWN_MS = 1 * 60 * 1000; // 1 минут
    private final ResponseRepository responseRepository;
    private final ResponseMapper responseMapper;
    private final AdMapper adMapper;
    private final AdRepository adRepository;
    private final FcmTokenService fcmTokenService;
    private final HmsTokenService hmsTokenService;
    private final RuStoreTokenService ruStoreTokenService;
    private final NotificationRetryScheduler retryScheduler;
    private final Map<String, Long> lastSentMap = new ConcurrentHashMap<>();

    // ========== НАСТРОЙКИ ==========

    @Transactional
    public NotificationSettingsResponse getSettings(UUID userId) {
        NotificationSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        List<SubscriptionResponse> subscriptions = subscriptionRepository.findByIdUserId(userId).stream()
                .map(sub -> new SubscriptionResponse(sub.getId().getType(), sub.getId().getSubType()))
                .collect(Collectors.toList());

        CitySimpleResponse cityResp = null;
        if (settings.getNotificationCity() != null) {
            cityResp = new CitySimpleResponse(settings.getNotificationCity().getId(),
                    settings.getNotificationCity().getName());
        }

        return NotificationSettingsResponse.builder()
                .notifyOnResponseToMyAd(settings.isNotifyOnResponseToMyAd())
                .notifyOnMyResponseAccepted(settings.isNotifyOnMyResponseAccepted())
                .notifyNewAdsInCity(settings.isNotifyNewAdsInCity())
                .notificationCity(cityResp)
                .subscriptions(subscriptions)
                .build();
    }

    @Transactional
    public NotificationSettingsResponse updateSettings(UUID userId, UpdateNotificationSettingsRequest request) {
        log.debug("Updating settings for user: {}", userId);
        NotificationSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        if (request.getNotifyOnResponseToMyAd() != null) {
            settings.setNotifyOnResponseToMyAd(request.getNotifyOnResponseToMyAd());
        }
        if (request.getNotifyOnMyResponseAccepted() != null) {
            settings.setNotifyOnMyResponseAccepted(request.getNotifyOnMyResponseAccepted());
        }
        if (request.getNotifyNewAdsInCity() != null) {
            settings.setNotifyNewAdsInCity(request.getNotifyNewAdsInCity());
        }
        if (request.getNotificationCityId() != null) {
            City city = cityRepository.findById(request.getNotificationCityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Город не найден"));
            settings.setNotificationCity(city);
        } else {
            settings.setNotificationCity(null);
        }
        settings.setUpdatedAt(LocalDateTime.now());
        settingsRepository.save(settings);
        return getSettings(userId);
    }

    private NotificationSettings createDefaultSettings(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        NotificationSettings settings = new NotificationSettings();
        settings.setUserId(userId);
        settings.setNotifyOnResponseToMyAd(true);
        settings.setNotifyOnMyResponseAccepted(true);
        settings.setNotifyNewAdsInCity(false);
        settings.setNotificationCity(null);
        settings.setUpdatedAt(LocalDateTime.now());
        log.debug("Creating new settings for user: {}", userId);
        return settingsRepository.save(settings);
    }

    // ========== ПОДПИСКИ НА ТИПЫ ==========

    @Transactional
    public void addSubscription(UUID userId, AddSubscriptionRequest request) {
        User user = userService.getUserById(userId);
        UserNotificationSubscription.SubscriptionId id =
                new UserNotificationSubscription.SubscriptionId(userId, request.getType(), request.getSubType());
        if (subscriptionRepository.existsById(id)) {
            throw new BadRequestException("Такая подписка уже существует");
        }
        UserNotificationSubscription subscription = new UserNotificationSubscription();
        subscription.setId(id);
        subscription.setUser(user);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void removeSubscription(UUID userId, Integer type, Integer subType) {
        subscriptionRepository.deleteByUserIdAndTypeAndSubType(userId, type, subType);
    }

    // ========== УВЕДОМЛЕНИЯ (СОЗДАНИЕ И ОТПРАВКА) ==========

    @Transactional
    public void createAndSendNotification(UUID userId, String type, String content, UUID relatedEntityId) {
        log.info(">>> Creating notification for user {}: type={}, content={}", userId, type, content);

        // 1. Получаем настройки пользователя
        NotificationSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        // 2. Проверяем, нужно ли отправлять уведомление для данного типа
        boolean shouldSend = shouldSendNotification(type, settings);
        if (!shouldSend) {
            log.info("Уведомления типа {} отключены для пользователя {}", type, userId);
            // Все равно сохраняем в БД, чтобы пользователь мог увидеть в истории
            saveNotification(userId, type, content, relatedEntityId);
            return;
        }

        String key = userId.toString() + "_" + type;
        Long lastSent = lastSentMap.get(key);
        long now = System.currentTimeMillis();
        if (lastSent != null && (now - lastSent) < 1000) { // 5 секунд
            log.info("⏳ Пропускаем дублирующее уведомление для {} типа {}, отправлено менее 5 сек назад", userId, type);
            return;
        }
        lastSentMap.put(key, now);

        // 3. Сохраняем уведомление в БД
        User user = userService.getUserById(userId);
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setContent(content);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification = notificationRepository.save(notification);
        log.info("Notification saved to DB with id={}", notification.getId());

        // 4. Проверяем наличие активной WebSocket-сессии
        SimpUser simpUser = userRegistry.getUser(userId.toString());
        boolean hasSession = (simpUser != null && simpUser.hasSessions());

        if (hasSession) {
            // Сессия есть – отправляем через WebSocket немедленно
            sendViaWebSocket(userId, notification);
            sendViaPushIfHasTokens(userId, type, content, notification);
        } else {
            // Сессии нет – проверяем наличие push-токенов и отправляем
            sendViaPushIfHasTokens(userId, type, content, notification);
        }
    }

    /**
     * Проверяет, нужно ли отправлять уведомление для данного типа
     */
    private boolean shouldSendNotification(String type, NotificationSettings settings) {
        switch (type) {
            case "RESPONSE":
            case "RESPONSE_WITHDRAWN":
                return settings.isNotifyOnResponseToMyAd();
            case "RESPONSE_ACCEPTED":
            case "RESPONSE_ACCEPTANCE_CANCELLED":
            case "RESPONSE_REJECTED":
                return settings.isNotifyOnMyResponseAccepted();
            case "NEW_AD":
                return settings.isNotifyNewAdsInCity();
            case "AD_STATUS_CHANGED":
            case "ADMIN_MESSAGE":
                return true; // Всегда отправляем
            default:
                return true; // Для неизвестных типов отправляем
        }
    }

    /**
     * Сохраняет уведомление без отправки
     */
    private Long saveNotification(UUID userId, String type, String content, UUID relatedEntityId) {
        User user = userService.getUserById(userId);
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setContent(content);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification = notificationRepository.save(notification);

        log.info("📝 Уведомление сохранено в БД с id={}", notification.getId());
        return notification.getId(); // Возвращаем Long ID уведомления
    }

    /**
     * Отправляет уведомление через WebSocket
     */
    private void sendViaWebSocket(UUID userId, Notification notification) {
        try {
            NotificationResponse response = toResponse(notification);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    response
            );
            log.info("✅ WebSocket notification sent to user {}", userId);
        } catch (Exception e) {
            log.error("❌ Failed to send WebSocket notification to user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Отправляет уведомление через push, если есть активные токены
     */
    private void sendViaPushIfHasTokens(UUID userId, String type, String content, Notification notification) {
        // Проверяем, есть ли у пользователя активные токены
        boolean hasAnyToken = checkUserHasAnyToken(userId);

        if (!hasAnyToken) {
            log.info("ℹ️ User {} has no push tokens, notification saved only in DB", userId);
            return;
        }

        // Проверяем, не отправляли ли недавно (защита от спама)
        if (!shouldSendPushNow(userId)) {
            log.debug("⏳ Push already sent recently for user {}, skipping", userId);
            return;
        }

        // Определяем заголовок и тело для push-уведомления
        String title = getPushTitle(type);
        String body = getPushBody(type, content);

        // Отправляем через все доступные push-сервисы
        int sentCount = 0;
        for (PushService pushService : pushServices) {
            try {
                sendPushNotification(userId, type, content, notification.getRelatedEntityId(), notification.getId());
                sentCount++;
                log.info("✅ Push sent via {} to user {}", pushService.getClass().getSimpleName(), userId);
            } catch (Exception e) {
                log.error("❌ Error sending push via {} to user {}: {}",
                        pushService.getClass().getSimpleName(), userId, e.getMessage());
            }
        }

        if (sentCount > 0) {
            lastFcmSentTime.put(userId, System.currentTimeMillis());
            log.info("📱 Push notifications sent to user {} via {} services", userId, sentCount);
        } else {
            log.warn("⚠️ Failed to send push to user {} via all services", userId);
        }
    }

    /**
     * Проверяет наличие любого активного push-токена у пользователя
     */
    private boolean checkUserHasAnyToken(UUID userId) {
        // Проверяем FCM
        List<String> fcmTokens = fcmTokenService.getActiveTokensForUser(userId);
        if (!fcmTokens.isEmpty()) {
            log.debug("User {} has {} FCM tokens", userId, fcmTokens.size());
            return true;
        }

        // Проверяем HMS
        List<String> hmsTokens = hmsTokenService.getActiveTokensForUser(userId);
        if (!hmsTokens.isEmpty()) {
            log.debug("User {} has {} HMS tokens", userId, hmsTokens.size());
            return true;
        }

        // Проверяем RuStore
        List<String> ruStoreTokens = ruStoreTokenService.getActiveTokensForUser(userId);
        if (!ruStoreTokens.isEmpty()) {
            log.debug("User {} has {} RuStore tokens", userId, ruStoreTokens.size());
            return true;
        }

        return false;
    }

    /**
     * Проверяет, можно ли отправлять push сейчас (защита от спама)
     */
    private boolean shouldSendPushNow(UUID userId) {
        Long lastSent = lastFcmSentTime.get(userId);
        if (lastSent == null) {
            return true;
        }
        // Не чаще чем раз в 5 минут для одного пользователя
        return System.currentTimeMillis() - lastSent > 1 * 60 * 1000;
    }

    /**
     * Возвращает заголовок для push-уведомления в зависимости от типа
     */
    private String getPushTitle(String type) {
        switch (type) {
            case "RESPONSE":
                return "Новый отклик";
            case "RESPONSE_ACCEPTED":
                return "Отклик принят 🎉";
            case "RESPONSE_REJECTED":
                return "Отклик отклонен";
            case "RESPONSE_ACCEPTANCE_CANCELLED":
                return "Отмена принятия";
            case "RESPONSE_WITHDRAWN":
                return "Отклик отозван";
            case "NEW_AD":
                return "Новое объявление 📢";
            case "AD_STATUS_CHANGED":
                return "Статус объявления изменен";
            case "ADMIN_MESSAGE":
                return "Сообщение от администратора";
            default:
                return "Новое уведомление";
        }
    }

    /**
     * Формирует тело push-уведомления
     */
    private String getPushBody(String type, String content) {
        // Если content содержит текст - используем его
        if (content != null && !content.isEmpty()) {
            // Обрезаем длинные сообщения (ограничение push-сервисов ~ 200 символов)
            if (content.length() > 180) {
                return content.substring(0, 177) + "...";
            }
            return content;
        }

        // Если content пустой - используем заглушку
        return "Новое уведомление в приложении";
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .content(n.getContent())
                .relatedEntityId(n.getRelatedEntityId())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    // ========== ПОЛУЧЕНИЕ УВЕДОМЛЕНИЙ (REST) ==========

    public Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable, Boolean onlyUnread) {
        Page<Notification> page;
        if (Boolean.TRUE.equals(onlyUnread)) {
            page = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        } else {
            page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return page.map(this::toResponse);
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) return;
        int updated = notificationRepository.markAsRead(userId, notificationIds);
        log.info("Marked {} notifications as read for user {}", updated, userId);
    }

    // ========== ЛОГИКА ГЕНЕРАЦИИ ПО СОБЫТИЯМ ==========

    public void onResponseCreated(UUID adAuthorId, UUID adId, UUID responseId, String responderName) {
        log.info("onResponseCreated: adAuthorId={}, responderName={}", adAuthorId, responderName);
        NotificationSettings settings = settingsRepository.findByUserId(adAuthorId)
                .orElseGet(() -> createDefaultSettings(adAuthorId));
        if (settings.isNotifyOnResponseToMyAd()) {
            Ad ad = adRepository.findById(adId).orElse(null);
            if (ad != null) {
                String details = getAdDetails(ad);
                String content = String.format("Пользователь %s откликнулся на ваше объявление %s", responderName, details);
                createAndSendNotification(adAuthorId, "RESPONSE", content, responseId);
            }
        } else {
            log.debug("User {} has notifications for RESPONSE disabled", adAuthorId);
        }
    }

    public void onResponseAccepted(UUID responderId, UUID adId, UUID responseId) {
        log.info("onResponseAccepted: responderId={}, adId={}", responderId, adId);
        NotificationSettings settings = settingsRepository.findByUserId(responderId)
                .orElseGet(() -> createDefaultSettings(responderId));
        if (settings.isNotifyOnMyResponseAccepted()) {
            Ad ad = adRepository.findById(adId).orElse(null);
            if (ad != null) {
                String details = getAdDetails(ad);
                String content = String.format("Ваш отклик на объявление %s принят", details);
                createAndSendNotification(responderId, "RESPONSE_ACCEPTED", content, responseId);
            }
        } else {
            log.debug("User {} has notifications for RESPONSE_ACCEPTED disabled", responderId);
        }
    }

    //Уведомление об отмене принятия отклика (APPROVED -> PENDING)
    public void onResponseAcceptanceCancelled(UUID responderId, UUID adId, UUID responseId) {
        log.info("onResponseAcceptanceCancelled: responderId={}, adId={}", responderId, adId);
        NotificationSettings settings = settingsRepository.findByUserId(responderId)
                .orElseGet(() -> createDefaultSettings(responderId));
        if (settings.isNotifyOnMyResponseAccepted()) {
            Ad ad = adRepository.findById(adId).orElse(null);
            if (ad != null) {
                String details = getAdDetails(ad);
                String content = String.format("Принятие вашего отклика на объявление %s отменено", details);
                createAndSendNotification(responderId, "RESPONSE_ACCEPTANCE_CANCELLED", content, responseId);
            }
        } else {
            log.debug("User {} has notifications for RESPONSE_ACCEPTANCE_CANCELLED disabled", responderId);
        }
    }

    // Уведомление об отклонении отклика (REJECTED)
    public void onResponseRejected(UUID responderId, UUID adId, UUID responseId) {
        log.info("onResponseRejected: responderId={}, adId={}", responderId, adId);
        NotificationSettings settings = settingsRepository.findByUserId(responderId)
                .orElseGet(() -> createDefaultSettings(responderId));
        if (settings.isNotifyOnMyResponseAccepted()) {
            Ad ad = adRepository.findById(adId).orElse(null);
            if (ad != null) {
                String details = getAdDetails(ad);
                String content = String.format("Ваш отклик на объявление %s отклонён", details);
                createAndSendNotification(responderId, "RESPONSE_REJECTED", content, responseId);
            }
        } else {
            log.debug("User {} has notifications for RESPONSE_REJECTED disabled", responderId);
        }
    }


    public void onNewAdCreated(Ad newAd) {
        Long cityId = newAd.getCity().getId();
        Integer type = newAd.getType();
        Integer subType = newAd.getSubType();
        log.info("onNewAdCreated: cityId={}, type={}, subType={}", cityId, type, subType);

        List<User> interestedUsers = userService.findUsersForNewAdNotification(cityId, type, subType);
        log.info("Found {} interested users for new ad notification", interestedUsers.size());

        String description = getAdDescription(type, subType);
        String content = "Новое объявление «" + description + "»";

        for (User user : interestedUsers) {
            createAndSendNotification(user.getId(), "NEW_AD", content, newAd.getId());
        }
    }

    private boolean shouldSendFcm(UUID userId) {
        Long last = lastFcmSentTime.get(userId);
        if (last == null) {
            return true;
        }
        return System.currentTimeMillis() - last > FCM_COOLDOWN_MS;
    }

    public void onResponseWithdrawn(UUID adAuthorId, UUID adId, String responderName) {
        log.info("onResponseWithdrawn: adAuthorId={}, responderName={}", adAuthorId, responderName);
        NotificationSettings settings = settingsRepository.findByUserId(adAuthorId)
                .orElseGet(() -> createDefaultSettings(adAuthorId));
        if (settings.isNotifyOnResponseToMyAd()) {
            Ad ad = adRepository.findById(adId).orElse(null);
            if (ad != null) {
                String details = getAdDetails(ad);
                String content = String.format("Пользователь %s отозвал свой отклик на ваше объявление %s", responderName, details);
                createAndSendNotification(adAuthorId, "RESPONSE_WITHDRAWN", content, adId);
            }
        } else {
            log.debug("User {} has notifications for RESPONSE_WITHDRAWN disabled", adAuthorId);
        }
    }

    public void sendAdStatusUpdate(Ad ad) {
        AdResponse adResponse = adMapper.toResponse(ad);
        Map<String, Object> payload = Map.of(
                "type", "AD_UPDATED",
                "entityId", ad.getId().toString(),
                "payload", adResponse
        );
        // Отправляем и в общий топик, и в персональный
        messagingTemplate.convertAndSend("/topic/ads", payload);
        messagingTemplate.convertAndSend("/topic/ad/" + ad.getId() + "/status", payload);
        log.info("AD_UPDATED (status) sent for ad {} to /topic/ads and /topic/ad/{}/status", ad.getId(), ad.getId());
    }

    public void sendResponseApproved(Ad ad, Response response) {
        ResponseResponse dto = responseMapper.toResponse(response);
        Map<String, Object> payload = Map.of(
                "type", "RESPONSE_APPROVED",
                "entityId", ad.getId().toString(),
                "payload", dto
        );
        messagingTemplate.convertAndSend("/topic/ad/" + ad.getId() + "/responses", payload);
        log.info("RESPONSE_APPROVED sent for response {} to ad {}", response.getId(), ad.getId());
    }

    public void sendResponseRejected(Ad ad, Response response) {
        ResponseResponse dto = responseMapper.toResponse(response);
        Map<String, Object> payload = Map.of(
                "type", "RESPONSE_REJECTED",
                "entityId", ad.getId().toString(),
                "payload", dto
        );
        messagingTemplate.convertAndSend("/topic/ad/" + ad.getId() + "/responses", payload);
        log.info("RESPONSE_REJECTED sent for response {} to ad {}", response.getId(), ad.getId());
    }

    public void sendResponseAdded(Ad ad, Response response) {
        ResponseResponse dto = responseMapper.toResponse(response);
        Map<String, Object> payload = Map.of(
                "type", "RESPONSE_ADDED",
                "entityId", ad.getId().toString(),   // теперь ID объявления
                "payload", dto
        );
        messagingTemplate.convertAndSend("/topic/ad/" + ad.getId() + "/responses", payload);
        log.info("RESPONSE_ADDED sent for response {} to ad {}", response.getId(), ad.getId());
    }

    public void sendApprovalCancelled(Ad ad, Response response) {
        ResponseResponse dto = responseMapper.toResponse(response);
        Map<String, Object> payload = Map.of(
                "type", "APPROVAL_CANCELLED",
                "entityId", ad.getId().toString(),
                "payload", dto
        );
        messagingTemplate.convertAndSend("/topic/ad/" + ad.getId() + "/responses", payload);
        log.info("APPROVAL_CANCELLED sent for response {} to ad {}", response.getId(), ad.getId());
    }

    // Перегрузка для Response (используется при удалении, когда у нас есть сущность Response)
    public void sendResponseRemoved(Ad ad, Response response) {
        Map<String, Object> payload = Map.of(
                "type", "RESPONSE_REMOVED",
                "entityId", ad.getId().toString(),
                "payload", responseMapper.toResponse(response)
        );
        messagingTemplate.convertAndSend("/topic/ad/" + ad.getId() + "/responses", payload);
        log.info("RESPONSE_REMOVED sent for response {}", response.getId());
    }

    // Перегрузка для ResponseResponse (используется, когда сущность уже удалена, но есть DTO)
    public void sendResponseRemoved(Ad ad, ResponseResponse responseDto) {
        Map<String, Object> payload = Map.of(
                "type", "RESPONSE_REMOVED",
                "entityId", ad.getId().toString(),
                "payload", responseDto
        );
        messagingTemplate.convertAndSend("/topic/ad/" + ad.getId() + "/responses", payload);
        log.info("RESPONSE_REMOVED sent for response {}", responseDto.getId());
    }

    public void sendAdCreated(Ad ad) {
        AdResponse adResponse = adMapper.toResponse(ad);
        log.info("AD_CREATED: adId={}, rinkIds в ответе = {}", ad.getId(), adResponse.getRinkIds());
        Map<String, Object> payload = Map.of(
                "type", "AD_CREATED",
                "entityId", ad.getId().toString(),
                "payload", adResponse
        );
        String destination = "/topic/ads";
        log.info("🔔 Отправка AD_CREATED в топик {}: {}", destination, payload);
        messagingTemplate.convertAndSend(destination, payload);
        log.info("✅ Сообщение AD_CREATED отправлено");
    }

    public void sendAdDeleted(Ad ad) {
        Map<String, Object> payload = Map.of(
                "type", "AD_DELETED",
                "entityId", ad.getId().toString()
        );
        // Отправляем в оба топика
        messagingTemplate.convertAndSend("/topic/ads", payload);
        messagingTemplate.convertAndSend("/topic/ad/" + ad.getId() + "/status", payload);
        log.info("AD_DELETED sent for ad {} to /topic/ads and /topic/ad/{}/status", ad.getId(), ad.getId());
    }

    // Новая перегрузка для использования после удаления (с UUID)
    public void sendAdDeleted(UUID adId) {
        Map<String, Object> payload = Map.of(
                "type", "AD_DELETED",
                "entityId", adId.toString()
        );
        messagingTemplate.convertAndSend("/topic/ads", payload);
        messagingTemplate.convertAndSend("/topic/ad/" + adId + "/status", payload);
        log.info("AD_DELETED sent for ad {} to /topic/ads and /topic/ad/{}/status", adId, adId);
    }

    public void sendAdUpdated(Ad ad) {
        AdResponse adResponse = adMapper.toResponse(ad);
        Map<String, Object> payload = Map.of(
                "type", "AD_UPDATED",
                "entityId", ad.getId().toString(),
                "payload", adResponse
        );
        // Отправляем и в общий топик, и в персональный
        messagingTemplate.convertAndSend("/topic/ads", payload);
        messagingTemplate.convertAndSend("/topic/ad/" + ad.getId() + "/status", payload);
        log.info("AD_UPDATED sent for ad {} to /topic/ads and /topic/ad/{}/status", ad.getId(), ad.getId());
    }

    private String getAdDescription(Integer type, Integer subType) {
        if (type == null) {
            return "Новое объявление";
        }
        return switch (type) {
            case 1 -> switch (subType != null ? subType : 0) {
                case 1 -> "Нужен вратарь";
                case 2 -> "Нужен полевой";
                default -> "Новое объявление";
            };
            case 2 -> switch (subType != null ? subType : 0) {
                case 1 -> "Ищу лёд (вратарь)";
                case 2 -> "Ищу лёд (полевой)";
                default -> "Новое объявление";
            };
            case 3 -> switch (subType != null ? subType : 0) {
                case 1 -> "Ищу товарищеский матч";
                case 2 -> "Предлагаю товарищеский матч";
                default -> "Новое объявление";
            };
            case 4 -> switch (subType != null ? subType : 0) {
                case 1 -> "Нужен судья";
                case 2 -> "Нужен фотограф";
                case 3 -> "Нужен медик";
                case 4 -> "Нужен тренер";
                default -> "Новое объявление";
            };
            default -> "Новое объявление";
        };
    }

    private String getAdDetails(Ad ad) {
        String typeDescription = getAdDescription(ad.getType(), ad.getSubType());

        String rinkName = "";
        if (ad.getAdRinks() != null && !ad.getAdRinks().isEmpty()) {
            rinkName = ad.getAdRinks().iterator().next().getRink().getName();
        }

        LocalDateTime startTime = ad.getStartTime();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String date = startTime.format(dateFormatter);
        String time = startTime.format(timeFormatter);

        return String.format("«%s», %s, %s, %s", typeDescription, rinkName, date, time);
    }

    @Transactional
    public void setupDefaultNotifications(UUID userId) {
        // 1. Создаём или обновляем настройки – включаем уведомления о новых объявлениях
        NotificationSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        // Включаем уведомления о новых объявлениях в городе
        settings.setNotifyNewAdsInCity(true);
        settings.setUpdatedAt(LocalDateTime.now());
        settingsRepository.save(settings);

        log.debug("Включены уведомления о новых объявлениях для пользователя {}", userId);

        // 2. Добавляем подписки на все типы объявлений
        addDefaultSubscriptions(userId);
    }

    public void addDefaultSubscriptions(UUID userId) {
        List<int[]> pairs = Arrays.asList(
                new int[]{1, 1}, new int[]{1, 2},
                new int[]{2, 1}, new int[]{2, 2},
                new int[]{3, 1}, new int[]{3, 2},
                new int[]{4, 1}, new int[]{4, 2}, new int[]{4, 3}, new int[]{4, 4}
        );

        for (int[] pair : pairs) {
            try {
                // Используем существующий метод, но он выбрасывает исключение, если подписка уже есть
                AddSubscriptionRequest request = new AddSubscriptionRequest();
                request.setType(pair[0]);
                request.setSubType(pair[1]);

                // Проверяем, существует ли уже подписка
                UserNotificationSubscription.SubscriptionId id =
                        new UserNotificationSubscription.SubscriptionId(userId, pair[0], pair[1]);
                if (!subscriptionRepository.existsById(id)) {
                    User user = userService.getUserById(userId);
                    UserNotificationSubscription subscription = new UserNotificationSubscription();
                    subscription.setId(id);
                    subscription.setUser(user);
                    subscriptionRepository.save(subscription);
                }
            } catch (Exception e) {
                log.warn("Не удалось добавить подписку по умолчанию для пользователя {}: type={}, subType={}",
                        userId, pair[0], pair[1], e);
            }
        }

        log.info("Добавлены подписки по умолчанию для пользователя {}", userId);
    }
    /**
     * Получает количество активных push-токенов пользователя по платформам
     */
    public Map<String, Integer> getPushTokensCount(UUID userId) {
        Map<String, Integer> result = new HashMap<>();
        result.put("FCM", fcmTokenService.getActiveTokensForUser(userId).size());
        result.put("HMS", hmsTokenService.getActiveTokensForUser(userId).size());
        result.put("RuStore", ruStoreTokenService.getActiveTokensForUser(userId).size());
        return result;
    }

    /**
     * Отправляет тестовое push-уведомление пользователю (для отладки)
     */
    public void sendTestPush(UUID userId) {
        String title = "🔔 Тестовое уведомление";
        String body = "Если вы это видите, push-уведомления работают!";

        for (PushService pushService : pushServices) {
            try {
                pushService.sendNotification(userId, title, body);
                log.info("✅ Test push sent via {} to user {}",
                        pushService.getClass().getSimpleName(), userId);
            } catch (Exception e) {
                log.error("❌ Failed to send test push via {}: {}",
                        pushService.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Повторная отправка уведомления (используется при ошибках)
     */
    @Transactional
    public void resendNotification(Long notificationId) {
        // Находим уведомление в БД
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Уведомление не найдено"));

        UUID userId = notification.getUser().getId();
        String type = notification.getType();
        String content = notification.getContent();
        UUID relatedEntityId = notification.getRelatedEntityId();

        // Проверяем настройки пользователя
        NotificationSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        // Проверяем, нужно ли отправлять push
        boolean shouldSendPush = shouldSendPushForType(type, settings);
        if (!shouldSendPush) {
            log.info("ℹ️ Уведомление {} не требует push-отправки", notificationId);
            return;
        }

        // Проверяем наличие активной WebSocket-сессии
        SimpUser simpUser = userRegistry.getUser(userId.toString());
        boolean hasSession = (simpUser != null && simpUser.hasSessions());

        // Если сессии нет - отправляем push
        if (!hasSession) {
            boolean hasAnyToken = hasAnyPushToken(userId);
            if (hasAnyToken) {
                sendPushNotification(userId, type, content, relatedEntityId, notificationId);
                log.info("✅ Push повторно отправлен для уведомления {}", notificationId);
            } else {
                log.info("ℹ️ У пользователя {} нет push-токенов для повторной отправки", userId);
            }
        } else {
            // Если сессия есть - отправляем через WebSocket
            NotificationResponse response = toResponse(notification);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    response
            );
            log.info("✅ WebSocket уведомление повторно отправлено пользователю {}", userId);
        }
    }

    /**
     * Проверка, нужно ли отправлять push для данного типа
     */
    private boolean shouldSendPushForType(String type, NotificationSettings settings) {
        switch (type) {
            case "RESPONSE":
                return settings.isNotifyOnResponseToMyAd();
            case "RESPONSE_ACCEPTED":
                return settings.isNotifyOnMyResponseAccepted();
            case "NEW_AD":
                return settings.isNotifyNewAdsInCity();
            default:
                return false;
        }
    }

    /**
     * Проверка наличия любого push-токена у пользователя
     */
    private boolean hasAnyPushToken(UUID userId) {
        for (PushService pushService : pushServices) {
            if (pushService instanceof FcmPushService) {
                if (!fcmTokenService.getActiveTokensForUser(userId).isEmpty()) {
                    return true;
                }
            } else if (pushService instanceof HuaweiPushService) {
                if (!hmsTokenService.getActiveTokensForUser(userId).isEmpty()) {
                    return true;
                }
            } else if (pushService instanceof RuStorePushService) {
                if (!ruStoreTokenService.getActiveTokensForUser(userId).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Отправка push-уведомления через все доступные сервисы
     */
    private void sendPushNotification(UUID userId, String type, String content, UUID relatedEntityId, Long notificationId) {
        String title = getNotificationTitle(type);
        String body = content;

        // Если title не определен, используем дефолтный
        if (title == null) {
            title = "Новое уведомление";
        }

        for (PushService pushService : pushServices) {
            try {
                pushService.sendNotification(userId, title, body);
                log.info("📤 Push отправлен через {} для пользователя {}",
                        pushService.getClass().getSimpleName(), userId);
            } catch (Exception e) {
                log.error("❌ Ошибка отправки push через {}: {}",
                        pushService.getClass().getSimpleName(), e.getMessage());

                // Запланировать повторную отправку (используем notificationId)
                if (notificationId != null) {
                    retryScheduler.scheduleRetry(notificationId);
                }
            }
        }
    }

    /**
     * Получение заголовка для уведомления по типу
     */
    private String getNotificationTitle(String type) {
        if (type == null) return null;
        switch (type) {
            case "RESPONSE":
                return "Новый отклик";
            case "RESPONSE_ACCEPTED":
                return "Отклик принят";
            case "NEW_AD":
                return "Новое объявление";
            case "RESPONSE_REJECTED":
                return "Отклик отклонен";
            case "RESPONSE_WITHDRAWN":
                return "Отклик отозван";
            case "RESPONSE_ACCEPTANCE_CANCELLED":
                return "Принятие отклика отменено";
            default:
                return "Новое уведомление";
        }
    }

    
}