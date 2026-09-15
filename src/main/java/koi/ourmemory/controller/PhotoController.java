package koi.ourmemory.controller;

import jakarta.validation.Valid;
import koi.ourmemory.dto.request.ReorderPhotosRequest;
import koi.ourmemory.dto.request.UpdatePhotoCaptionRequest;
import koi.ourmemory.dto.response.ApiResponse;
import koi.ourmemory.dto.response.PhotoResponse;
import koi.ourmemory.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping
    public ResponseEntity<ApiResponse<List<PhotoResponse>>> uploadPhotos(
            @PathVariable UUID sessionId,
            @RequestParam("files") MultipartFile[] files) {
        List<PhotoResponse> response = photoService.uploadPhotos(sessionId, files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Photos uploaded"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PhotoResponse>>> getPhotos(@PathVariable UUID sessionId) {
        List<PhotoResponse> response = photoService.getPhotosBySession(sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{photoId}/caption")
    public ResponseEntity<ApiResponse<PhotoResponse>> updateCaption(
            @PathVariable UUID sessionId,
            @PathVariable UUID photoId,
            @Valid @RequestBody UpdatePhotoCaptionRequest request) {
        PhotoResponse response = photoService.updateCaption(photoId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Caption updated"));
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            @PathVariable UUID sessionId,
            @PathVariable UUID photoId) {
        photoService.deletePhoto(photoId);
        return ResponseEntity.ok(ApiResponse.success(null, "Photo deleted"));
    }

    @PatchMapping("/{photoId}/toggle-public")
    public ResponseEntity<ApiResponse<PhotoResponse>> togglePublic(
            @PathVariable UUID sessionId,
            @PathVariable UUID photoId) {
        PhotoResponse response = photoService.togglePublic(photoId);
        return ResponseEntity.ok(ApiResponse.success(response, "Visibility toggled"));
    }

    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderPhotos(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ReorderPhotosRequest request) {
        photoService.reorderPhotos(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Photos reordered"));
    }
}
