package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.UserMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface UserMessageRepository extends JpaRepository<UserMessage, Long> {

    Page<UserMessage> findByUserIdOrderByAdminMessageCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT COUNT(um) FROM UserMessage um WHERE um.user.id = :userId AND um.isRead = false")
    long countUnreadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE UserMessage um SET um.isRead = true, um.readAt = CURRENT_TIMESTAMP WHERE um.user.id = :userId AND um.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE UserMessage um SET um.isRead = true, um.readAt = CURRENT_TIMESTAMP WHERE um.id = :id AND um.user.id = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") UUID userId);

    boolean existsByIdAndUserId(Long id, UUID userId);

    @Modifying
    @Query("DELETE FROM UserMessage um WHERE um.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserMessage um WHERE um.adminMessage.sender.id = :senderId")
    void deleteByAdminMessageSenderId(@Param("senderId") UUID senderId);

}