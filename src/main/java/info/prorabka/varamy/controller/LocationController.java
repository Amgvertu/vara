package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.response.*;
import info.prorabka.varamy.service.CountryService;
import info.prorabka.varamy.service.RegionService;
import info.prorabka.varamy.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "Локации", description = "Справочники стран, регионов и городов")
public class LocationController {

    private final CountryService countryService;
    private final RegionService regionService;
    private final CityService cityService;

    @GetMapping("/countries")
    @Operation(summary = "Получить список всех стран")
    public ResponseEntity<ApiResponse<List<CountryResponse>>> getAllCountries() {
        List<CountryResponse> countries = countryService.getAllCountries();
        return ResponseEntity.ok(ApiResponse.success(countries));
    }

    @GetMapping("/regions/country/{countryId}")
    @Operation(summary = "Получить список регионов по ID страны")
    public ResponseEntity<ApiResponse<List<RegionResponse>>> getRegionsByCountry(
            @Parameter(description = "ID страны", example = "1", required = true)
            @PathVariable Long countryId) {
        List<RegionResponse> regions = regionService.getRegionsByCountry(countryId);
        return ResponseEntity.ok(ApiResponse.success(regions));
    }

    @GetMapping("/cities/region/{regionId}")
    @Operation(summary = "Получить список городов по ID региона")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getCitiesByRegion(
            @Parameter(description = "ID региона", example = "1", required = true)
            @PathVariable Long regionId) {
        List<CityResponse> cities = cityService.getCitiesByRegion(regionId);
        return ResponseEntity.ok(ApiResponse.success(cities));
    }

    @GetMapping("/cities/simple/region/{regionId}")
    @Operation(summary = "Получить список городов по ID региона (упрощённый формат)")
    public ResponseEntity<ApiResponse<List<CitySimpleResponse>>> getSimpleCitiesByRegion(
            @Parameter(description = "ID региона", example = "26", required = true)
            @PathVariable Long regionId) {
        List<CitySimpleResponse> cities = cityService.getSimpleCitiesByRegion(regionId);
        return ResponseEntity.ok(ApiResponse.success(cities));
    }

    @GetMapping("/countries/simple")
    @Operation(summary = "Получить список всех стран (упрощённый формат)")
    public ResponseEntity<ApiResponse<List<CountrySimpleResponse>>> getSimpleCountries() {
        List<CountrySimpleResponse> countries = countryService.getSimpleCountries();
        return ResponseEntity.ok(ApiResponse.success(countries));
    }

    @GetMapping("/regions/simple/country/{countryId}")
    @Operation(summary = "Получить список регионов по ID страны (упрощённый формат)")
    public ResponseEntity<ApiResponse<List<RegionSimpleResponse>>> getSimpleRegionsByCountry(
            @Parameter(description = "ID страны", example = "1", required = true)
            @PathVariable Long countryId) {
        List<RegionSimpleResponse> regions = regionService.getSimpleRegionsByCountry(countryId);
        return ResponseEntity.ok(ApiResponse.success(regions));
    }

    @GetMapping("/cities/country/{countryId}")
    @Operation(summary = "Получить список всех городов страны (полная информация)")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getCitiesByCountry(
            @Parameter(description = "ID страны", example = "1", required = true)
            @PathVariable Long countryId) {
        List<CityResponse> cities = cityService.getCitiesByCountry(countryId);
        return ResponseEntity.ok(ApiResponse.success(cities));
    }

    @GetMapping("/cities/simple/country/{countryId}")
    @Operation(summary = "Получить список всех городов страны (упрощённый формат)")
    public ResponseEntity<ApiResponse<List<CitySimpleResponse>>> getSimpleCitiesByCountry(
            @Parameter(description = "ID страны", example = "1", required = true)
            @PathVariable Long countryId) {
        List<CitySimpleResponse> cities = cityService.getSimpleCitiesByCountry(countryId);
        return ResponseEntity.ok(ApiResponse.success(cities));
    }

    @GetMapping("/cities/all")
    @Operation(summary = "Получить список всех городов")
    public ResponseEntity<ApiResponse<List<CitySimpleResponse>>> getAllCities() {
        List<CitySimpleResponse> cities = cityService.getAllCitiesSimple();
        return ResponseEntity.ok(ApiResponse.success(cities));
    }
}