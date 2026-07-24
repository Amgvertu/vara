package info.prorabka.varamy.dto.request;

import info.prorabka.varamy.entity.DeliveryCriteria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Запрос на отправку административного сообщения")
public class AdminMessageRequest {

    @Schema(description = "Заголовок (опционально)")
    private String title;

    @Schema(description = "Текст сообщения (для текстовых – обязательно)")
    private String content;

    @Schema(description = "Ссылка на изображение (для графических – обязательно)")
    private String imageUrl;

    @Schema(description = "Ссылка (опционально)")
    private String link;

    @NotNull(message = "Категория обязательна")
    @Schema(description = "INTERNAL или PUSH", allowableValues = {"INTERNAL", "PUSH"})
    private String category;

    @NotNull(message = "Критерии доставки обязательны")
    @Valid
    private DeliveryCriteria delivery;
}