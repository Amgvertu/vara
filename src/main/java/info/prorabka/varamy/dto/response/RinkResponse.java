package info.prorabka.varamy.dto.response;

import lombok.Data;

@Data
public class RinkResponse {

    private Long id;
    private String name;
    private CountryResponse country;
    private RegionResponse region;
    private CityResponse city;
    private String address;
    private String phone;
    private Double rating;
    private Double latitude;
    private Double longitude;
    private String[] features;
}
