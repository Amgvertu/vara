package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.AdStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AdStatRepository extends JpaRepository<AdStat, Long>, JpaSpecificationExecutor<AdStat> {
    // Все методы удалены, теперь используем спецификации
}