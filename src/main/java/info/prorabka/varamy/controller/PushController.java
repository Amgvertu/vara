package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.security.SecurityUser;
import info.prorabka.varamy.service.FcmTokenService;
import info.prorabka.varamy.service.HmsTokenService;
import info.prorabka.varamy.service.RuStoreTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import info.prorabka.varamy.dto.request.PushTokenRequest;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final FcmTokenService fcmTokenService;
    private final HmsTokenService hmsTokenService;
    private final RuStoreTokenService ruStoreTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerToken(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody PushTokenRequest request) {
        String platform = request.getPlatform();
        String token = request.getToken();
        if ("FCM".equalsIgnoreCase(platform)) {
            fcmTokenService.registerToken(currentUser.getId(), token);
        } else if ("HMS".equalsIgnoreCase(platform)) {
            hmsTokenService.registerToken(currentUser.getId(), token);
        } else if ("RUSTORE".equalsIgnoreCase(platform)) {
            ruStoreTokenService.registerToken(currentUser.getId(), token);
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Неизвестная платформа"));
        }
        return ResponseEntity.ok(ApiResponse.success("Токен зарегистрирован", null));
    }

    @DeleteMapping("/unregister")
    public ResponseEntity<ApiResponse<Void>> unregisterToken(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam String token,
            @RequestParam String platform) {
        if ("FCM".equalsIgnoreCase(platform)) {
            fcmTokenService.unregisterToken(currentUser.getId(), token);
        } else if ("HMS".equalsIgnoreCase(platform)) {
            hmsTokenService.unregisterToken(currentUser.getId(), token);
        } else if ("RUSTORE".equalsIgnoreCase(platform)) {
            ruStoreTokenService.unregisterToken(currentUser.getId(), token);
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Неизвестная платформа"));
        }
        return ResponseEntity.ok(ApiResponse.success("Токен удалён", null));
    }
}