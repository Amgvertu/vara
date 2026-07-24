package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.AcceptedStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AcceptedStatRepository extends JpaRepository<AcceptedStat, Long>, JpaSpecificationExecutor<AcceptedStat> {
    // Все методы удалены, теперь используем спецификации
}
