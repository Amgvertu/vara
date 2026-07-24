package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.response.AgreementResponse;
import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.entity.Agreement;
import info.prorabka.varamy.exception.ResourceNotFoundException;
import info.prorabka.varamy.service.AgreementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agreements")
@RequiredArgsConstructor
@Tag(name = "Соглашения", description = "Получение текстов соглашений (пользовательское соглашение и т.д.)")
public class AgreementController {

    private final AgreementService agreementService;

    @GetMapping("/{type}")
    @Operation(summary = "Получить текст соглашения по его типу",
            description = "Возвращает актуальный текст соглашения. Например, type = terms_of_service")
    public ResponseEntity<ApiResponse<AgreementResponse>> getAgreement(
            @Parameter(description = "Тип соглашения", example = "terms_of_service")
            @PathVariable String type) {

        Agreement agreement = agreementService.getAgreementByType(type)
                .orElseThrow(() -> new ResourceNotFoundException("Соглашение не найдено для типа: " + type));

        AgreementResponse response = new AgreementResponse(
                agreement.getType(),
                agreement.getContent(),
                agreement.getUpdatedAt()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}