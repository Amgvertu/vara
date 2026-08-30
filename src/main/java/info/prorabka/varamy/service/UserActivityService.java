package info.prorabka.varamy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserActivityService {
    private final Map<UUID, Boolean> activeUsers = new ConcurrentHashMap<>();

    public void setActive(UUID userId, boolean active) {
        log.info("🔵 setActive: userId={}, active={}", userId, active);
        if (active) {
            activeUsers.put(userId, true);
        } else {
            activeUsers.remove(userId);
        }
        log.info("🔵 После setActive: активных пользователей: {}, для userId={} статус = {}",
                activeUsers.size(), userId, activeUsers.getOrDefault(userId, false));
    }

    public boolean isActive(UUID userId) {
        boolean active = activeUsers.getOrDefault(userId, false);
        log.debug("🔍 isActive: userId={}, active={}", userId, active);
        return active;
    }
}