package koi.ourmemory.service;

import koi.ourmemory.dto.request.CreateNoteRequest;
import koi.ourmemory.dto.response.NoteResponse;
import koi.ourmemory.entity.LoveNote;
import koi.ourmemory.entity.Photo;
import koi.ourmemory.entity.User;
import koi.ourmemory.exception.ResourceNotFoundException;
import koi.ourmemory.mapper.NoteMapper;
import koi.ourmemory.repository.LoveNoteRepository;
import koi.ourmemory.repository.PhotoRepository;
import koi.ourmemory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteService {

    private final LoveNoteRepository noteRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final NoteMapper noteMapper;
    private final AuthService authService;

    public NoteResponse createNote(UUID photoId, CreateNoteRequest request) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));

        UUID userId = authService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        LoveNote note = LoveNote.builder()
                .photo(photo)
                .content(request.getContent())
                .writtenBy(user)
                .build();

        LoveNote saved = noteRepository.save(note);
        return noteMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> getNotesByPhoto(UUID photoId) {
        return noteRepository.findByPhotoIdOrderByCreatedAtDesc(photoId)
                .stream().map(noteMapper::toResponse).collect(Collectors.toList());
    }

    public void deleteNote(UUID noteId) {
        LoveNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note", "id", noteId));
        noteRepository.delete(note);
    }
}
