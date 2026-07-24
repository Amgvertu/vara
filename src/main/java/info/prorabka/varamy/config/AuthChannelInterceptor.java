package info.prorabka.varamy.config;

import info.prorabka.varamy.entity.User;
import info.prorabka.varamy.service.JwtService;
import info.prorabka.varamy.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String token = null;

            // 1. Пробуем получить токен из заголовка Authorization (новый способ)
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            // 2. Если не нашли – пробуем старый заголовок "token"
            if (token == null) {
                List<String> tokens = accessor.getNativeHeader("token");
                if (tokens != null && !tokens.isEmpty()) {
                    token = tokens.get(0);
                }
            }

            // 3. Валидируем токен и проверяем статус пользователя
            if (token != null && jwtService.validateToken(token)) {
                try {
                    String userId = jwtService.getUserIdFromToken(token);
                    User user = userService.getUserById(UUID.fromString(userId));
                    if (user != null && user.getStatus() == User.UserStatus.ACTIVE) {
                        Principal principal = () -> userId;
                        accessor.setUser(principal);
                        log.info("STOMP CONNECT authenticated user: {}", userId);
                        return message;
                    } else {
                        log.warn("STOMP CONNECT rejected: user {} is not active", userId);
                    }
                } catch (Exception e) {
                    log.error("Error during STOMP authentication", e);
                }
            }
            log.warn("STOMP CONNECT rejected: invalid or missing token");
            return null; // отклоняем соединение
        }
        return message;
    }
}