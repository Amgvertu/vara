package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.Rink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RinkRepository extends JpaRepository<Rink, Long> {

    List<Rink> findByCityId(Long cityId);

    @Query("SELECT r FROM Rink r WHERE " +
            "(:cityId IS NULL OR r.city.id = :cityId) AND " +
            "(:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.address) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Rink> findWithFilters(
            @Param("cityId") Long cityId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT r FROM Rink r WHERE " +
            "(:query IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.address) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:cityId IS NULL OR r.city.id = :cityId)")
    List<Rink> search(@Param("query") String query, @Param("cityId") Long cityId);
}
