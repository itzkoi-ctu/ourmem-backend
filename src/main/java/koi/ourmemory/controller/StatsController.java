package koi.ourmemory.controller;

import koi.ourmemory.dto.response.ApiResponse;
import koi.ourmemory.dto.response.StatsResponse;
import koi.ourmemory.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public ResponseEntity<ApiResponse<StatsResponse>> getStats() {
        StatsResponse response = statsService.getStats();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
