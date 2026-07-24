package info.prorabka.varamy.repository;

import info.prorabka.varamy.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    Optional<VerificationCode> findByPhoneAndCodeAndPurposeAndUsedFalse(
            String phone,
            String code,
            VerificationCode.VerificationPurpose purpose);

    @Modifying
    @Transactional
    @Query("UPDATE VerificationCode v SET v.used = true WHERE v.phone = :phone AND v.purpose = :purpose AND v.used = false")
    void invalidateAllCodesForPhone(@Param("phone") String phone, @Param("purpose") VerificationCode.VerificationPurpose purpose);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationCode v WHERE v.expiryDate < :now")
    void deleteExpiredCodes(@Param("now") LocalDateTime now);  // ← Изменено с int на void
}