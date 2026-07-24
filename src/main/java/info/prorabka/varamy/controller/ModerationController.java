package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.response.AdResponse;
import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@Tag(name = "Модерация", description = "Модерация объявлений")
public class ModerationController {

    private final AdService adService;

    @GetMapping("/ads")
    @Operation(summary = "Получение списка объявлений на модерации")
    public ResponseEntity<ApiResponse<Page<AdResponse>>> getAdsForModeration(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdResponse> ads = adService.getAdsForModeration(pageable);
        return ResponseEntity.ok(ApiResponse.success(ads));
    }

    @PutMapping("/ads/{adId}/approve")
    @Operation(summary = "Одобрить объявление")
    public ResponseEntity<ApiResponse<AdResponse>> approveAd(@PathVariable UUID adId) {
        AdResponse response = adService.moderateAd(adId, true);
        return ResponseEntity.ok(ApiResponse.success("Объявление одобрено", response));
    }

    @PutMapping("/ads/{adId}/reject")
    @Operation(summary = "Отклонить объявление")
    public ResponseEntity<ApiResponse<AdResponse>> rejectAd(@PathVariable UUID adId) {
        AdResponse response = adService.moderateAd(adId, false);
        return ResponseEntity.ok(ApiResponse.success("Объявление отклонено", response));
    }
}
