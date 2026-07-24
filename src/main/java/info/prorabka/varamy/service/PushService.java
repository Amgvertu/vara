package info.prorabka.varamy.service;

import java.util.UUID;

public interface PushService {
    void sendWakeUpNotification(UUID userId);
    void sendNotification(UUID userId, String title, String body);
}