package info.prorabka.varamy.controller;

import info.prorabka.varamy.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserStatusStompController {

    private final UserActivityService userActivityService;

    @MessageMapping("/user/status")
    public void updateStatus(@Payload Map<String, Boolean> payload,
                             SimpMessageHeaderAccessor headerAccessor) {
        // Получаем пользователя из заголовков (а не из @AuthenticationPrincipal)
        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            log.warn("Не удалось определить пользователя в STOMP-сообщении статуса");
            return;
        }
        UUID userId = UUID.fromString(principal.getName());
        Boolean active = payload.get("active");
        if (active != null) {
            userActivityService.setActive(userId, active);
            log.info("📨 STOMP статус обновлён: userId={}, active={}", userId, active);
        }
    }
}