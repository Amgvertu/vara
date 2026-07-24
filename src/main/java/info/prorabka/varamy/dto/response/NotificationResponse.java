package info.prorabka.varamy.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Уведомление")
public class NotificationResponse {
    private Long id;
    private String type;            // RESPONSE, RESPONSE_ACCEPTED, NEW_AD
    private String content;
    private UUID relatedEntityId;
    private boolean isRead;
    private LocalDateTime createdAt;
}