package koi.ourmemory.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import koi.ourmemory.dto.response.CloudinaryUploadResult;
import koi.ourmemory.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
    public static final String VIDEO_TRANSFORM = "ac_aac,c_limit,f_mp4,h_1920,vc_h264,w_1920";
    public static final java.util.Set<String> VIDEO_FORMATS = java.util.Set.of("mp4", "mov", "webm", "mkv", "avi", "m4v");

    public String playbackUrl(String publicId) {
        return cloudinary.url().resourceType("video").type("authenticated").signed(true)
                .transformation(new com.cloudinary.Transformation().rawTransformation(VIDEO_TRANSFORM))
                .generate(publicId);
    }

    public void uploadVideoAsync(MultipartFile file, String publicId, String callback) {
        validateVideoFile(file);
        java.nio.file.Path temporary = null;
        try {
            // The SDK streams a File, avoiding a 100 MB byte[] on the Fly machine heap.
            temporary = java.nio.file.Files.createTempFile("ourmemory-video-", ".upload");
            file.transferTo(temporary);
            cloudinary.uploader().upload(temporary.toFile(), ObjectUtils.asMap(
                    "public_id", publicId, "resource_type", "video", "type", "authenticated",
                    "overwrite", false, "allowed_formats", new java.util.ArrayList<>(VIDEO_FORMATS),
                    "eager", VIDEO_TRANSFORM, "eager_async", true,
                    "eager_notification_url", callback));
        } catch (IOException e) {
            throw new FileUploadException("Video upload failed; the previous video has not been changed", e);
        } finally {
            if (temporary != null) try { java.nio.file.Files.deleteIfExists(temporary); }
            catch (IOException e) { log.warn("Could not remove temporary video file", e); }
        }
    }

    public boolean verifyVideoNotification(String body, String timestamp, String signature) {
        try {
            long issuedAt = Long.parseLong(timestamp);
            long now = java.time.Instant.now().getEpochSecond();
            return issuedAt <= now + 60 && issuedAt >= now - 7200
                    && cloudinary.verifyNotificationSignature(body, timestamp, signature, 7200);
        } catch (RuntimeException e) { return false; }
    }

    public CloudinaryUploadResult uploadImage(MultipartFile file) {
        validateImageFile(file);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "ourmemory/photos",
                            "resource_type", "image",
                            "type", "authenticated"
                    ));

            String publicId = (String) result.get("public_id");

            // Generate signed URLs for authenticated delivery
            String url = generateSignedImageUrl(publicId, null);
            String thumbnailUrl = generateSignedImageUrl(publicId,
                    new com.cloudinary.Transformation()
                            .width(400).height(400).crop("fill").gravity("auto").quality("auto"));

            return CloudinaryUploadResult.builder()
                    .publicId(publicId)
                    .url(url)
                    .thumbnailUrl(thumbnailUrl)
                    .build();
        } catch (IOException e) {
            throw new FileUploadException("Failed to upload image to Cloudinary", e);
        }
    }

    public CloudinaryUploadResult uploadVideo(MultipartFile file) {
        validateVideoFile(file);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "ourmemory/videos",
                            "resource_type", "video",
                            "type", "authenticated"
                    ));

            String publicId = (String) result.get("public_id");

            // Generate signed URLs for authenticated delivery
            String url = generateSignedVideoUrl(publicId, null);
            String thumbnailUrl = generateSignedVideoUrl(publicId,
                    new com.cloudinary.Transformation()
                            .width(800).height(450).crop("fill").gravity("auto").quality("auto")
                            .fetchFormat("jpg"));

            return CloudinaryUploadResult.builder()
                    .publicId(publicId)
                    .url(url)
                    .thumbnailUrl(thumbnailUrl)
                    .build();
        } catch (IOException e) {
            throw new FileUploadException("Failed to upload video to Cloudinary", e);
        }
    }

    /**
     * Generate a signed URL for an authenticated image.
     * @param publicId Cloudinary public ID
     * @param transformation optional transformation, null for original
     */
    public String generateSignedImageUrl(String publicId, com.cloudinary.Transformation transformation) {
        var urlBuilder = cloudinary.url()
                .type("authenticated")
                .signed(true)
                .resourceType("image");
        if (transformation != null) {
            urlBuilder.transformation(transformation);
        }
        return urlBuilder.generate(publicId);
    }

    /**
     * Generate a signed URL for an authenticated video.
     * @param publicId Cloudinary public ID
     * @param transformation optional transformation, null for original
     */
    public String generateSignedVideoUrl(String publicId, com.cloudinary.Transformation transformation) {
        if (transformation == null) return playbackUrl(publicId);
        var urlBuilder = cloudinary.url()
                .type("authenticated")
                .signed(true)
                .resourceType("video");
        if (transformation != null) {
            urlBuilder.transformation(transformation);
        }
        return urlBuilder.generate(publicId);
    }

    public void deleteResource(String publicId, String resourceType) {
        try {
            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", resourceType, "type", "authenticated"));
        } catch (IOException e) {
            log.error("Failed to delete resource from Cloudinary: {}", publicId, e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileUploadException("File is empty");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new FileUploadException("Image file size exceeds maximum limit of 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new FileUploadException("Only image files are allowed");
        }
    }

    public void validateVideoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileUploadException("File is empty");
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new FileUploadException("Video file size exceeds maximum limit of 100MB");
        }
        String name = java.util.Objects.toString(file.getOriginalFilename(), "").toLowerCase(java.util.Locale.ROOT);
        String extension = name.substring(name.lastIndexOf('.') + 1);
        if (!VIDEO_FORMATS.contains(extension)) {
            throw new FileUploadException("Supported videos: MP4, MOV, WebM, MKV, AVI, M4V");
        }
    }
}
