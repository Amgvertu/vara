package info.prorabka.varamy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitySimpleResponse {
    private Long id;
    private String name;
}