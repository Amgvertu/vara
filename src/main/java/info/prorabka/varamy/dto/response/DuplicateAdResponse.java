// DuplicateAdResponse.java
package info.prorabka.varamy.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о существующем дублирующемся объявлении")
public class DuplicateAdResponse {

    @Schema(description = "ID существующего объявления")
    private UUID id;

    @Schema(description = "Время начала события")
    private LocalDateTime startTime;

    @Schema(description = "Название ЛДС")
    private String rinkName;

    @Schema(description = "Город")
    private String cityName;

    @Schema(description = "Статус объявления")
    private String status;

    @Schema(description = "Количество принятых откликов / общее количество")
    private String filledProgress;
}
