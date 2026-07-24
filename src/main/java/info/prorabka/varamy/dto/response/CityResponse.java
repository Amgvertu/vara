package info.prorabka.varamy.dto.response;

import lombok.Data;

@Data
public class CityResponse {

    private Long id;
    private String name;
    private RegionResponse region;
}
