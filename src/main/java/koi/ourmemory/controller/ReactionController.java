package koi.ourmemory.controller;

import jakarta.validation.Valid;
import koi.ourmemory.dto.request.ReactionRequest;
import koi.ourmemory.dto.response.ApiResponse;
import koi.ourmemory.dto.response.ReactionSummary;
import koi.ourmemory.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/photos/{photoId}/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<List<ReactionSummary>>> toggleReaction(
            @PathVariable UUID photoId,
            @Valid @RequestBody ReactionRequest request) {
        List<ReactionSummary> response = reactionService.toggleReaction(photoId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
