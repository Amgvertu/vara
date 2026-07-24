package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "ads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"author", "city", "adRinks", "responses", "details"})
// исключаем все поля, которые могут вызвать рекурсию: author (User), city (City), adRinks, responses, details (может содержать ссылки)
public class Ad {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "type", nullable = false)
    private Integer type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AdStatus status = AdStatus.MODERATION;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @ElementCollection
    @CollectionTable(name = "ad_levels", joinColumns = @JoinColumn(name = "ad_id"))
    @Column(name = "level")
    private List<String> levels;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(name = "team", length = 200)
    private String team;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @OneToMany(mappedBy = "ad", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AdRink> adRinks = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private AdDetails details;

    @OneToMany(mappedBy = "ad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Response> responses;

    public enum AdStatus {
        MODERATION, ACTIVE, FILLED, ARCHIVED
    }

    @Column(name = "sub_type")
    private Integer subType;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "show_team")
    private Boolean showTeam = true;

    @Column(name = "goalies_count")
    private Integer goaliesCount;

    @Column(name = "defenders_count")
    private Integer defendersCount;

    @Column(name = "forwards_count")
    private Integer forwardsCount;

    @Column(name = "accepted_goalies_count")
    private Integer acceptedGoaliesCount = 0;

    @Column(name = "accepted_defenders_count")
    private Integer acceptedDefendersCount = 0;

    @Column(name = "accepted_forwards_count")
    private Integer acceptedForwardsCount = 0;

    @Column(name = "accepted_responses_count")
    private Integer acceptedResponsesCount = 0;
}