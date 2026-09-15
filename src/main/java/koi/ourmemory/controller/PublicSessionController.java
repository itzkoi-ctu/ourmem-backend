package koi.ourmemory.controller;

import koi.ourmemory.dto.response.ApiResponse;
import koi.ourmemory.dto.response.CountdownResponse;
import koi.ourmemory.dto.response.PageResponse;
import koi.ourmemory.dto.response.SessionResponse;
import koi.ourmemory.service.CoupleConfigService;
import koi.ourmemory.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicSessionController {

    private final SessionService sessionService;
    private final CoupleConfigService configService;

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<PageResponse<SessionResponse>>> getPublicSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<SessionResponse> response = sessionService.getPublicSessions(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> getPublicSession(@PathVariable UUID id) {
        SessionResponse response = sessionService.getPublicSessionById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sessions/on-this-day")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getPublicOnThisDay() {
        List<SessionResponse> response = sessionService.getPublicOnThisDay();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sessions/random")
    public ResponseEntity<ApiResponse<SessionResponse>> getRandomPublicSession() {
        SessionResponse response = sessionService.getRandomPublicSession();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/countdown")
    public ResponseEntity<ApiResponse<CountdownResponse>> getCountdown() {
        CountdownResponse response = configService.getCountdown();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
