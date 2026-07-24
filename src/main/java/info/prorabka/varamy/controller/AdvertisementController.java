package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.request.AdvertisementRequest;
import info.prorabka.varamy.dto.request.AdvertisementStatusRequest;
import info.prorabka.varamy.dto.response.AdvertisementResponse;
import info.prorabka.varamy.dto.response.AdvertisementStatsResponse;
import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.entity.Advertisement;
import info.prorabka.varamy.service.AdvertisementService;
import info.prorabka.varamy.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/advertisements")
@RequiredArgsConstructor
@Tag(name = "Реклама", description = "Управление рекламными постами")
public class AdvertisementController {

    private final AdvertisementService advertisementService;
    private final FileStorageService fileStorageService;

    // ========== ПУБЛИЧНЫЙ ЭНДПОИНТ (для клиента) ==========

    @GetMapping("/active")
    @Operation(summary = "Получить активные рекламы для города и типа")
    public ResponseEntity<ApiResponse<List<AdvertisementResponse>>> getActive(
            @RequestParam Integer type,
            @RequestParam Long cityId) {
        List<AdvertisementResponse> ads = advertisementService.getActiveAdvertisements(type, cityId);
        return ResponseEntity.ok(ApiResponse.success(ads));
    }

    // ========== ПОЛУЧЕНИЕ ОДНОЙ РЕКЛАМЫ (для редактирования) ==========

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADVERT')")
    @Operation(summary = "Получить рекламу по ID")
    public ResponseEntity<ApiResponse<AdvertisementResponse>> getById(@PathVariable UUID id) {
        AdvertisementResponse response = advertisementService.getAdvertisementById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== АДМИНИСТРАТИВНЫЙ СПИСОК (с фильтрами) ==========
    // ВАЖНО: путь указан как "" (пустая строка) – это обрабатывает GET /api/advertisements
    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADVERT')")
    @Operation(summary = "Получить список всех реклам (с фильтрами)")
    public ResponseEntity<ApiResponse<Page<AdvertisementResponse>>> getAdminList(
            @RequestParam(required = false) List<Advertisement.AdvertisementStatus> status,
            @RequestParam(required = false) String advertiser,
            @RequestParam(required = false) List<Long> cityIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTo,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdvertisementResponse> page = advertisementService.getAdminAdvertisements(
                status, advertiser, cityIds, dateFrom, dateTo, endDateFrom, endDateTo, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    // ========== СОЗДАНИЕ ==========

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADVERT')")
    @Operation(summary = "Создать новую рекламу")
    public ResponseEntity<ApiResponse<AdvertisementResponse>> create(
            @Valid @RequestBody AdvertisementRequest request) {
        AdvertisementResponse response = advertisementService.createAdvertisement(request);
        return ResponseEntity.ok(ApiResponse.success("Реклама создана", response));
    }

    // ========== ОБНОВЛЕНИЕ ==========

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADVERT')")
    @Operation(summary = "Обновить рекламу")
    public ResponseEntity<ApiResponse<AdvertisementResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AdvertisementRequest request) {
        AdvertisementResponse response = advertisementService.updateAdvertisement(id, request);
        return ResponseEntity.ok(ApiResponse.success("Реклама обновлена", response));
    }

    // ========== СМЕНА СТАТУСА ==========

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADVERT')")
    @Operation(summary = "Изменить статус рекламы (пауза/возобновление)")
    public ResponseEntity<ApiResponse<Void>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AdvertisementStatusRequest request) {
        advertisementService.changeStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Статус изменён", null));
    }

    // ========== УДАЛЕНИЕ (МЯГКОЕ) ==========

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADVERT')")
    @Operation(summary = "Удалить рекламу (мягкое удаление)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        advertisementService.deleteAdvertisement(id);
        return ResponseEntity.ok(ApiResponse.success("Реклама удалена", null));
    }

    // ========== ЗАГРУЗКА ИЗОБРАЖЕНИЯ ==========

    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADVERT')")
    @Operation(summary = "Загрузить изображение для рекламы")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        String imageUrl = fileStorageService.storeFile(file, "advertisements");
        Map<String, String> data = new HashMap<>();
        data.put("imageUrl", imageUrl);
        return ResponseEntity.ok(ApiResponse.success("Изображение загружено", data));
    }

    // ========== СТАТИСТИКА ==========

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить статистику по рекламе")
    public ResponseEntity<ApiResponse<AdvertisementStatsResponse>> getStatistics(
            @RequestParam(required = false) List<Long> cityIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo) {
        AdvertisementStatsResponse stats = advertisementService.getStatistics(cityIds, dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}