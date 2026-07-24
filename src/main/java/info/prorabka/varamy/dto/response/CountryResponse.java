package info.prorabka.varamy.dto.response;

import lombok.Data;

@Data
public class CountryResponse {

    private Long id;
    private String name;
    private String code;
    private String phoneCode;
}