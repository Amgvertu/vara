package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.FeedbackMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface FeedbackMessageRepository extends JpaRepository<FeedbackMessage, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM FeedbackMessage f WHERE f.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
