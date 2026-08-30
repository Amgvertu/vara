package info.prorabka.varamy.controller;

import info.prorabka.varamy.security.SecurityUser;
import info.prorabka.varamy.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/user/status")
@RequiredArgsConstructor
public class UserStatusController {
    private final UserActivityService userActivityService;

    @PostMapping
    public ResponseEntity<Void> setStatus(@AuthenticationPrincipal SecurityUser user,
                                          @RequestParam boolean active) {
        log.info("📨 Получен запрос на смену статуса: userId={}, active={}", user.getId(), active);
        userActivityService.setActive(user.getId(), active);
        return ResponseEntity.ok().build();
    }
}