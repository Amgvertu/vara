package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"profile"})   // <-- не включаем profile в toString
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "phone", unique = true, nullable = false, columnDefinition = "text")
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "subrole", length = 20)
    private UserSubrole subrole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Profile profile;

    public enum UserRole {
        USER, MODERATOR, ADMIN, ADVERT, SMS_GATEWAY
    }

    public enum UserSubrole {
        PLAYER, SPECIALIST
    }

    public enum UserStatus {
        ACTIVE, BLOCKED
    }
}