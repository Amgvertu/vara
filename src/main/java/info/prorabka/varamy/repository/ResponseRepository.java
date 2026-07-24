package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.Response;
import info.prorabka.varamy.entity.Ad;
import info.prorabka.varamy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;  // <-- Добавить этот импорт
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResponseRepository extends JpaRepository<Response, UUID> {

    boolean existsByAdAndUser(Ad ad, User user);
    List<Response> findByAd(Ad ad);
    @Query("SELECT r FROM Response r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH u.profile p " +
            "WHERE r.ad = :ad")
    List<Response> findByAdWithUserAndProfile(@Param("ad") Ad ad);

    @Query("SELECT r FROM Response r " +
            "JOIN FETCH r.ad a " +
            "JOIN FETCH a.author " +
            "JOIN FETCH a.city c " +
            "JOIN FETCH c.region reg " +
            "JOIN FETCH reg.country " +
            "WHERE r.user.id = :userId " +
            "ORDER BY r.createdAt DESC")
    List<Response> findResponsesByUserIdWithAds(@Param("userId") UUID userId);

    // Альтернативный вариант с пагинацией
    @Query("SELECT r FROM Response r " +
            "JOIN FETCH r.ad a " +
            "JOIN FETCH a.author " +
            "JOIN FETCH a.city c " +
            "JOIN FETCH c.region reg " +
            "JOIN FETCH reg.country " +
            "WHERE r.user.id = :userId " +
            "ORDER BY r.createdAt DESC")
    Page<Response> findResponsesByUserIdWithAds(@Param("userId") UUID userId, Pageable pageable);

    
}
