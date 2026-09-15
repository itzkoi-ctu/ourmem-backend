package koi.ourmemory.service;

import koi.ourmemory.dto.request.CreateSessionRequest;
import koi.ourmemory.dto.request.UpdateSessionRequest;
import koi.ourmemory.dto.response.PageResponse;
import koi.ourmemory.dto.response.SessionResponse;
import koi.ourmemory.entity.Photo;
import koi.ourmemory.entity.PhotoSession;
import koi.ourmemory.entity.User;
import koi.ourmemory.exception.ResourceNotFoundException;
import koi.ourmemory.mapper.SessionMapper;
import koi.ourmemory.repository.PhotoRepository;
import koi.ourmemory.repository.PhotoSessionRepository;
import koi.ourmemory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final PhotoSessionRepository sessionRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final SessionMapper sessionMapper;
    private final AuthService authService;
    private final CloudinaryService cloudinaryService;

    // Pattern to extract publicId from stored Cloudinary URLs
    // e.g. .../ourmemory/photos/abc123 -> ourmemory/photos/abc123
    private static final Pattern PUBLIC_ID_PATTERN = Pattern.compile("/v\\d+/(.+?)(?:\\.[a-zA-Z]+)?$");

    public SessionResponse createSession(CreateSessionRequest request) {
        UUID userId = authService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        PhotoSession session = sessionMapper.toEntity(request);
        session.setCreatedBy(user);
        session.setIsPublic(false);

        PhotoSession saved = sessionRepository.save(session);
        return toSignedResponse(saved);
    }

    public SessionResponse updateSession(UUID sessionId, UpdateSessionRequest request) {
        PhotoSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        if (request.getTitle() != null) session.setTitle(request.getTitle());
        if (request.getSessionDate() != null) session.setSessionDate(request.getSessionDate());
        if (request.getLocation() != null) session.setLocation(request.getLocation());
        if (request.getMoodTag() != null) session.setMoodTag(request.getMoodTag());
        if (request.getSpotifyLink() != null) session.setSpotifyLink(request.getSpotifyLink());
        if (request.getDescription() != null) session.setDescription(request.getDescription());

        PhotoSession updated = sessionRepository.save(session);
        return toSignedResponse(updated);
    }

    public void deleteSession(UUID sessionId) {
        PhotoSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));
        sessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSessionById(UUID sessionId) {
        PhotoSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));
        return toSignedResponse(session);
    }

    @Transactional(readOnly = true)
    public PageResponse<SessionResponse> getAllSessions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PhotoSession> sessions = sessionRepository.findAllByOrderBySessionDateDesc(pageable);
        return toPageResponse(sessions);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getOnThisDay() {
        LocalDate today = LocalDate.now();
        return sessionRepository.findOnThisDay(today.getMonthValue(), today.getDayOfMonth())
                .stream().map(this::toSignedResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SessionResponse getRandomSession() {
        PhotoSession session = sessionRepository.findRandomSession();
        if (session == null) {
            throw new ResourceNotFoundException("No sessions found");
        }
        return toSignedResponse(session);
    }

    public SessionResponse togglePublic(UUID sessionId) {
        PhotoSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));
        session.setIsPublic(!session.getIsPublic());
        PhotoSession updated = sessionRepository.save(session);
        return toSignedResponse(updated);
    }

    public SessionResponse setCoverPhoto(UUID sessionId, UUID photoId) {
        PhotoSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        Photo photo = photoRepository.findByIdAndSessionId(photoId, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));

        session.setCoverPhotoUrl(photo.getThumbnailUrl());
        PhotoSession updated = sessionRepository.save(session);
        return toSignedResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> searchSessions(String location, String moodTag, LocalDate startDate, LocalDate endDate) {
        if (location != null && !location.isBlank()) {
            return sessionRepository.findByLocationContainingIgnoreCase(location)
                    .stream().map(this::toSignedResponse).collect(Collectors.toList());
        }
        if (moodTag != null && !moodTag.isBlank()) {
            return sessionRepository.findByMoodTagContainingIgnoreCase(moodTag)
                    .stream().map(this::toSignedResponse).collect(Collectors.toList());
        }
        if (startDate != null && endDate != null) {
            return sessionRepository.findBySessionDateBetween(startDate, endDate)
                    .stream().map(this::toSignedResponse).collect(Collectors.toList());
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public PageResponse<SessionResponse> getPublicSessions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PhotoSession> sessions = sessionRepository.findByIsPublicTrueOrderBySessionDateDesc(pageable);
        return toPageResponse(sessions);
    }

    @Transactional(readOnly = true)
    public SessionResponse getPublicSessionById(UUID id) {
        PhotoSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", id));
        if (!session.getIsPublic()) {
            throw new ResourceNotFoundException("Session", "id", id);
        }
        return toSignedResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getPublicOnThisDay() {
        LocalDate today = LocalDate.now();
        return sessionRepository.findPublicOnThisDay(today.getMonthValue(), today.getDayOfMonth())
                .stream().map(this::toSignedResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SessionResponse getRandomPublicSession() {
        PhotoSession session = sessionRepository.findRandomPublicSession();
        if (session == null) {
            throw new ResourceNotFoundException("No public sessions found");
        }
        return toSignedResponse(session);
    }

    private PageResponse<SessionResponse> toPageResponse(Page<PhotoSession> page) {
        List<SessionResponse> content = page.getContent().stream()
                .map(this::toSignedResponse).collect(Collectors.toList());
        return PageResponse.<SessionResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    /**
     * Convert entity to response and sign all Cloudinary URLs.
     */
    private SessionResponse toSignedResponse(PhotoSession session) {
        SessionResponse response = sessionMapper.toResponse(session);
        signSessionUrls(response, session);
        return response;
    }

    /**
     * Replace stored Cloudinary URLs with fresh signed URLs for authenticated delivery.
     */
    private void signSessionUrls(SessionResponse response, PhotoSession session) {
        // Sign cover photo URL using the first photo's publicId if available
        if (response.getCoverPhotoUrl() != null) {
            String publicId = extractPublicId(response.getCoverPhotoUrl());
            if (publicId != null) {
                response.setCoverPhotoUrl(cloudinaryService.generateSignedImageUrl(publicId,
                        new com.cloudinary.Transformation()
                                .width(400).height(400).crop("fill").gravity("auto").quality("auto")));
            }
        }

        // Sign video URL
        if (session.getVideoPublicId() != null) {
            response.setVideoUrl(cloudinaryService.generateSignedVideoUrl(session.getVideoPublicId(), null));
            response.setVideoThumbnailUrl(cloudinaryService.generateSignedVideoUrl(session.getVideoPublicId(),
                    new com.cloudinary.Transformation()
                            .width(800).height(450).crop("fill").gravity("auto").quality("auto")
                            .fetchFormat("jpg")));
        }
    }

    /**
     * Extract Cloudinary public ID from a stored URL.
     */
    private String extractPublicId(String url) {
        if (url == null) return null;
        Matcher m = PUBLIC_ID_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }
}
