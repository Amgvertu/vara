package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.request.FeedbackRequest;
import info.prorabka.varamy.dto.response.ApiResponse;
import info.prorabka.varamy.security.SecurityUser;
import info.prorabka.varamy.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> sendFeedback(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody FeedbackRequest request) {
        UUID userId = (currentUser != null) ? currentUser.getId() : null;
        feedbackService.sendFeedback(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Сообщение отправлено", null));
    }
}