package info.prorabka.varamy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на обновление токена")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token обязателен")
    @Schema(description = "Refresh токен")
    private String refreshToken;
}
