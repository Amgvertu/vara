package info.prorabka.varamy.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Объявление с информацией об отклике пользователя")
public class MyResponseAdResponse {

    @Schema(description = "Полная информация об объявлении")
    private AdResponse ad;

    @Schema(description = "Информация об отклике пользователя")
    private ResponseResponse myResponse;
}