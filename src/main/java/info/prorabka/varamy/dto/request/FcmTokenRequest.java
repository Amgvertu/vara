package info.prorabka.varamy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FcmTokenRequest {
    @NotBlank
    private String token;
}
