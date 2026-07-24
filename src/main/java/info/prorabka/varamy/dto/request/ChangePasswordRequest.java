package info.prorabka.varamy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на смену пароля")
public class ChangePasswordRequest {

    @NotBlank(message = "Старый пароль обязателен")
    @Schema(description = "Старый пароль")
    private String oldPassword;

    @NotBlank(message = "Новый пароль обязателен")
    @Schema(description = "Новый пароль")
    private String newPassword;
}
