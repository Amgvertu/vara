package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.dto.response.UserMessageResponse;
import info.prorabka.varamy.security.SecurityUser;
import info.prorabka.varamy.service.UserMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/messages")
@RequiredArgsConstructor
@Tag(name = "Сообщения для пользователей", description = "Получение и управление административными сообщениями")
public class UserMessageController {

    private final UserMessageService userMessageService;

    @GetMapping
    @Operation(summary = "Получить список сообщений для текущего пользователя")
    public ResponseEntity<ApiResponse<Page<UserMessageResponse>>> getMessages(
            @AuthenticationPrincipal SecurityUser currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserMessageResponse> page = userMessageService.getMessagesForUser(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Отметить одно сообщение как прочитанное")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal SecurityUser currentUser,
            @PathVariable Long id) {
        userMessageService.markAsRead(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Отмечено как прочитанное", null));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Отметить все сообщения как прочитанные")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal SecurityUser currentUser) {
        int count = userMessageService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Все сообщения отмечены прочитанными (" + count + ")", null));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Получить количество непрочитанных сообщений")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal SecurityUser currentUser) {
        long count = userMessageService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить личное сообщение (пользователь)")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @AuthenticationPrincipal SecurityUser currentUser,
            @PathVariable Long id) {
        userMessageService.deleteMessage(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Сообщение удалено", null));
    }

    @DeleteMapping
    @Operation(summary = "Удалить все личные сообщения (пользователь)")
    public ResponseEntity<ApiResponse<Void>> deleteAllMessages(
            @AuthenticationPrincipal SecurityUser currentUser) {
        userMessageService.deleteAllMessages(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Все сообщения удалены", null));
    }
}
