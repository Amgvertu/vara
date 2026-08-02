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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private static final long FCM_COOLDOWN_MS = 5 * 60 * 1000; // 5 минут (настройте под свои задачи)
    private final ResponseRepository responseRepository;
    private final ResponseMapper responseMapper;
    private final AdMapper adMapper;
    private final AdRepository adRepository;

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

        // 1. Сохраняем уведомление в БД (ВСЕГДА, независимо от наличия сессии)
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

        // 2. Проверяем наличие активной WebSocket-сессии
        SimpUser simpUser = userRegistry.getUser(userId.toString());
        boolean hasSession = (simpUser != null && simpUser.hasSessions());

        if (hasSession) {
            // Сессия есть – отправляем уведомление через WebSocket немедленно
            NotificationResponse response = toResponse(notification);
            try {
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/notifications",
                        response
                );
                log.info("WebSocket notification successfully sent to user {}", userId);
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification to user {}: {}", userId, e.getMessage(), e);
            }
        } else {
            // Сессии нет – отправляем FCM "пробуждение" (с защитой от частых отправок)
            log.info("No active WebSocket session for user {}, sending FCM wake-up", userId);

            if (shouldSendFcm(userId)) {
                for (PushService pushService : pushServices) {
                    try {
                        pushService.sendWakeUpNotification(userId);
                    } catch (Exception e) {
                        log.error("Ошибка отправки wake-up через {}: {}", pushService.getClass().getSimpleName(), e.getMessage());
                    }
                }
                lastFcmSentTime.put(userId, System.currentTimeMillis());
                log.info("FCM wake-up sent to user {}", userId);
            } else {
                log.debug("FCM wake-up already sent recently for user {}, skipping", userId);
            }
            // Уведомление сохранено в БД, клиент заберёт его через REST после пробуждения
        }
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
}