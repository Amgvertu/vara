package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.City;
import info.prorabka.varamy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE " +
            "(:phone IS NULL OR u.phone LIKE %:phone%) AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:status IS NULL OR u.status = :status)")
    Page<User> findWithFilters(
            @Param("phone") String phone,
            @Param("role") User.UserRole role,
            @Param("status") User.UserStatus status,
            Pageable pageable);

    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN u.profile p " +
            "LEFT JOIN NotificationSettings ns ON ns.userId = u.id " +
            "LEFT JOIN UserNotificationSubscription subs ON subs.id.userId = u.id " +
            "WHERE u.status = 'ACTIVE' " +
            "AND ns.notifyNewAdsInCity = true " +
            "AND (ns.notificationCity.id = :cityId OR (ns.notificationCity IS NULL AND p.homeCity.id = :cityId)) " +
            "AND subs.id.type = :type AND subs.id.subType = :subType")
    List<User> findUsersForNewAdNotification(@Param("cityId") Long cityId,
                                             @Param("type") Integer type,
                                             @Param("subType") Integer subType);


    @Query("SELECT u FROM User u WHERE u.status = :status AND u.profile.homeCity.id = :cityId")
    List<User> findAllByStatusAndProfileHomeCityId(@Param("status") User.UserStatus status,
                                                   @Param("cityId") Long cityId);

    @Query("SELECT u FROM User u WHERE u.status = :status AND u.profile.position = :position")
    List<User> findAllByStatusAndProfilePosition(@Param("status") User.UserStatus status,
                                                 @Param("position") String position);

    @Query("SELECT u FROM User u WHERE u.status = :status AND u.subrole = :subrole")
    List<User> findAllByStatusAndSubrole(@Param("status") User.UserStatus status,
                                         @Param("subrole") User.UserSubrole subrole);

    // Поиск по городам и командам (пересечение)
    @Query("SELECT DISTINCT u FROM User u JOIN u.profile p WHERE u.status = :status " +
            "AND (:cityIds IS NULL OR p.homeCity.id IN :cityIds) " +
            "AND (:teamNames IS NULL OR p.team IN :teamNames)")
    List<User> findUsersByStatusAndCityIdsAndTeamNames(@Param("status") User.UserStatus status,
                                                       @Param("cityIds") List<Long> cityIds,
                                                       @Param("teamNames") List<String> teamNames);

    // Найти всех пользователей по статусу
    @Query("SELECT u FROM User u WHERE u.status = :status")
    List<User> findAllByStatus(@Param("status") User.UserStatus status);

    // Получить уникальные названия команд (только активные пользователи)
    @Query("SELECT DISTINCT p.team FROM User u JOIN u.profile p WHERE u.status = :status AND p.team IS NOT NULL AND p.team <> ''")
    List<String> findDistinctTeamsByStatus(@Param("status") User.UserStatus status);

    // Поиск пользователей по частичным совпадениям (для админского поиска)
    @Query("SELECT u FROM User u LEFT JOIN u.profile p WHERE " +
            "(:query IS NULL OR " +
            "CAST(u.id AS string) LIKE %:query% OR " +
            "u.phone LIKE %:query% OR " +
            "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    // Метод для получения всех городов (уникальные)
    @Query("SELECT DISTINCT c FROM User u JOIN u.profile p JOIN p.homeCity c WHERE c IS NOT NULL")
    List<City> findAllCitiesFromProfiles(); // или использовать CityRepository
}

