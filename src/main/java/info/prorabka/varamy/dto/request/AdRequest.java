package info.prorabka.varamy.dto.request;

import info.prorabka.varamy.entity.Ad;
import info.prorabka.varamy.entity.AdDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Schema(description = "Запрос на создание/обновление объявления")
public class AdRequest {

    //@NotNull(message = "Тип объявления обязателен")
    @Schema(description = "Тип объявления (1-5)", example = "1")
    private Integer type;

    //@NotNull(message = "Время начала обязательно")
    @Schema(description = "Время начала события")
    private LocalDateTime startTime;

    @Schema(description = "Уровни игроков", example = "[\"A\",\"B\"]")
    private List<String> level;

    //@NotNull(message = "ID города обязателен")
    @Schema(description = "ID города", example = "57")
    private Long cityId;

    @Schema(description = "Название команды", example = "Динамо")
    private String team;

    @Schema(description = "Контактное имя", example = "Иван")
    private String contactName;

    @Schema(description = "Контактный телефон", example = "+79001234567")
    private String contactPhone;

    @Schema(description = "ID катков", example = "[1,2,3]")
    private List<Long> rinkIds;

    @Schema(description = "Детали объявления")
    private AdDetails details;

    @Schema(description = "Подтип объявления (1-2 для type=1,2,3; 1-4 для type=4)", example = "1")
    private Integer subType;

    @Schema(description = "Время окончания (для диапазона времени)", example = "2026-03-26T20:00:00")
    private LocalDateTime endTime;

    @Schema(description = "Отображать команду в объявлении", example = "true")
    private Boolean showTeam = true;

    @Schema(description = "Количество вратарей (только для type=1, subType=1)", example = "2")
    private Integer goaliesCount;

    @Schema(description = "Количество защитников (только для type=1, subType=2)", example = "2")
    private Integer defendersCount;

    @Schema(description = "Количество нападающих (только для type=1, subType=2)", example = "2")
    private Integer forwardsCount;

    @Schema(description = "Статус объявления (для архивации автором)")
    private Ad.AdStatus status;
}