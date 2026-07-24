package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.UserNotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserNotificationSubscriptionRepository extends JpaRepository<UserNotificationSubscription, UserNotificationSubscription.SubscriptionId> {
    List<UserNotificationSubscription> findByIdUserId(UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserNotificationSubscription s WHERE s.id.userId = :userId AND s.id.type = :type AND s.id.subType = :subType")
    void deleteByUserIdAndTypeAndSubType(@Param("userId") UUID userId, @Param("type") Integer type, @Param("subType") Integer subType);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserNotificationSubscription s WHERE s.id.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

}