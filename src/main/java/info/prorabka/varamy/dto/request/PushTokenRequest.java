package info.prorabka.varamy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PushTokenRequest {
    @NotBlank
    private String token;
    @NotBlank
    private String platform; // "FCM" или "HMS"
}