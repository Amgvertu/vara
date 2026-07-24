// VerificationCode.java - исправление устаревшего API
package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_codes", indexes = {
        @Index(name = "idx_phone", columnList = "phone"),
        @Index(name = "idx_code", columnList = "code"),
        @Index(name = "idx_expiry", columnList = "expiry_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCode {

    @Id
    @GeneratedValue
    @UuidGenerator  // ← Используем современный генератор вместо устаревшего GenericGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "code", nullable = false, length = 6)
    private String code;

    @Column(name = "purpose", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private VerificationPurpose purpose;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "used", nullable = false)
    private boolean used = false;

    public enum VerificationPurpose {
        REGISTRATION,      // Регистрация
        PASSWORD_RESET,    // Сброс пароля
        PHONE_CHANGE       // Смена телефона
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isValid() {
        return !isExpired() && !used;
    }
}