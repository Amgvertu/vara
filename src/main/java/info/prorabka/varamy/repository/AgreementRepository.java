package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long> {
    Optional<Agreement> findByType(String type);
}
