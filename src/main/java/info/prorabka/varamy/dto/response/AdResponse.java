package info.prorabka.varamy.dto.response;

import info.prorabka.varamy.entity.Ad;
import info.prorabka.varamy.entity.AdDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
public class AdResponse {

    private UUID id;
    private UUID authorId;
    private UserResponse author;
    private Integer type;
    private Ad.AdStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startTime;
    private List<String> level; // теперь список строк, а не массив
    private CityResponse city;
    private String team;
    private String contactName;
    private String contactPhone;
    private List<Long> rinkIds;
    private AdDetails details;
    private List<ResponseResponse> responses;
    @Schema(description = "Подтип объявления")
    private Integer subType;

    @Schema(description = "Время окончания (для диапазона времени)")
    private LocalDateTime endTime;

    @Schema(description = "Отображать команду в объявлении")
    private Boolean showTeam;

    @Schema(description = "Количество вратарей (только для type=1, subType=1)")
    private Integer goaliesCount;

    @Schema(description = "Количество защитников (только для type=1, subType=2)")
    private Integer defendersCount;

    @Schema(description = "Количество нападающих (только для type=1, subType=2)")
    private Integer forwardsCount;

    @Schema(description = "Принято откликов вратарей")
    private Integer acceptedGoaliesCount;

    @Schema(description = "Принято откликов защитников")
    private Integer acceptedDefendersCount;

    @Schema(description = "Принято откликов нападающих")
    private Integer acceptedForwardsCount;

}
