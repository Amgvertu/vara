package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "rinks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"country", "region", "city"})
public class Rink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, columnDefinition = "text")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(name = "address", nullable = false, columnDefinition = "text")
    private String address;

    @Column(name = "phone", columnDefinition = "text")
    private String phone;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "features", columnDefinition = "text[]")
    private List<String> features;
}