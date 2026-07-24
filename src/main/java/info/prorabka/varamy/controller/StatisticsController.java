package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.response.AdStatsResponse;
import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.dto.response.UserStatsResponse;
import info.prorabka.varamy.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUsersStatistics(
            @RequestParam(required = false) List<Long> cityIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> positions) {

        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to = dateTo != null ? dateTo.atTime(23, 59, 59) : null;

        UserStatsResponse stats = statisticsService.getUserStats(cityIds, from, to, positions);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/ads")
    public ResponseEntity<ApiResponse<AdStatsResponse>> getAdsStatistics(
            @RequestParam(required = false) List<Long> cityIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> statuses) {

        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to = dateTo != null ? dateTo.atTime(23, 59, 59) : null;

        AdStatsResponse stats = statisticsService.getAdStats(cityIds, from, to, statuses);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}