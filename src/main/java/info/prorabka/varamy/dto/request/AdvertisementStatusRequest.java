package info.prorabka.varamy.dto.request;

import info.prorabka.varamy.entity.Advertisement;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdvertisementStatusRequest {
    @NotNull
    private Advertisement.AdvertisementStatus status;
}