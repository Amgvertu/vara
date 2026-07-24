package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.UserStat;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserStatRepository extends JpaRepository<UserStat, Long>, JpaSpecificationExecutor<UserStat> {
    // Все методы удалены, теперь используем спецификации

    @Modifying
    @Transactional
    @Query("DELETE FROM UserStat s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}