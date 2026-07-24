package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.request.AdminMessageRequest;
import info.prorabka.varamy.dto.response.*;
import info.prorabka.varamy.entity.User;
import info.prorabka.varamy.security.SecurityUser;
import info.prorabka.varamy.service.AdminMessageService;
import info.prorabka.varamy.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
@Tag(name = "Административные сообщения", description = "Отправка и управление сообщениями")
public class AdminMessageController {

    private final AdminMessageService adminMessageService;
    private final UserService userService;

    // 2.1 Загрузка изображения
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить изображение для сообщения")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        String imageUrl = adminMessageService.uploadImage(file);
        Map<String, String> data = new HashMap<>();
        data.put("imageUrl", imageUrl);
        return ResponseEntity.ok(ApiResponse.success("Изображение загружено", data));
    }

    // 2.2 Отправка сообщения
    @PostMapping
    @Operation(summary = "Отправить сообщение пользователям")
    public ResponseEntity<ApiResponse<AdminMessageResponse>> sendMessage(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody AdminMessageRequest request) {
        AdminMessageResponse response = adminMessageService.sendMessage(request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Сообщение отправлено", response));
    }

    // 2.3 Получение списка команд (для мультивыбора)
    @GetMapping("/teams")
    @Operation(summary = "Получить список уникальных названий команд")
    public ResponseEntity<ApiResponse<List<String>>> getTeams() {
        List<String> teams = userService.getAllTeams();
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    // 2.4 Поиск пользователей (для индивидуального выбора)
    @GetMapping("/users/search")
    @Operation(summary = "Поиск пользователей по ID, телефону, имени или фамилии")
    public ResponseEntity<ApiResponse<Page<UserSearchResponse>>> searchUsers(
            @RequestParam(name = "query", required = false) String query,
            @PageableDefault(size = 10) Pageable pageable) {
        // Ограничим размер страницы максимум 20
        if (pageable.getPageSize() > 20) {
            pageable = PageRequest.of(pageable.getPageNumber(), 20, pageable.getSort());
        }
        Page<User> userPage = userService.searchUsers(query, pageable);
        Page<UserSearchResponse> responsePage = userPage.map(user -> {
            var profile = user.getProfile();
            return UserSearchResponse.builder()
                    .id(user.getId())
                    .phone(user.getPhone())
                    .profile(ProfileSimpleResponse.builder()
                            .firstName(profile != null ? profile.getFirstName() : null)
                            .lastName(profile != null ? profile.getLastName() : null)
                            .team(profile != null ? profile.getTeam() : null)
                            .homeCity(profile != null && profile.getHomeCity() != null ?
                                    new CitySimpleResponse(profile.getHomeCity().getId(), profile.getHomeCity().getName()) :
                                    null)
                            .build())
                    .build();
        });
        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }

    // 2.5 Получение истории отправленных сообщений (админ)
    @GetMapping
    @Operation(summary = "Получить историю отправленных сообщений (для текущего администратора/модератора)")
    public ResponseEntity<ApiResponse<Page<AdminMessageResponse>>> getSentMessages(
            @AuthenticationPrincipal SecurityUser currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdminMessageResponse> page = adminMessageService.getSentMessages(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }
}