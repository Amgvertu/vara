package info.prorabka.varamy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdStatsResponse {
    // Текущие данные (из основных таблиц)
    private long currentAds;
    private long currentResponses;
    private long currentAccepted;

    // Кумулятивные данные (из таблиц статистики)
    private long cumulativeAds;
    private long cumulativeResponses;
    private long cumulativeAccepted;
}