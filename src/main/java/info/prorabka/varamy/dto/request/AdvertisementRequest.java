package info.prorabka.varamy.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class AdvertisementRequest {

    @NotBlank(message = "Название рекламодателя обязательно")
    private String advertiser;

    @NotBlank(message = "Ссылка на изображение обязательна")
    private String imageUrl;

    private String link;                // опционально
    private String refData;             // опционально

    @NotNull(message = "Тип рекламы обязателен (1 или 2)")
    @Min(1)
    @Max(2)
    private Integer type;

    @Min(5)
    @Max(10)
    private Integer interval;           // только для type=1

    @NotNull(message = "Срок действия в днях обязателен")
    @Min(1)
    private Integer periodDays;

    private Boolean allCities = false;  // если true – cityIds игнорируется
    private List<Long> cityIds;         // список ID городов
}