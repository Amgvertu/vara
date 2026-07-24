package info.prorabka.varamy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос на обновление настроек уведомлений")
public class UpdateNotificationSettingsRequest {
    @Schema(description = "Уведомлять при отклике на моё объявление")
    private Boolean notifyOnResponseToMyAd;

    @Schema(description = "Уведомлять при принятии моего отклика")
    private Boolean notifyOnMyResponseAccepted;

    @Schema(description = "Уведомлять о новых объявлениях в городе")
    private Boolean notifyNewAdsInCity;

    @Schema(description = "ID города для уведомлений (может быть null)")
    private Long notificationCityId;
}
