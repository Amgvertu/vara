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
import java.util.Map;
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

            log.info("🔵 STOMP CONNECT received");

            String token = null;

            // 1. Пробуем получить токен из заголовка Authorization (новый способ)
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                log.info("🔑 Token extracted from Authorization header");
            }

            // 2. Если не нашли – пробуем старый заголовок "token"
            if (token == null) {
                List<String> tokens = accessor.getNativeHeader("token");
                if (tokens != null && !tokens.isEmpty()) {
                    token = tokens.get(0);
                    log.info("🔑 Token extracted from 'token' header");
                }
            }

            // 3. Если всё ещё null — пробуем получить из параметров подключения (для SockJS)
            if (token == null) {
                // Получаем параметры запроса из заголовка
                String queryString = accessor.getFirstNativeHeader("query-string");
                if (queryString != null && queryString.contains("token=")) {
                    String[] params = queryString.split("&");
                    for (String param : params) {
                        if (param.startsWith("token=")) {
                            token = param.substring(6);
                            log.info("🔑 Token extracted from query string");
                            break;
                        }
                    }
                }

                // Проверяем также заголовок Authorization через nativeMessage
                if (token == null) {
                    // Используем то, что уже есть в accessor
                    // Попробуем получить из параметров соединения
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes != null && sessionAttributes.containsKey("token")) {
                        token = (String) sessionAttributes.get("token");
                        log.info("🔑 Token extracted from session attributes");
                    }
                }
            }

            log.info("🔑 Token present: {}", token != null ? "YES (length=" + token.length() + ")" : "NO");

            // 4. Валидируем токен и проверяем статус пользователя
            if (token != null && jwtService.validateToken(token)) {
                try {
                    String userId = jwtService.getUserIdFromToken(token);
                    User user = userService.getUserById(UUID.fromString(userId));
                    if (user != null && user.getStatus() == User.UserStatus.ACTIVE) {
                        Principal principal = () -> userId;
                        accessor.setUser(principal);
                        log.info("✅ STOMP CONNECT authenticated user: {}", userId);
                        return message;
                    } else {
                        log.warn("❌ STOMP CONNECT rejected: user {} is not active", userId);
                    }
                } catch (Exception e) {
                    log.error("❌ Error during STOMP authentication", e);
                }
            } else {
                if (token == null) {
                    log.warn("❌ STOMP CONNECT rejected: no token provided");
                } else {
                    log.warn("❌ STOMP CONNECT rejected: invalid token");
                }
            }
            return null; // отклоняем соединение
        }
        return message;
    }
}