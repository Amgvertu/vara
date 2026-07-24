package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.request.AdminProfileUpdateRequest;
import info.prorabka.varamy.dto.request.ChangeRoleRequest;
import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.dto.response.UserResponse;
import info.prorabka.varamy.entity.User;
import info.prorabka.varamy.exception.BadRequestException;
import info.prorabka.varamy.security.SecurityUser;
import info.prorabka.varamy.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Администрирование пользователей", description = "Управление пользователями для администраторов")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Получить список пользователей с расширенными фильтрами (админ)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) List<String> role,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<Long> cityId,
            @RequestParam(required = false) List<String> team,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserResponse> page = userService.getUsersForAdmin(search, role, status, cityId, team, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID (админ)")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable UUID id) {
        UserResponse response = userService.getUserResponseById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Обновить профиль пользователя (админ)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(
            @PathVariable UUID id,
            @Valid @RequestBody AdminProfileUpdateRequest request) {
        UserResponse response = userService.updateUserProfileByAdmin(id, request);
        return ResponseEntity.ok(ApiResponse.success("Профиль пользователя обновлён", response));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request) {
        User.UserRole role = User.UserRole.valueOf(request.getRole().toUpperCase());
        UserResponse response = userService.updateUser(id, null, role, null, null);
        return ResponseEntity.ok(ApiResponse.success("Роль изменена", response));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Изменить статус пользователя (блокировка/разблокировка)")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @PathVariable UUID id,
            @RequestParam User.UserStatus status) {
        UserResponse response = userService.updateUser(id, null, null, status, null);
        return ResponseEntity.ok(ApiResponse.success("Статус изменён", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id, true); // всегда жёстко
        return ResponseEntity.ok(ApiResponse.success("Пользователь удалён", null));
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable UUID id) {
        userService.updateUser(id, null, null, User.UserStatus.BLOCKED, null);
        return ResponseEntity.ok(ApiResponse.success("Пользователь заблокирован", null));
    }

    @GetMapping("/teams")
    @Operation(summary = "Получить список уникальных названий команд")
    public ResponseEntity<ApiResponse<List<String>>> getTeams() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllTeams()));
    }
}