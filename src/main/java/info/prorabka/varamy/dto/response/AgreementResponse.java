package info.prorabka.varamy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AgreementResponse {
    private String type;
    private String content;
    private LocalDateTime updatedAt;
}