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

    private void validateVideoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileUploadException("File is empty");
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new FileUploadException("Video file size exceeds maximum limit of 100MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("video/mp4")) {
            throw new FileUploadException("Only MP4 video files are allowed");
        }
    }
}
