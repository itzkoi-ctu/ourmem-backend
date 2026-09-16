package koi.ourmemory.controller;

import jakarta.validation.Valid;
import koi.ourmemory.dto.request.CreateSessionRequest;
import koi.ourmemory.dto.request.UpdateSessionRequest;
import koi.ourmemory.dto.response.ApiResponse;
import koi.ourmemory.dto.response.PageResponse;
import koi.ourmemory.dto.response.SessionResponse;
import koi.ourmemory.service.SessionService;
import koi.ourmemory.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final VideoService videoService;
    private final koi.ourmemory.service.VideoProcessingService videoProcessingService;

    @PostMapping
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @Valid @RequestBody CreateSessionRequest request) {
        SessionResponse response = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Session created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> updateSession(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSessionRequest request) {
        SessionResponse response = sessionService.updateSession(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Session updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable UUID id) {
        sessionService.deleteSession(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Session deleted"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(@PathVariable UUID id) {
        SessionResponse response = sessionService.getSessionById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SessionResponse>>> getAllSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<SessionResponse> response = sessionService.getAllSessions(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/on-this-day")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getOnThisDay() {
        List<SessionResponse> response = sessionService.getOnThisDay();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/random")
    public ResponseEntity<ApiResponse<SessionResponse>> getRandomSession() {
        SessionResponse response = sessionService.getRandomSession();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/toggle-public")
    public ResponseEntity<ApiResponse<SessionResponse>> togglePublic(@PathVariable UUID id) {
        SessionResponse response = sessionService.togglePublic(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Visibility toggled"));
    }

    @PatchMapping("/{id}/cover/{photoId}")
    public ResponseEntity<ApiResponse<SessionResponse>> setCoverPhoto(
            @PathVariable UUID id,
            @PathVariable UUID photoId) {
        SessionResponse response = sessionService.setCoverPhoto(id, photoId);
        return ResponseEntity.ok(ApiResponse.success(response, "Cover photo updated"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> searchSessions(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String moodTag,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<SessionResponse> response = sessionService.searchSessions(location, moodTag, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Video timelapse endpoints
    @PostMapping("/{id}/video")
    public ResponseEntity<ApiResponse<koi.ourmemory.entity.VideoUpload>> uploadTimelapse(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        var response = videoProcessingService.start(id, file);
        return ResponseEntity.accepted().body(ApiResponse.success(response, "Video is processing"));
    }

    @GetMapping("/{id}/video/status")
    public ResponseEntity<ApiResponse<koi.ourmemory.entity.VideoUpload>> videoStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(videoProcessingService.latest(id)));
    }

    @DeleteMapping("/{id}/video")
    public ResponseEntity<ApiResponse<SessionResponse>> deleteTimelapse(@PathVariable UUID id) {
        videoProcessingService.deleteVideo(id);
        SessionResponse response = sessionService.getSessionById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Timelapse deleted"));
    }
}
