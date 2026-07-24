package info.prorabka.varamy.repository;

import info.prorabka.varamy.dto.response.AdStatsResponse;
import info.prorabka.varamy.entity.Ad;
import info.prorabka.varamy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdRepository extends JpaRepository<Ad, UUID>, JpaSpecificationExecutor<Ad> {

    // ============= ПУБЛИЧНЫЕ МЕТОДЫ (ACTIVE и FILLED) =============

    @Query("SELECT DISTINCT a FROM Ad a LEFT JOIN a.levels l WHERE " +
            "a.status IN :statuses AND " +
            "(:cityId IS NULL OR a.city.id = :cityId) AND " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(:subType IS NULL OR a.subType = :subType) AND " +
            "(:level IS NULL OR l IN :level) AND " +
            "(:authorId IS NULL OR a.author.id = :authorId)")
    Page<Ad> findActiveAndFilledAds(
            @Param("cityId") Long cityId,
            @Param("type") Integer type,
            @Param("subType") Integer subType,
            @Param("statuses") List<Ad.AdStatus> statuses,
            @Param("level") List<String> level,
            @Param("authorId") UUID authorId,
            Pageable pageable);

    @Query("SELECT DISTINCT a FROM Ad a LEFT JOIN a.levels l WHERE " +
            "a.status IN :statuses AND " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(:subType IS NULL OR a.subType = :subType) AND " +
            "(:level IS NULL OR l IN :level)")
    Page<Ad> findAllActiveAndFilledPublic(
            @Param("type") Integer type,
            @Param("subType") Integer subType,
            @Param("statuses") List<Ad.AdStatus> statuses,
            @Param("level") List<String> level,
            Pageable pageable);

    @Query("SELECT DISTINCT a FROM Ad a LEFT JOIN a.levels l WHERE " +
            "a.status IN :statuses AND " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(:subType IS NULL OR a.subType = :subType) AND " +
            "(:level IS NULL OR l IN :level)")
    Page<Ad> findMainPageAdsPublic(
            @Param("statuses") List<Ad.AdStatus> statuses,
            @Param("type") Integer type,
            @Param("subType") Integer subType,
            @Param("level") List<String> level,
            Pageable pageable);

    // ============= АДМИНИСТРАТИВНЫЕ МЕТОДЫ (могут включать ARCHIVED) =============

    // ============= МЕТОДЫ ДЛЯ АВТОРА =============

    @Query("SELECT a FROM Ad a WHERE a.author = :author ORDER BY a.createdAt DESC")
    Page<Ad> findByAuthor(@Param("author") User author, Pageable pageable);

    // ============= МЕТОДЫ ДЛЯ МОДЕРАЦИИ =============

    Page<Ad> findByStatus(Ad.AdStatus status, Pageable pageable);

    // ============= МЕТОДЫ ДЛЯ АРХИВАЦИИ И ОЧИСТКИ =============

    @Query("SELECT a FROM Ad a WHERE a.endTime < :endTime AND a.status != :status")
    List<Ad> findByEndTimeBeforeAndStatusNot(
            @Param("endTime") LocalDateTime endTime,
            @Param("status") Ad.AdStatus status);

    @Query("SELECT a FROM Ad a WHERE a.status = :status AND a.createdAt < :date")
    List<Ad> findByStatusAndCreatedAtBefore(
            @Param("status") Ad.AdStatus status,
            @Param("date") LocalDateTime date);

    long countByStatus(Ad.AdStatus status);

    // ============= ПРОВЕРКА ДУБЛИКАТОВ =============

    @Query("SELECT a FROM Ad a WHERE " +
            "a.type = :type AND " +
            "a.subType = :subType AND " +
            "a.city.id = :cityId AND " +
            "a.status IN :statuses AND " +
            "a.startTime BETWEEN :startTimeMinus AND :startTimePlus")
    List<Ad> findDuplicateAds(
            @Param("type") Integer type,
            @Param("subType") Integer subType,
            @Param("cityId") Long cityId,
            @Param("statuses") List<Ad.AdStatus> statuses,
            @Param("startTimeMinus") LocalDateTime startTimeMinus,
            @Param("startTimePlus") LocalDateTime startTimePlus);

    @Modifying
    @Query("DELETE FROM Ad a WHERE a.author = :author")
    void deleteByAuthor(@Param("author") User author);

    @Query("SELECT new info.prorabka.varamy.dto.response.AdStatsResponse(" +
            "COUNT(DISTINCT a), " +
            "COUNT(r), " +
            "SUM(CASE WHEN r.status = 'APPROVED' THEN 1 ELSE 0 END)) " +
            "FROM Ad a LEFT JOIN a.responses r " +
            "WHERE (:cityIds IS NULL OR a.city.id IN :cityIds) " +
            "AND (:dateFrom IS NULL OR a.createdAt >= :dateFrom) " +
            "AND (:dateTo IS NULL OR a.createdAt <= :dateTo) " +
            "AND (:statuses IS NULL OR a.status IN :statuses)")
    AdStatsResponse getStatisticsWithStatuses(@Param("cityIds") List<Long> cityIds,
                                              @Param("dateFrom") LocalDateTime dateFrom,
                                              @Param("dateTo") LocalDateTime dateTo,
                                              @Param("statuses") List<Ad.AdStatus> statuses);
}