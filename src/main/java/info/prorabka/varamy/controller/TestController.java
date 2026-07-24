package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Тестовый контроллер", description = "Методы для проверки работоспособности API")
public class TestController {

    @GetMapping("/ping")
    @Operation(summary = "Проверка доступности сервера", description = "Возвращает 'pong'")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.success("pong"));
    }
}