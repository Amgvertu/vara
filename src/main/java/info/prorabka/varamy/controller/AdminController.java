package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.response.AdResponse;
import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.dto.response.RinkResponse;
import info.prorabka.varamy.entity.Ad;
import info.prorabka.varamy.service.AdService;
import info.prorabka.varamy.service.RinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Администрирование", description = "Административные функции")
public class AdminController {

    private final RinkService rinkService;
    private final AdService adService;

    // ВСЕ МЕТОДЫ УПРАВЛЕНИЯ ПОЛЬЗОВАТЕЛЯМИ УДАЛЕНЫ (теперь в AdminUserController)

    @GetMapping("/ads")
    public ResponseEntity<ApiResponse<Page<AdResponse>>> getAllAdsAdmin(
            @RequestParam(required = false) List<Long> cityId,
            @RequestParam(required = false) List<Integer> type,
            @RequestParam(required = false) List<Integer> subType,
            @RequestParam(required = false) List<Ad.AdStatus> status,
            @RequestParam(required = false) List<String> level,
            @RequestParam(required = false) List<UUID> authorId,
            @RequestParam(required = false) List<Long> rinkId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdResponse> ads = adService.getAdsAdmin(
                cityId, type, subType, status, level, authorId, rinkId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(ads));
    }

    @GetMapping("/ads/statistics")
    @Operation(summary = "Получение статистики по объявлениям",
            description = "Возвращает количество объявлений в каждом статусе.")
    public ResponseEntity<ApiResponse<AdService.AdStatistics>> getAdStatistics() {
        AdService.AdStatistics stats = adService.getAdStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/ads/cleanup")
    @Operation(summary = "Принудительный запуск очистки старых архивных объявлений",
            description = "Удаляет объявления, находившиеся в статусе ARCHIVED более 5 месяцев.")
    public ResponseEntity<ApiResponse<String>> runCleanup() {
        adService.cleanupOldArchivedAds();
        return ResponseEntity.ok(ApiResponse.success("Очистка старых архивных объявлений запущена"));
    }

    @PostMapping("/ads/archive")
    @Operation(summary = "Принудительный запуск архивации просроченных объявлений",
            description = "Переводит объявления с истекшим endTime в статус ARCHIVED.")
    public ResponseEntity<ApiResponse<String>> runArchive() {
        adService.archiveExpiredAds();
        return ResponseEntity.ok(ApiResponse.success("Архивация просроченных объявлений запущена"));
    }

    @GetMapping("/rinks")
    @Operation(summary = "Получить список всех ЛДС (для админского фильтра)")
    public ResponseEntity<ApiResponse<List<RinkResponse>>> getAllRinks() {
        List<RinkResponse> rinks = rinkService.getAllRinks();
        return ResponseEntity.ok(ApiResponse.success(rinks));
    }
}