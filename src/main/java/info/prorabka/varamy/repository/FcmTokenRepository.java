package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    Optional<FcmToken> findByToken(String token);
    List<FcmToken> findByUserIdAndIsActiveTrue(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE FcmToken t SET t.isActive = false WHERE t.user.id = :userId AND t.token = :token")
    void deactivateToken(@Param("userId") UUID userId, @Param("token") String token);

    @Modifying
    @Query("DELETE FROM FcmToken t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}