package info.prorabka.varamy.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FeedbackRequest {
    @NotBlank(message = "Фамилия и имя обязательны")
    private String fullName;

    @NotBlank(message = "Телефон обязателен")
    @Pattern(regexp = "^\\+7\\d{10}$", message = "Телефон должен быть в формате +7XXXXXXXXXX")
    private String phone;

    @NotBlank(message = "E-mail обязателен")
    @Email(message = "Неверный формат email")
    private String email;

    @NotBlank(message = "Тема обязательна")
    private String subject;

    @NotBlank(message = "Сообщение обязательно")
    private String message;
}