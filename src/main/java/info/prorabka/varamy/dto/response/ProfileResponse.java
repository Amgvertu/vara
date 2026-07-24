package info.prorabka.varamy.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ProfileResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String position;
    private String level;
    private Integer number;
    private String team;
    private String email;
    private String avatarUrl;
    private CountryResponse homeCountry;
    private RegionResponse homeRegion;
    private CityResponse homeCity;
}
