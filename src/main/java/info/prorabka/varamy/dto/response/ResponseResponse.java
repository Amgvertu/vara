package info.prorabka.varamy.dto.response;

import info.prorabka.varamy.entity.City;
import info.prorabka.varamy.entity.Country;
import info.prorabka.varamy.entity.Region;
import info.prorabka.varamy.entity.Response;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ResponseResponse {

    private UUID id;
    private UUID adId;
    private UUID userId;
    private UserResponse user;
    private Response.ResponseStatus status;
    private String message;
    private LocalDateTime createdAt;
    @Schema(description = "Роль отклика (для объявлений 1.2)")
    private Response.ResponseRole responseRole;
}
