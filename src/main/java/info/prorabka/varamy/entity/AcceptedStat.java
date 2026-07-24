package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accepted_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptedStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "response_id", nullable = false)
    private UUID responseId;

    @Column(name = "ad_id", nullable = false)
    private UUID adId;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;
}