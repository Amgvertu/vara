package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.response.UserMessageResponse;
import info.prorabka.varamy.entity.UserMessage;
import info.prorabka.varamy.exception.BadRequestException;
import info.prorabka.varamy.exception.ResourceNotFoundException;
import info.prorabka.varamy.exception.UnauthorizedException;
import info.prorabka.varamy.repository.UserMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMessageService {

    private final UserMessageRepository userMessageRepository;

    @Transactional(readOnly = true)
    public Page<UserMessageResponse> getMessagesForUser(UUID userId, Pageable pageable) {
        Page<UserMessage> page = userMessageRepository.findByUserIdOrderByAdminMessageCreatedAtDesc(userId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public void markAsRead(UUID userId, Long messageId) {
        // Проверяем, что запись принадлежит пользователю
        if (!userMessageRepository.existsByIdAndUserId(messageId, userId)) {
            throw new ResourceNotFoundException("Сообщение не найдено или не принадлежит вам");
        }
        int updated = userMessageRepository.markAsRead(messageId, userId);
        if (updated == 0) {
            throw new BadRequestException("Сообщение уже прочитано или не существует");
        }
        log.debug("Сообщение {} отмечено прочитанным для пользователя {}", messageId, userId);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        int updated = userMessageRepository.markAllAsRead(userId);
        log.debug("Отмечены все сообщения как прочитанные для пользователя {} ({} шт.)", userId, updated);
        return updated;
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return userMessageRepository.countUnreadByUserId(userId);
    }

    private UserMessageResponse toResponse(UserMessage um) {
        return UserMessageResponse.builder()
                .id(um.getId())
                .title(um.getAdminMessage().getTitle())
                .content(um.getAdminMessage().getContent())
                .imageUrl(um.getAdminMessage().getImageUrl())
                .link(um.getAdminMessage().getLink())
                .category(um.getAdminMessage().getCategory())
                .createdAt(um.getAdminMessage().getCreatedAt())
                .isRead(um.isRead())
                .build();
    }

    @Transactional
    public void deleteMessage(Long messageId, UUID userId) {
        UserMessage message = userMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Сообщение не найдено"));
        if (!message.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Вы не можете удалить это сообщение");
        }
        userMessageRepository.delete(message);
    }

    @Transactional
    public void deleteAllMessages(UUID userId) {
        userMessageRepository.deleteByUserId(userId);
    }
}