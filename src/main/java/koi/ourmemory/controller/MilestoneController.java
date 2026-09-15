package koi.ourmemory.controller;

import jakarta.validation.Valid;
import koi.ourmemory.dto.request.CreateMilestoneRequest;
import koi.ourmemory.dto.response.ApiResponse;
import koi.ourmemory.dto.response.MilestoneResponse;
import koi.ourmemory.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
public class MilestoneController {

    private final MilestoneService milestoneService;

    @PostMapping
    public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(
            @Valid @RequestBody CreateMilestoneRequest request) {
        MilestoneResponse response = milestoneService.createMilestone(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Milestone created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getAllMilestones() {
        List<MilestoneResponse> response = milestoneService.getAllMilestones();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getUpcoming() {
        List<MilestoneResponse> response = milestoneService.getUpcomingMilestones();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(@PathVariable UUID id) {
        milestoneService.deleteMilestone(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Milestone deleted"));
    }
}
