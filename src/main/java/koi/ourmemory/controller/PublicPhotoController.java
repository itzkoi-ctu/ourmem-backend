package koi.ourmemory.controller;

import koi.ourmemory.dto.response.ApiResponse;
import koi.ourmemory.dto.response.PhotoResponse;
import koi.ourmemory.entity.PhotoSession;
import koi.ourmemory.exception.ResourceNotFoundException;
import koi.ourmemory.repository.PhotoSessionRepository;
import koi.ourmemory.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/sessions/{sessionId}/photos")
@RequiredArgsConstructor
public class PublicPhotoController {

    private final PhotoService photoService;
    private final PhotoSessionRepository sessionRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PhotoResponse>>> getPublicPhotos(
            @PathVariable UUID sessionId) {
        // Verify session is public
        PhotoSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));
        if (!session.getIsPublic()) {
            throw new ResourceNotFoundException("Session", "id", sessionId);
        }

        List<PhotoResponse> response = photoService.getPublicPhotosBySession(sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
