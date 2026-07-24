package info.prorabka.varamy.dto.response;

import lombok.Data;

@Data
public class RegionResponse {

    private Long id;
    private String name;
    private CountryResponse country;
    private String autoCode;
}