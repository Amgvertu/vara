package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, UUID> {
    Optional<NotificationSettings> findByUserId(UUID userId);
}
