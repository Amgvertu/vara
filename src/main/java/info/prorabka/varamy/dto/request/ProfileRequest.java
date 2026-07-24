package info.prorabka.varamy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Запрос на обновление профиля")
public class ProfileRequest {

    @Schema(description = "Имя", example = "Иван")
    private String firstName;

    @Schema(description = "Фамилия", example = "Иванов")
    private String lastName;

    @Schema(description = "Дата рождения", example = "1990-01-01")
    private LocalDate birthDate;

    @Schema(description = "Амплуа", example = "вратарь")
    private String position;

    @Schema(description = "Уровень", example = "A")
    private String level;

    @Schema(description = "Игровой номер", example = "99")
    private Integer number;

    @Schema(description = "Команда", example = "Динамо")
    private String team;

    @Schema(description = "Email", example = "ivan@example.com")
    private String email;

    @Schema(description = "ID домашней страны", example = "1")
    private Long homeCountryId;

    @Schema(description = "ID домашнего региона", example = "1")
    private Long homeRegionId;

    @Schema(description = "ID домашнего города", example = "1")
    private Long homeCityId;
}