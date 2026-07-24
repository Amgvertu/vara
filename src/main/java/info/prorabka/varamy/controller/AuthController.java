package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.request.*;
import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.dto.response.AuthResponse;
import info.prorabka.varamy.entity.VerificationCode;
import info.prorabka.varamy.service.AuthService;
import info.prorabka.varamy.service.UserService;
import info.prorabka.varamy.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Регистрация, вход, обновление токенов, сброс пароля")
public class AuthController {

    private final AuthService authService;
    private final VerificationService verificationService;
    private final UserService userService;

    @Value("${sms.mock-mode:true}")
    private boolean mockMode;

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя (без SMS подтверждения)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Неверные данные или телефон уже существует")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Пользователь успешно зарегистрирован", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Успешный вход"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Неверный телефон или пароль")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Вход выполнен успешно", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление access токена")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.refresh(request.getRefreshToken(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Токен обновлён", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход из системы")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Выход выполнен успешно", null));
    }

    // ===================== НАПОМИНАНИЕ ПАРОЛЯ (СБРОС ПАРОЛЯ) =====================

    @PostMapping("/send-password-reset-code")
    @Operation(
            summary = "Отправить код для сброса пароля",
            description = """
            Отправляет SMS с 6-значным кодом подтверждения на указанный номер телефона.
            В режиме разработки (sms.mock-mode=true) код также возвращается в поле data.code.
            """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Код подтверждения отправлен"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Неверный формат телефона или пользователь не найден"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Пользователь с таким телефоном не найден")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> sendPasswordResetCode(
            @Valid @RequestBody SendVerificationCodeRequest request) {

        if (!userService.isPhoneExists(request.getPhone())) {
            throw new info.prorabka.varamy.exception.BadRequestException("Пользователь с таким телефоном не найден");
        }

        String code = verificationService.sendVerificationCode(
                request.getPhone(),
                VerificationCode.VerificationPurpose.PASSWORD_RESET);

        Map<String, String> data = null;
        if (mockMode) {
            data = new HashMap<>();
            data.put("code", code);
        }

        return ResponseEntity.ok(ApiResponse.success("Код подтверждения отправлен", data));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Сброс пароля с подтверждением кода",
            description = """
            Завершает процесс сброса пароля.
            """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Пароль успешно изменён"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Неверный код подтверждения или истёк срок действия"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        verificationService.verifyCode(
                request.getPhone(),
                request.getCode(),
                VerificationCode.VerificationPurpose.PASSWORD_RESET);

        authService.resetPassword(request.getPhone(), request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.success("Пароль успешно изменён", null));
    }

    // ===================== РЕГИСТРАЦИЯ С SMS ПОДТВЕРЖДЕНИЕМ =====================

    @PostMapping("/send-registration-code")
    @Operation(
            summary = "Отправить код подтверждения для регистрации",
            description = """
            Отправляет SMS с 6-значным кодом подтверждения на указанный номер телефона.
            В режиме разработки (sms.mock-mode=true) код также возвращается в поле data.code.
            """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Код подтверждения отправлен"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Неверный формат телефона или телефон уже зарегистрирован")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> sendRegistrationCode(
            @Valid @RequestBody SendVerificationCodeRequest request) {

        String code = verificationService.sendVerificationCode(
                request.getPhone(),
                VerificationCode.VerificationPurpose.REGISTRATION);

        Map<String, String> data = null;
        if (mockMode) {
            data = new HashMap<>();
            data.put("code", code);
        }

        return ResponseEntity.ok(ApiResponse.success("Код подтверждения отправлен", data));
    }

    @PostMapping("/register-with-verification")
    @Operation(
            summary = "Регистрация с подтверждением кода",
            description = """
            Завершает регистрацию пользователя после подтверждения телефона.
            """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Неверный код подтверждения или данные")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> registerWithVerification(
            @Valid @RequestBody RegisterWithVerificationRequest request,
            HttpServletRequest httpRequest) {

        verificationService.verifyCode(
                request.getPhone(),
                request.getCode(),
                VerificationCode.VerificationPurpose.REGISTRATION);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPhone(request.getPhone());
        registerRequest.setPassword(request.getPassword());
        registerRequest.setCountryId(request.getCountryId());
        registerRequest.setRegionId(request.getRegionId());
        registerRequest.setCityId(request.getCityId());

        AuthResponse response = authService.register(registerRequest, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Пользователь успешно зарегистрирован", response));
    }
}