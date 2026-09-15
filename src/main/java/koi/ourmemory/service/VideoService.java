package koi.ourmemory.service;

import koi.ourmemory.dto.response.CloudinaryUploadResult;
import koi.ourmemory.dto.response.SessionResponse;
import koi.ourmemory.entity.PhotoSession;
import koi.ourmemory.exception.ResourceNotFoundException;
import koi.ourmemory.mapper.SessionMapper;
import koi.ourmemory.repository.PhotoSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VideoService {

    private final PhotoSessionRepository sessionRepository;
    private final CloudinaryService cloudinaryService;
    private final SessionMapper sessionMapper;

    public SessionResponse uploadTimelapse(UUID sessionId, MultipartFile file) {
        PhotoSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        // Delete existing video if any
        if (session.getVideoPublicId() != null) {
            cloudinaryService.deleteResource(session.getVideoPublicId(), "video");
        }

        CloudinaryUploadResult result = cloudinaryService.uploadVideo(file);
        session.setVideoUrl(result.getUrl());
        session.setVideoThumbnailUrl(result.getThumbnailUrl());
        session.setVideoPublicId(result.getPublicId());

        PhotoSession saved = sessionRepository.save(session);
        return signSessionResponse(saved);
    }

    public SessionResponse deleteTimelapse(UUID sessionId) {
        PhotoSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        if (session.getVideoPublicId() != null) {
            cloudinaryService.deleteResource(session.getVideoPublicId(), "video");
        }

        session.setVideoUrl(null);
        session.setVideoThumbnailUrl(null);
        session.setVideoPublicId(null);

        PhotoSession saved = sessionRepository.save(session);
        return signSessionResponse(saved);
    }

    private SessionResponse signSessionResponse(PhotoSession session) {
        SessionResponse response = sessionMapper.toResponse(session);

        // Sign cover photo URL
        if (response.getCoverPhotoUrl() != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("/v\\d+/(.+?)(?:\\.[a-zA-Z]+)?$")
                    .matcher(response.getCoverPhotoUrl());
            if (m.find()) {
                response.setCoverPhotoUrl(cloudinaryService.generateSignedImageUrl(m.group(1),
                        new com.cloudinary.Transformation()
                                .width(400).height(400).crop("fill").gravity("auto").quality("auto")));
            }
        }

        // Sign video URLs
        if (session.getVideoPublicId() != null) {
            response.setVideoUrl(cloudinaryService.generateSignedVideoUrl(session.getVideoPublicId(), null));
            response.setVideoThumbnailUrl(cloudinaryService.generateSignedVideoUrl(session.getVideoPublicId(),
                    new com.cloudinary.Transformation()
                            .width(800).height(450).crop("fill").gravity("auto").quality("auto")
                            .fetchFormat("jpg")));
        }

        return response;
    }
}
