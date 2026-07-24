package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findByRegionId(Long regionId);

    @Query("SELECT c FROM City c WHERE c.region.country.id = :countryId")
    List<City> findByCountryId(@Param("countryId") Long countryId);
}