package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.request.AdFilterRequest;
import info.prorabka.varamy.dto.request.AdRequest;
import info.prorabka.varamy.dto.response.AdResponse;
import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.dto.response.DuplicateAdResponse;
import info.prorabka.varamy.entity.Ad;
import info.prorabka.varamy.security.SecurityUser;
import info.prorabka.varamy.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
@Tag(name = "Объявления", description = "Управление объявлениями")
public class AdController {

    private final AdService adService;

    @GetMapping
    @Operation(summary = "Получение списка объявлений по городу",
            description = "Возвращает активные (ACTIVE) и заполненные (FILLED) объявления для указанного города. " +
                    "Можно фильтровать по типу, подтипу, уровню.")
    public ResponseEntity<ApiResponse<Page<AdResponse>>> getAds(
            @Parameter(description = "ID города", required = true, example = "57")
            @RequestParam(name = "cityId") Long cityId,
            @Parameter(description = "Тип объявления (1-5)", example = "1")
            @RequestParam(name = "type", required = false) Integer type,
            @Parameter(description = "Подтип объявления", example = "1")
            @RequestParam(name = "subType", required = false) Integer subType,
            @Parameter(description = "Уровни игроков (список строк A-H)", example = "[\"A\",\"B\"]")
            @RequestParam(name = "level", required = false) List<String> level,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AdResponse> ads = adService.getAds(cityId, type, subType, level, pageable);
        return ResponseEntity.ok(ApiResponse.success(ads));
    }

    @GetMapping("/all")
    @Operation(summary = "Получение всех активных объявлений (без фильтрации по городу)",
            description = "Возвращает все объявления со статусами ACTIVE и FILLED, без привязки к городу.")
    public ResponseEntity<ApiResponse<Page<AdResponse>>> getAllActiveAds(
            @Parameter(description = "Тип объявления (1-5)", example = "1")
            @RequestParam(name = "type", required = false) Integer type,
            @Parameter(description = "Подтип объявления", example = "1")
            @RequestParam(name = "subType", required = false) Integer subType,
            @Parameter(description = "Уровни игроков (список строк A-H)", example = "[\"A\",\"B\"]")
            @RequestParam(name = "level", required = false) List<String> level,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdResponse> ads = adService.getAllActiveAds(type, subType, level, pageable);
        return ResponseEntity.ok(ApiResponse.success(ads));
    }

    // ===================== ПРОВЕРКА НА ДУБЛИРОВАНИЕ =====================

    @PostMapping("/check-duplicate")
    @Operation(
            summary = "Проверка объявления на дублирование",
            description = """
            Проверяет, существует ли уже объявление с такими же параметрами.
            
            **Для каких типов выполняется проверка:**
            - Тип 1.1 (Поиск вратаря) – проверка по времени (±30 мин), ЛДС, городу
            - Тип 1.2 (Поиск полевых) – проверка по времени (±30 мин), ЛДС, городу
            - Тип 3.2 (Предлагаю матч) – проверка по времени (±30 мин), ЛДС, городу
            - Тип 4.1-4.4 (Специалисты) – проверка по времени (±30 мин), городу
            
            **Параметры проверки:**
            - Тип и подтип объявления
            - Город
            - Время начала ± 30 минут
            - ЛДС (если указан, проверяется совпадение хотя бы одного)
            
            **Результат:**
            - Если дубликатов нет – возвращается пустой список
            - Если дубликаты найдены – возвращается список с информацией о них
            
            **Рекомендация:** Вызывать этот метод перед созданием объявления,
            чтобы предупредить пользователя о возможном дублировании.
            """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Проверка выполнена"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Неверные параметры запроса")
    })
    public ResponseEntity<ApiResponse<List<DuplicateAdResponse>>> checkDuplicate(
            @Valid @RequestBody AdRequest request) {

        List<DuplicateAdResponse> duplicates = adService.checkDuplicate(request);

        if (duplicates.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Дубликатов не найдено", duplicates));
        } else {
            return ResponseEntity.ok(ApiResponse.success(
                    "Найдено " + duplicates.size() + " похожих объявлений", duplicates));
        }
    }

    @PostMapping
    @Operation(summary = "Создание нового объявления")
    public ResponseEntity<ApiResponse<AdResponse>> createAd(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody AdRequest request) {
        AdResponse response = adService.createAd(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Объявление создано", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получение объявления по ID")
    public ResponseEntity<ApiResponse<AdResponse>> getAdById(
            @Parameter(description = "UUID объявления", example = "ae286c07-5abf-4f05-be48-7835b8629909")
            @PathVariable UUID id) {
        AdResponse response = adService.getAdById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Редактирование объявления")
    public ResponseEntity<ApiResponse<AdResponse>> updateAd(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "UUID объявления", example = "ae286c07-5abf-4f05-be48-7835b8629909")
            @PathVariable UUID id,
            @Valid @RequestBody AdRequest request) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        AdResponse response = adService.updateAd(id, currentUser.getId(), request, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Объявление обновлено", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удаление объявления")
    public ResponseEntity<ApiResponse<Void>> deleteAd(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "UUID объявления", example = "ae286c07-5abf-4f05-be48-7835b8629909")
            @PathVariable UUID id) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        adService.deleteAd(id, currentUser.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Объявление удалено", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Получение списка своих объявлений",
            description = "Возвращает все объявления автора, включая архивные и заполненные.")
    public ResponseEntity<ApiResponse<Page<AdResponse>>> getMyAds(
            @AuthenticationPrincipal SecurityUser currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdResponse> ads = adService.getMyAds(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(ads));
    }

    @GetMapping("/filter")
    @Operation(summary = "Расширенная фильтрация объявлений",
            description = "Возвращает объявления со статусами ACTIVE и FILLED, поддерживая фильтры по городу, типу, дате, времени, ЛДС.")
    public ResponseEntity<ApiResponse<Page<AdResponse>>> getFilteredAds(
            @Parameter(description = "Фильтры", required = true)
            @ModelAttribute AdFilterRequest filterRequest,   // используем @ModelAttribute для query параметров
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdResponse> ads = adService.getFilteredAds(filterRequest, pageable);
        return ResponseEntity.ok(ApiResponse.success(ads));
    }
}