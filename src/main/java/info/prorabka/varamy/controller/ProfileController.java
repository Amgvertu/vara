package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.request.ProfileRequest;
import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.dto.response.ProfileResponse;
import info.prorabka.varamy.security.SecurityUser;
import info.prorabka.varamy.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Профили", description = "Управление профилями пользователей")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}")
    @Operation(summary = "Получение публичного профиля пользователя")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@PathVariable UUID userId) {
        ProfileResponse response = profileService.getProfileById(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Получение своего профиля для редактирования")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal SecurityUser currentUser) {
        ProfileResponse response = profileService.getProfileById(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Обновление своего профиля")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody ProfileRequest request) {
        ProfileResponse response = profileService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Профиль обновлён", response));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Загрузка аватара")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam(name = "file") MultipartFile file) {
        String avatarUrl = profileService.uploadAvatar(currentUser.getId(), file);
        return ResponseEntity.ok(ApiResponse.success("Аватар загружен", avatarUrl));
    }

    @DeleteMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Удаление аватара текущего пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Аватар удалён"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Не авторизован"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Аватар не найден")
    })
    public ResponseEntity<ApiResponse<Void>> deleteAvatar(
            @AuthenticationPrincipal SecurityUser currentUser) {
        profileService.deleteAvatar(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Аватар удалён", null));
    }
}
