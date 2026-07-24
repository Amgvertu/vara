package info.prorabka.varamy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserMessageResponse {
    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private String link;
    private String category;
    private LocalDateTime createdAt;
    private boolean isRead;
}