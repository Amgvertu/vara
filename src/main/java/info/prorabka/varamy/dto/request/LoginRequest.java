package info.prorabka.varamy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на вход")
public class LoginRequest {

    @NotBlank(message = "Телефон обязателен")
    @Schema(description = "Номер телефона", example = "+79001234567")
    private String phone;

    @NotBlank(message = "Пароль обязателен")
    @Schema(description = "Пароль", example = "password")
    private String password;
}
