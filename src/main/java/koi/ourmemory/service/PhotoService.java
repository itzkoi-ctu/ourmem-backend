package koi.ourmemory.service;

import koi.ourmemory.dto.request.ReorderPhotosRequest;
import koi.ourmemory.dto.request.UpdatePhotoCaptionRequest;
import koi.ourmemory.dto.response.CloudinaryUploadResult;
import koi.ourmemory.dto.response.PhotoResponse;
import koi.ourmemory.dto.response.ReactionSummary;
import koi.ourmemory.entity.Photo;
import koi.ourmemory.entity.PhotoSession;
import koi.ourmemory.entity.Reaction;
import koi.ourmemory.entity.User;
import koi.ourmemory.exception.FileUploadException;
import koi.ourmemory.exception.ResourceNotFoundException;
import koi.ourmemory.mapper.PhotoMapper;
import koi.ourmemory.repository.PhotoRepository;
import koi.ourmemory.repository.PhotoSessionRepository;
import koi.ourmemory.repository.ReactionRepository;
import koi.ourmemory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final PhotoSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ReactionRepository reactionRepository;
    private final CloudinaryService cloudinaryService;
    private final PhotoMapper photoMapper;
    private final AuthService authService;

    private static final int MAX_PHOTOS_PER_UPLOAD = 20;

    public List<PhotoResponse> uploadPhotos(UUID sessionId, MultipartFile[] files) {
        if (files.length > MAX_PHOTOS_PER_UPLOAD) {
            throw new FileUploadException("Maximum " + MAX_PHOTOS_PER_UPLOAD + " photos per upload");
        }

        PhotoSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        UUID userId = authService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        int currentCount = (int) photoRepository.countBySessionId(sessionId);
        List<PhotoResponse> responses = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            CloudinaryUploadResult uploadResult = cloudinaryService.uploadImage(files[i]);

            Photo photo = Photo.builder()
                    .session(session)
                    .cloudinaryPublicId(uploadResult.getPublicId())
                    .originalUrl(uploadResult.getUrl())
                    .thumbnailUrl(uploadResult.getThumbnailUrl())
                    .sortOrder(currentCount + i)
                    .isPublic(false)
                    .uploadedBy(user)
                    .build();

            Photo saved = photoRepository.save(photo);

            // Set first photo as cover if session has none
            if (session.getCoverPhotoUrl() == null) {
                session.setCoverPhotoUrl(uploadResult.getThumbnailUrl());
                sessionRepository.save(session);
            }

            responses.add(enrichPhotoResponse(saved, userId));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> getPhotosBySession(UUID sessionId) {
        UUID userId = authService.getCurrentUserId();
        List<Photo> photos = photoRepository.findBySessionIdOrderBySortOrderAsc(sessionId);
        return photos.stream().map(p -> enrichPhotoResponse(p, userId)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> getPublicPhotosBySession(UUID sessionId) {
        // A public session means guests can view the full set of photos in that session.
        // Keep photo.isPublic for future fine-grained control, but do not filter it here.
        List<Photo> photos = photoRepository.findBySessionIdOrderBySortOrderAsc(sessionId);
        return photos.stream().map(p -> enrichPhotoResponse(p, null)).collect(Collectors.toList());
    }

    public PhotoResponse updateCaption(UUID photoId, UpdatePhotoCaptionRequest request) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));
        photo.setCaption(request.getCaption());
        Photo updated = photoRepository.save(photo);
        UUID userId = authService.getCurrentUserId();
        return enrichPhotoResponse(updated, userId);
    }

    public void deletePhoto(UUID photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));

        PhotoSession session = photo.getSession();
        boolean wasCover = session.getCoverPhotoUrl() != null
                && session.getCoverPhotoUrl().equals(photo.getThumbnailUrl());

        cloudinaryService.deleteResource(photo.getCloudinaryPublicId(), "image");
        photoRepository.delete(photo);

        if (wasCover) {
            List<Photo> remainingPhotos = photoRepository.findBySessionIdOrderBySortOrderAsc(session.getId());
            Photo nextCover = remainingPhotos.stream()
                    .filter(p -> !p.getId().equals(photoId))
                    .findFirst()
                    .orElse(null);
            session.setCoverPhotoUrl(nextCover != null ? nextCover.getThumbnailUrl() : null);
            sessionRepository.save(session);
        }
    }

    public PhotoResponse togglePublic(UUID photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));
        photo.setIsPublic(!photo.getIsPublic());
        Photo updated = photoRepository.save(photo);
        UUID userId = authService.getCurrentUserId();
        return enrichPhotoResponse(updated, userId);
    }

    public void reorderPhotos(UUID sessionId, ReorderPhotosRequest request) {
        List<UUID> photoIds = request.getPhotoIds();
        for (int i = 0; i < photoIds.size(); i++) {
            Photo photo = photoRepository.findById(photoIds.get(i))
                    .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoIds.get(0)));
            photo.setSortOrder(i);
            photoRepository.save(photo);
        }
    }

    private PhotoResponse enrichPhotoResponse(Photo photo, UUID currentUserId) {
        PhotoResponse response = photoMapper.toResponse(photo);

        // Generate fresh signed URLs for authenticated Cloudinary delivery
        String publicId = photo.getCloudinaryPublicId();
        if (publicId != null && !publicId.isBlank()) {
            response.setOriginalUrl(cloudinaryService.generateSignedImageUrl(publicId, null));
            response.setThumbnailUrl(cloudinaryService.generateSignedImageUrl(publicId,
                    new com.cloudinary.Transformation()
                            .width(400).height(400).crop("fill").gravity("auto").quality("auto")));
        }

        // Build reaction summary
        List<Reaction> reactions = reactionRepository.findByPhotoId(photo.getId());
        Map<String, List<Reaction>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji));

        List<ReactionSummary> summaries = grouped.entrySet().stream()
                .map(entry -> ReactionSummary.builder()
                        .emoji(entry.getKey())
                        .count(entry.getValue().size())
                        .reactedByCurrentUser(currentUserId != null && entry.getValue().stream()
                                .anyMatch(r -> r.getReactedBy().getId().equals(currentUserId)))
                        .build())
                .collect(Collectors.toList());
        response.setReactions(summaries);

        return response;
    }
}
