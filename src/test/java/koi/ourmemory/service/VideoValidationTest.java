package koi.ourmemory.service;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class VideoValidationTest {
    private final CloudinaryService service = new CloudinaryService(new Cloudinary(Map.of(
            "cloud_name", "test", "api_key", "test", "api_secret", "test-secret", "secure", true)));

    @Test void browserMissingMimeDoesNotRejectSupportedFiles() {
        for (String extension : CloudinaryService.VIDEO_FORMATS) {
            assertDoesNotThrow(() -> service.validateVideoFile(new MockMultipartFile("file", "clip." + extension.toUpperCase(), "application/octet-stream", new byte[]{1})));
        }
    }
    @Test void invalidOrEmptyFileIsRejected() {
        assertThrows(RuntimeException.class, () -> service.validateVideoFile(new MockMultipartFile("file", "fake.exe", "video/mp4", new byte[]{1})));
        assertThrows(RuntimeException.class, () -> service.validateVideoFile(new MockMultipartFile("file", "empty.mp4", "video/mp4", new byte[]{})));
    }
    @Test void playbackUsesExplicitCodecAndFormat() {
        String url = service.playbackUrl("ourmemory/videos/test");
        assertTrue(url.contains("f_mp4")); assertTrue(url.contains("vc_h264")); assertTrue(url.contains("ac_aac"));
        assertTrue(url.contains("/authenticated/s--"));
    }
    @Test void validSignatureAcceptedAndTamperingRejected() throws Exception {
        String body = "{\"notification_type\":\"eager\"}";
        String timestamp = Long.toString(java.time.Instant.now().getEpochSecond());
        String signature = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                .digest((body + timestamp + "test-secret").getBytes(StandardCharsets.UTF_8)));
        assertTrue(service.verifyVideoNotification(body, timestamp, signature));
        assertFalse(service.verifyVideoNotification(body + " ", timestamp, signature));
        assertFalse(service.verifyVideoNotification(body, "0", signature));
        assertFalse(service.verifyVideoNotification(body, "invalid", signature));
    }
}
