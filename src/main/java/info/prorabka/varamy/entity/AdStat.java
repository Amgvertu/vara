package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ad_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ad_id", nullable = false)
    private UUID adId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "city_id")
    private Long cityId;
}