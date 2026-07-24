// RegisterWithVerificationRequest.java
package info.prorabka.varamy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Запрос на регистрацию с подтверждением")
public class RegisterWithVerificationRequest {

    @NotBlank(message = "Телефон обязателен")
    @Pattern(regexp = "^\\+7\\d{10}$", message = "Телефон должен быть в формате +7XXXXXXXXXX")
    @Schema(description = "Номер телефона", example = "+79001234567")
    private String phone;

    @NotBlank(message = "Пароль обязателен")
    @Schema(description = "Пароль", example = "password")
    private String password;

    @Schema(description = "ID страны", example = "1")
    private Long countryId;

    @Schema(description = "ID региона", example = "1")
    private Long regionId;

    @Schema(description = "ID города", example = "1")
    private Long cityId;

    @NotBlank(message = "Код подтверждения обязателен")
    @Pattern(regexp = "^\\d{6}$", message = "Код должен состоять из 6 цифр")
    @Schema(description = "Код подтверждения", example = "123456")
    private String code;
}