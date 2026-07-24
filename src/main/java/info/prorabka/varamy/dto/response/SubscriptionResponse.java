package info.prorabka.varamy.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Подписка на тип/подтип")
public class SubscriptionResponse {
    private Integer type;
    private Integer subType;
}
