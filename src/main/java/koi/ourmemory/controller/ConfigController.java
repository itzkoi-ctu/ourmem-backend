package koi.ourmemory.controller;

import jakarta.validation.Valid;
import koi.ourmemory.dto.request.UpdateConfigRequest;
import koi.ourmemory.dto.response.ApiResponse;
import koi.ourmemory.dto.response.CoupleConfigResponse;
import koi.ourmemory.dto.response.CountdownResponse;
import koi.ourmemory.service.CoupleConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final CoupleConfigService configService;

    @GetMapping("/countdown")
    public ResponseEntity<ApiResponse<CountdownResponse>> getCountdown() {
        CountdownResponse response = configService.getCountdown();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CoupleConfigResponse>>> getAllConfigs() {
        List<CoupleConfigResponse> response = configService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<CoupleConfigResponse>> updateConfig(
            @PathVariable String key,
            @Valid @RequestBody UpdateConfigRequest request) {
        CoupleConfigResponse response = configService.updateConfig(key, request.getConfigValue());
        return ResponseEntity.ok(ApiResponse.success(response, "Config updated"));
    }
}
