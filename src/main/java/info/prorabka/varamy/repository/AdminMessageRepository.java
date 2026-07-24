package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.AdminMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface AdminMessageRepository extends JpaRepository<AdminMessage, Long> {
    Page<AdminMessage> findBySenderIdOrderByCreatedAtDesc(UUID senderId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM AdminMessage m WHERE m.sender.id = :senderId")
    void deleteBySenderId(@Param("senderId") UUID senderId);
}

