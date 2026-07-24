package info.prorabka.varamy.dto.response;

import info.prorabka.varamy.entity.Advertisement;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AdvertisementResponse {
    private UUID id;
    private String advertiser;
    private String imageUrl;
    private String link;
    private String refData;
    private Integer type;
    private Integer interval;
    private Integer periodDays;
    private boolean allCities;
    private List<Long> cityIds;
    private Advertisement.AdvertisementStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}