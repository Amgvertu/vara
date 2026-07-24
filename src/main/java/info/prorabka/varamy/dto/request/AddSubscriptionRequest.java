package info.prorabka.varamy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Запрос на добавление подписки на тип/подтип объявления")
public class AddSubscriptionRequest {
    @NotNull
    @Schema(description = "Тип объявления (1-5)", example = "1")
    private Integer type;

    @NotNull
    @Schema(description = "Подтип объявления", example = "1")
    private Integer subType;
}