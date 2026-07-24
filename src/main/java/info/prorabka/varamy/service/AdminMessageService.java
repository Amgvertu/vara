package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.request.AdminMessageRequest;
import info.prorabka.varamy.dto.response.AdminMessageResponse;
import info.prorabka.varamy.dto.response.UserMessageResponse;
import info.prorabka.varamy.entity.*;
import info.prorabka.varamy.exception.BadRequestException;
import info.prorabka.varamy.exception.ResourceNotFoundException;
import info.prorabka.varamy.repository.AdminMessageRepository;
import info.prorabka.varamy.repository.UserMessageRepository;
import info.prorabka.varamy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMessageService {

    private final AdminMessageRepository adminMessageRepository;
    private final UserMessageRepository userMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final List<PushService> pushServices;
    private final FileStorageService fileStorageService; // для удаления файлов (опционально)

    /**
     * Загрузка изображения для сообщения.
     */
    public String uploadImage(org.springframework.web.multipart.MultipartFile file) {
        // Проверка формата (можно добавить)
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/jpeg") ||
                contentType.startsWith("image/png") || contentType.startsWith("image/webp"))) {
            throw new BadRequestException("Недопустимый формат файла. Разрешены JPEG, PNG, WebP.");
        }
        // Сохраняем в папку "messages"
        return fileStorageService.storeFile(file, "messages");
    }

    /**
     * Отправка сообщения.
     */
    @Transactional
    public AdminMessageResponse sendMessage(AdminMessageRequest request, UUID senderId) {
        // 1. Валидация
        validateRequest(request);

        // 2. Определить получателей
        List<User> recipients = resolveRecipients(request.getDelivery());

        if (recipients.isEmpty()) {
            throw new BadRequestException("Не найдено получателей для указанных критериев");
        }

        // 3. Сохранить AdminMessage
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Отправитель не найден"));

        AdminMessage message = new AdminMessage();
        message.setSender(sender);
        message.setTitle(request.getTitle());
        message.setContent(request.getContent());
        message.setImageUrl(request.getImageUrl());
        message.setLink(request.getLink());
        message.setCategory(request.getCategory());
        message.setDeliveryCriteria(request.getDelivery());
        message.setDeliveryType(request.getCategory());
        message = adminMessageRepository.save(message);

        // 4. Создать записи для получателей
        List<UserMessage> userMessages = new ArrayList<>();
        for (User user : recipients) {
            UserMessage um = new UserMessage();
            um.setUser(user);
            um.setAdminMessage(message);
            um.setRead(false);
            um.setReadAt(null);
            userMessages.add(um);
        }
        userMessageRepository.saveAll(userMessages);

        // 5. Отправить WebSocket в личные очереди
        for (UserMessage um : userMessages) {
            UserMessageResponse dto = buildUserMessageResponse(um);
            messagingTemplate.convertAndSendToUser(
                    um.getUser().getId().toString(),
                    "/queue/admin-messages",
                    dto
            );
        }

        // 6. Отправить PUSH, если категория PUSH
        if ("PUSH".equalsIgnoreCase(request.getCategory())) {
            String title = request.getTitle() != null ? request.getTitle() : "Новое сообщение";
            String body = request.getContent() != null ? request.getContent() : "";
            for (User user : recipients) {
                for (PushService pushService : pushServices) {
                    try {
                        pushService.sendNotification(user.getId(), title, body);
                    } catch (Exception e) {
                        log.error("Ошибка отправки push через {} пользователю {}: {}",
                                pushService.getClass().getSimpleName(), user.getId(), e.getMessage());
                    }
                }
            }
        }

        // 7. Ответ
        return AdminMessageResponse.builder()
                .id(message.getId())
                .title(message.getTitle())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .link(message.getLink())
                .category(message.getCategory())
                .deliveryCriteria(message.getDeliveryCriteria())
                .createdAt(message.getCreatedAt())
                .recipientCount(recipients.size())
                .build();
    }

    /**
     * Получение истории отправленных сообщений для админа.
     */
    public Page<AdminMessageResponse> getSentMessages(UUID senderId, Pageable pageable) {
        return adminMessageRepository.findBySenderIdOrderByCreatedAtDesc(senderId, pageable)
                .map(msg -> AdminMessageResponse.builder()
                        .id(msg.getId())
                        .title(msg.getTitle())
                        .content(msg.getContent())
                        .imageUrl(msg.getImageUrl())
                        .link(msg.getLink())
                        .category(msg.getCategory())
                        .deliveryCriteria(msg.getDeliveryCriteria())
                        .createdAt(msg.getCreatedAt())
                        .recipientCount(0) // можно добавить запрос на количество получателей
                        .build());
    }

    // ---------- Вспомогательные методы ----------

    private void validateRequest(AdminMessageRequest request) {
        boolean isText = request.getContent() != null && !request.getContent().isEmpty();
        boolean isImage = request.getImageUrl() != null && !request.getImageUrl().isEmpty();

        if (!isText && !isImage) {
            throw new BadRequestException("Необходимо указать либо текст сообщения (content), либо изображение (imageUrl)");
        }

        if (request.getDelivery() == null) {
            throw new BadRequestException("Критерии доставки не заданы");
        }

        DeliveryCriteria criteria = request.getDelivery();
        boolean hasCriteria = criteria.isAllUsers() ||
                criteria.isAdmins() ||
                criteria.isModerators() ||
                (criteria.getUserIds() != null && !criteria.getUserIds().isEmpty()) ||
                (!criteria.isAllCities() && criteria.getCityIds() != null && !criteria.getCityIds().isEmpty()) ||
                (!criteria.isAllTeams() && criteria.getTeamNames() != null && !criteria.getTeamNames().isEmpty());

        if (!hasCriteria) {
            throw new BadRequestException("Не выбран ни один критерий доставки");
        }
    }

    private List<User> resolveRecipients(DeliveryCriteria criteria) {
        // 1. Приоритет: индивидуальные пользователи
        if (criteria.getUserIds() != null && !criteria.getUserIds().isEmpty()) {
            return userRepository.findAllById(criteria.getUserIds()).stream()
                    .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                    .collect(Collectors.toList());
        }

        // 2. Загружаем всех активных пользователей
        List<User> allActive = userRepository.findAllByStatus(User.UserStatus.ACTIVE);

        // 3. Базовое множество с учётом allUsers
        List<User> baseUsers;
        if (criteria.isAllUsers()) {
            // только пользователи с ролью USER (не админы и не модераторы)
            baseUsers = allActive.stream()
                    .filter(u -> u.getRole() == User.UserRole.USER)
                    .collect(Collectors.toList());
        } else {
            // все активные
            baseUsers = new ArrayList<>(allActive);
        }

        // 4. Фильтр по городам (если не allCities)
        if (!criteria.isAllCities() && criteria.getCityIds() != null && !criteria.getCityIds().isEmpty()) {
            List<Long> cityIds = criteria.getCityIds();
            baseUsers = baseUsers.stream()
                    .filter(u -> u.getProfile() != null && u.getProfile().getHomeCity() != null &&
                            cityIds.contains(u.getProfile().getHomeCity().getId()))
                    .collect(Collectors.toList());
        }

        // 5. Фильтр по командам (если не allTeams)
        if (!criteria.isAllTeams() && criteria.getTeamNames() != null && !criteria.getTeamNames().isEmpty()) {
            List<String> teamNames = criteria.getTeamNames().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
            baseUsers = baseUsers.stream()
                    .filter(u -> u.getProfile() != null && u.getProfile().getTeam() != null &&
                            teamNames.contains(u.getProfile().getTeam().toLowerCase()))
                    .collect(Collectors.toList());
        }

        // 6. Добавляем админов и модераторов, если флаги установлены (поверх всех фильтров)
        List<User> result = new ArrayList<>(baseUsers);
        if (criteria.isAdmins()) {
            result.addAll(allActive.stream()
                    .filter(u -> u.getRole() == User.UserRole.ADMIN)
                    .collect(Collectors.toList()));
        }
        if (criteria.isModerators()) {
            result.addAll(allActive.stream()
                    .filter(u -> u.getRole() == User.UserRole.MODERATOR)
                    .collect(Collectors.toList()));
        }

        // 7. Убираем дубликаты
        return result.stream().distinct().collect(Collectors.toList());
    }

    private UserMessageResponse buildUserMessageResponse(UserMessage um) {
        AdminMessage msg = um.getAdminMessage();
        return UserMessageResponse.builder()
                .id(um.getId())
                .title(msg.getTitle())
                .content(msg.getContent())
                .imageUrl(msg.getImageUrl())
                .link(msg.getLink())
                .category(msg.getCategory())
                .createdAt(msg.getCreatedAt())
                .isRead(um.isRead())
                .build();
    }
}