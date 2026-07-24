package info.prorabka.varamy.dto.request;

import info.prorabka.varamy.entity.Response;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание отклика")
public class ResponseRequest {

    @Schema(description = "Сообщение к отклику", example = "Хочу присоединиться!")
    private String message;

    @Schema(description = "Роль, на которую откликается пользователь (DEFENDER или FORWARD)")
    private Response.ResponseRole responseRole;
}
