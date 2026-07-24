package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "position", length = 50)
    private String position;
}