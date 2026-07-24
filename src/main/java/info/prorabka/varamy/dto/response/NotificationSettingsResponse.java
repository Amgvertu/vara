package info.prorabka.varamy.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Настройки уведомлений пользователя")
public class NotificationSettingsResponse {
    private boolean notifyOnResponseToMyAd;
    private boolean notifyOnMyResponseAccepted;
    private boolean notifyNewAdsInCity;
    private CitySimpleResponse notificationCity;  // может быть null
    private List<SubscriptionResponse> subscriptions;
}