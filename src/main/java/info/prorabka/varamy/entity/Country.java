package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "country")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "regions")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 2)
    private String code;

    @Column(name = "phone_code", length = 10)
    private String phoneCode;

    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL)
    private List<Region> regions;
}
