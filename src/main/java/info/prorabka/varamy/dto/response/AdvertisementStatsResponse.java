package info.prorabka.varamy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdvertisementStatsResponse {
    private long total;                 // общее количество реклам (всех статусов)
    private long active;                // активных
    private long paused;                // приостановленных
    private long expired;               // истекших
    private long deleted;               // удалённых
}
