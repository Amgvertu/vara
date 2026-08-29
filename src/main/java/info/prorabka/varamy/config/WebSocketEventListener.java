package info.prorabka.varamy.config;

import info.prorabka.varamy.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final UserActivityService userActivityService;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            userActivityService.setActive(UUID.fromString(user.getName()), true);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            userActivityService.setActive(UUID.fromString(user.getName()), false);
        }
    }
}
