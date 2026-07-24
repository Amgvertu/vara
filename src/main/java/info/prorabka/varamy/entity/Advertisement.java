package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "advertisements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"cityIds"})   // исключаем список городов, чтобы избежать рекурсии
public class Advertisement {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "advertiser", nullable = false, length = 255)
    private String advertiser;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(name = "link", length = 512)
    private String link;

    @Column(name = "ref_data", length = 255)
    private String refData;

    @Column(name = "type", nullable = false)
    private Integer type;   // 1 – в ленте, 2 – в диалоге

    @Column(name = "interval_n")
    private Integer interval;

    @Column(name = "period_days", nullable = false)
    private Integer periodDays;

    @Column(name = "all_cities", nullable = false)
    private boolean allCities = false;

    @ElementCollection
    @CollectionTable(name = "advertisement_cities", joinColumns = @JoinColumn(name = "advertisement_id"))
    @Column(name = "city_id")
    private List<Long> cityIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AdvertisementStatus status = AdvertisementStatus.ACTIVE;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AdvertisementStatus {
        ACTIVE, PAUSED, EXPIRED, DELETED
    }

    public void recalculateEndDate() {
        if (startDate != null && periodDays != null) {
            endDate = startDate.plusDays(periodDays);
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDate);
    }
}