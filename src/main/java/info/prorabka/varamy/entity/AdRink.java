package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "ad_rinks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"ad", "rink"})   // исключаем связанные сущности
public class AdRink {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private AdRinkId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("adId")
    @JoinColumn(name = "ad_id", columnDefinition = "uuid")
    private Ad ad;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("rinkId")
    @JoinColumn(name = "rink_id")
    @EqualsAndHashCode.Include
    private Rink rink;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class AdRinkId implements Serializable {
        @Column(name = "ad_id", columnDefinition = "uuid")
        @EqualsAndHashCode.Include
        private UUID adId;

        @Column(name = "rink_id")
        @EqualsAndHashCode.Include
        private Long rinkId;
    }
}