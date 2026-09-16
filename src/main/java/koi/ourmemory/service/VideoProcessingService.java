package koi.ourmemory.service;

import koi.ourmemory.entity.VideoUpload;
import koi.ourmemory.repository.PhotoSessionRepository;
import koi.ourmemory.repository.VideoUploadRepository;
import koi.ourmemory.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.multipart.MultipartFile;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class VideoProcessingService {
    private final VideoUploadRepository jobs;
    private final PhotoSessionRepository sessions;
    private final CloudinaryService cloud;
    private final MediaReplacementCleanup cleanup;
    private final TransactionTemplate tx;
    private final String callbackBase;

    public VideoProcessingService(VideoUploadRepository jobs, PhotoSessionRepository sessions,
            CloudinaryService cloud, MediaReplacementCleanup cleanup, PlatformTransactionManager manager,
            @Value("${app.cloudinary.video-callback-base:}") String callbackBase) {
        this.jobs = jobs; this.sessions = sessions; this.cloud = cloud; this.cleanup = cleanup;
        this.tx = new TransactionTemplate(manager); this.callbackBase = callbackBase.replaceAll("/+$", "");
    }

    public VideoUpload start(UUID sessionId, MultipartFile file) {
        if (!callbackBase.startsWith("https://")) throw new IllegalArgumentException("Video processing requires an HTTPS callback URL on the server");
        cloud.validateVideoFile(file);
        VideoUpload job = tx.execute(status -> {
            lock(sessionId);
            VideoUpload previous = jobs.findFirstBySessionIdOrderByCreatedAtDesc(sessionId).orElse(null);
            expire(previous);
            if (previous != null && "PROCESSING".equals(previous.getStatus()))
                throw new IllegalArgumentException("A video is already processing. Please wait for it to finish");
            // Flush the previous timeout before inserting under the partial unique index.
            jobs.flush();
            VideoUpload next = new VideoUpload(); next.setId(UUID.randomUUID()); next.setSessionId(sessionId);
            next.setPublicId("ourmemory/videos/" + next.getId()); next.setStatus("PROCESSING");
            next.setCreatedAt(OffsetDateTime.now()); return jobs.saveAndFlush(next);
        });
        // No database connection/transaction is held during the network transfer.
        try {
            cloud.uploadVideoAsync(file, job.getPublicId(), callbackBase + "/api/webhooks/cloudinary/video/" + job.getId());
        } catch (RuntimeException e) {
            tx.executeWithoutResult(status -> {
                lock(sessionId);
                jobs.findById(job.getId()).ifPresent(current -> {
                    if ("PROCESSING".equals(current.getStatus())) {
                        current.setStatus("FAILED"); current.setMessage("Upload failed. Select the file and try again.");
                    }
                });
            });
            throw e;
        }
        return latest(sessionId);
    }

    public VideoUpload latest(UUID sessionId) {
        return tx.execute(status -> {
            lock(sessionId);
            VideoUpload job = jobs.findFirstBySessionIdOrderByCreatedAtDesc(sessionId).orElse(null);
            expire(job); return job;
        });
    }

    private void expire(VideoUpload job) {
        if (job != null && "PROCESSING".equals(job.getStatus()) && job.getCreatedAt().isBefore(OffsetDateTime.now().minusMinutes(30))) {
            job.setStatus("FAILED"); job.setMessage("Processing timed out. The previous video is unchanged. Please retry.");
        }
    }

    public void deleteVideo(UUID sessionId) {
        tx.executeWithoutResult(status -> {
            var session = lock(sessionId);
            jobs.findFirstBySessionIdOrderByCreatedAtDesc(sessionId).ifPresent(job -> {
                if ("PROCESSING".equals(job.getStatus())) {
                    job.setStatus("FAILED"); job.setMessage("Video replacement was cancelled.");
                }
            });
            cleanup.register(session.getVideoPublicId(), null, "video");
            session.setVideoPublicId(null); session.setVideoUrl(null); session.setVideoThumbnailUrl(null);
        });
    }

    private koi.ourmemory.entity.PhotoSession lock(UUID id) {
        return sessions.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException("Session", "id", id));
    }

    public void complete(UUID jobId, String body) {
        var payload = tools.jackson.databind.json.JsonMapper.builder().build().readTree(body);
        if (!"eager".equals(payload.path("notification_type").asText())) return;
        tx.executeWithoutResult(status -> {
            UUID sessionId = jobs.findSessionId(jobId).orElse(null);
            if (sessionId == null) return;
            var session = lock(sessionId);
            VideoUpload initial = jobs.findById(jobId).orElse(null);
            if (initial == null) return;
            expire(initial);
            if (!initial.getPublicId().equals(payload.path("public_id").asText()))
                throw new IllegalArgumentException("Unexpected video notification");
            if ("READY".equals(initial.getStatus())) return;
            if ("FAILED".equals(initial.getStatus())) {
                cleanup.register(initial.getPublicId(), null, "video"); return;
            }
            var eager = payload.path("eager");
            boolean ready = eager.isArray() && !eager.isEmpty();
            for (var item : eager) {
                ready &= !item.has("error") && !"failed".equals(item.path("status").asText())
                        && item.path("secure_url").asText().startsWith("https://")
                        && item.path("width").asInt() > 0 && item.path("height").asInt() > 0
                        && "mp4".equals(item.path("format").asText());
            }
            if (!ready || "failed".equals(payload.path("status").asText())) {
                initial.setStatus("FAILED"); initial.setMessage("Video conversion failed. Please try a different source file.");
                cleanup.register(initial.getPublicId(), null, "video"); return;
            }
            String oldId = session.getVideoPublicId();
            session.setVideoPublicId(initial.getPublicId());
            session.setVideoUrl(cloud.playbackUrl(initial.getPublicId()));
            session.setVideoThumbnailUrl(cloud.generateSignedVideoUrl(initial.getPublicId(),
                    new com.cloudinary.Transformation().width(800).height(800).crop("limit").fetchFormat("jpg")));
            initial.setStatus("READY"); initial.setMessage(null);
            // On rollback keep the new asset so the webhook can retry; delete the old one only on commit.
            cleanup.register(oldId, null, "video");
        });
    }
}
