package koi.ourmemory.controller;

import koi.ourmemory.service.CloudinaryService;
import koi.ourmemory.service.VideoProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequiredArgsConstructor
public class VideoWebhookController {
    private final CloudinaryService cloud;
    private final VideoProcessingService processing;

    @PostMapping("/api/webhooks/cloudinary/video/{id}")
    public ResponseEntity<Void> complete(@PathVariable UUID id, @RequestBody String body,
            @RequestHeader(value = "X-Cld-Timestamp", defaultValue = "") String timestamp,
            @RequestHeader(value = "X-Cld-Signature", defaultValue = "") String signature) {
        if (body.length() > 65536 || !cloud.verifyVideoNotification(body, timestamp, signature))
            return ResponseEntity.status(403).build();
        processing.complete(id, body);
        return ResponseEntity.noContent().build();
    }
}
