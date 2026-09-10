package info.prorabka.varamy.service;

import java.util.UUID;

public interface PushService {

    /**
     * @return true, если уведомление успешно отправлено хотя бы на одно устройство.
     */
    boolean sendWakeUpNotification(UUID userId);

    /**
     * @return true, если уведомление успешно отправлено хотя бы на одно устройство.
     */
    boolean sendNotification(UUID userId, String title, String body);
}