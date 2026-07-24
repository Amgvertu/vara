package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.Advertisement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, UUID>, JpaSpecificationExecutor<Advertisement> {

    // Найти активные рекламы для города (тип 1 или 2)
    @Query("SELECT a FROM Advertisement a " +
            "WHERE a.status = 'ACTIVE' " +
            "AND a.endDate >= :now " +
            "AND a.type = :type " +
            "AND (a.allCities = true OR :cityId IN elements(a.cityIds))")
    List<Advertisement> findActiveByTypeAndCity(@Param("type") Integer type,
                                                @Param("cityId") Long cityId,
                                                @Param("now") LocalDateTime now);

    // Найти все рекламы с истекшим сроком (для фоновой задачи)
    @Query("SELECT a FROM Advertisement a WHERE a.status = 'ACTIVE' AND a.endDate < :now")
    List<Advertisement> findExpiredActive(@Param("now") LocalDateTime now);
}