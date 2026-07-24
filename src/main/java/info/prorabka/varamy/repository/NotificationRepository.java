package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    long countByUserIdAndIsReadFalse(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.id IN :ids")
    int markAsRead(@Param("userId") UUID userId, @Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.relatedEntityId = :entityId")
    void deleteByRelatedEntityId(@Param("entityId") UUID entityId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
