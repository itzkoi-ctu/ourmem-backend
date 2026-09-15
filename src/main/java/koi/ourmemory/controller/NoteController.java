package koi.ourmemory.controller;

import jakarta.validation.Valid;
import koi.ourmemory.dto.request.CreateNoteRequest;
import koi.ourmemory.dto.response.ApiResponse;
import koi.ourmemory.dto.response.NoteResponse;
import koi.ourmemory.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/photos/{photoId}/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    public ResponseEntity<ApiResponse<NoteResponse>> createNote(
            @PathVariable UUID photoId,
            @Valid @RequestBody CreateNoteRequest request) {
        NoteResponse response = noteService.createNote(photoId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Note created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoteResponse>>> getNotes(@PathVariable UUID photoId) {
        List<NoteResponse> response = noteService.getNotesByPhoto(photoId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @PathVariable UUID photoId,
            @PathVariable UUID noteId) {
        noteService.deleteNote(noteId);
        return ResponseEntity.ok(ApiResponse.success(null, "Note deleted"));
    }
}
