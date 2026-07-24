package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.dto.response.RinkResponse;
import info.prorabka.varamy.service.RinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rinks")
@RequiredArgsConstructor
@Tag(name = "Ледовые дворцы (ЛДС)", description = "Управление информацией о катках")
public class RinkController {

    private final RinkService rinkService;

    @GetMapping("/city/{cityId}")
    @Operation(summary = "Получить список ЛДС по ID города")
    public ResponseEntity<ApiResponse<List<RinkResponse>>> getRinksByCity(
            @Parameter(description = "ID города", example = "129", required = true)
            @PathVariable Long cityId) {
        List<RinkResponse> rinks = rinkService.getRinksByCity(cityId);
        return ResponseEntity.ok(ApiResponse.success(rinks));
    }

    @GetMapping("/{rinkId}")
    @Operation(summary = "Получить информацию о ЛДС по ID")
    public ResponseEntity<ApiResponse<RinkResponse>> getRinkById(
            @Parameter(description = "ID ЛДС", example = "36", required = true)
            @PathVariable Long rinkId) {
        RinkResponse rink = rinkService.getRinkById(rinkId);
        return ResponseEntity.ok(ApiResponse.success(rink));
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск ЛДС по названию или адресу")
    public ResponseEntity<ApiResponse<List<RinkResponse>>> searchRinks(
            @Parameter(description = "Поисковый запрос (название или адрес)", example = "Сибирь")
            @RequestParam(required = false) String query,
            @Parameter(description = "ID города для фильтрации", example = "129")
            @RequestParam(required = false) Long cityId) {
        List<RinkResponse> rinks = rinkService.searchRinks(query, cityId);
        return ResponseEntity.ok(ApiResponse.success(rinks));
    }
}