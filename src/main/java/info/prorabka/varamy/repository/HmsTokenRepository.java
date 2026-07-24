package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.HmsToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface HmsTokenRepository extends JpaRepository<HmsToken, Long> {
    List<HmsToken> findByUserIdAndIsActiveTrue(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE HmsToken t SET t.isActive = false WHERE t.user.id = :userId AND t.token = :token")
    void deactivateToken(@Param("userId") UUID userId, @Param("token") String token);

    @Modifying
    @Query("DELETE FROM HmsToken t WHERE t.user.id = :userId")
    void deleteByUserId(UUID userId);
}