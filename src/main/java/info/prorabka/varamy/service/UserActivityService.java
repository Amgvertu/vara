package info.prorabka.varamy.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserActivityService {
    private final Map<UUID, Boolean> activeUsers = new ConcurrentHashMap<>();

    public void setActive(UUID userId, boolean active) {
        if (active) {
            activeUsers.put(userId, true);
        } else {
            activeUsers.remove(userId);
        }
    }

    public boolean isActive(UUID userId) {
        return activeUsers.getOrDefault(userId, false);
    }
}
