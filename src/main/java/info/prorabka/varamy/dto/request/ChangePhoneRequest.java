// ChangePhoneRequest.java
package info.prorabka.varamy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Запрос на смену телефона")
public class ChangePhoneRequest {

    @NotBlank(message = "Новый телефон обязателен")
    @Pattern(regexp = "^\\+7\\d{10}$", message = "Телефон должен быть в формате +7XXXXXXXXXX")
    @Schema(description = "Новый номер телефона", example = "+79009998877")
    private String newPhone;

    @NotBlank(message = "Код подтверждения обязателен")
    @Pattern(regexp = "^\\d{6}$", message = "Код должен состоять из 6 цифр")
    @Schema(description = "Код подтверждения", example = "123456")
    private String code;
}