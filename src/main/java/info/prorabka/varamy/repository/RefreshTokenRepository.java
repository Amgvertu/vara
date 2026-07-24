package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.RefreshToken;
import info.prorabka.varamy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}