package koi.ourmemory.service;

import koi.ourmemory.entity.PhotoSession;
import koi.ourmemory.entity.VideoUpload;
import koi.ourmemory.repository.PhotoSessionRepository;
import koi.ourmemory.repository.VideoUploadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoProcessingTest {
    private final VideoUploadRepository jobs = mock(VideoUploadRepository.class);
    private final PhotoSessionRepository sessions = mock(PhotoSessionRepository.class);
    private final CloudinaryService cloud = mock(CloudinaryService.class);
    private final MediaReplacementCleanup cleanup = mock(MediaReplacementCleanup.class);
    private final UUID sessionId = UUID.randomUUID();
    private final VideoUpload job = new VideoUpload();
    private final PhotoSession session = new PhotoSession();
    private VideoProcessingService service;

    @BeforeEach void setup() {
        var manager = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object transaction, TransactionDefinition definition) {}
            protected void doCommit(DefaultTransactionStatus status) {}
            protected void doRollback(DefaultTransactionStatus status) {}
        };
        service = new VideoProcessingService(jobs, sessions, cloud, cleanup, manager, "https://example.com");
        job.setId(UUID.randomUUID()); job.setSessionId(sessionId); job.setPublicId("new");
        job.setStatus("PROCESSING"); job.setCreatedAt(OffsetDateTime.now());
        session.setVideoPublicId("old"); session.setVideoUrl("old-url");
        when(jobs.findSessionId(job.getId())).thenReturn(Optional.of(sessionId));
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobs.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)).thenReturn(Optional.of(job));
        when(sessions.findLockedById(sessionId)).thenReturn(Optional.of(session));
        when(cloud.playbackUrl("new")).thenReturn("https://example.com/playable.mp4");
    }

    private String ready() {
        return """
          {"notification_type":"eager","public_id":"new","eager":[
           {"format":"mp4","width":360,"height":640,"secure_url":"https://example.com/playable.mp4"}]}
          """;
    }

    @Test void oldVideoRemainsUntilEagerSuccess() {
        assertEquals("old", session.getVideoPublicId());
        service.complete(job.getId(), ready());
        assertEquals("READY", job.getStatus());
        assertEquals("new", session.getVideoPublicId());
        assertEquals("https://example.com/playable.mp4", session.getVideoUrl());
        verify(cleanup).register("old", null, "video");
    }
    @Test void duplicateCallbackDoesNotDeleteActiveAsset() {
        service.complete(job.getId(), ready()); service.complete(job.getId(), ready());
        verify(cleanup, times(1)).register("old", null, "video");
        verifyNoMoreInteractions(cleanup);
    }
    @Test void conversionFailureKeepsOldVideo() {
        service.complete(job.getId(), "{\"notification_type\":\"eager\",\"public_id\":\"new\",\"status\":\"failed\"}");
        assertEquals("FAILED", job.getStatus()); assertEquals("old", session.getVideoPublicId());
        verify(cleanup).register("new", null, "video");
    }
    @Test void missingDerivedOutputCannotReplaceVideo() {
        service.complete(job.getId(), "{\"notification_type\":\"eager\",\"public_id\":\"new\",\"eager\":[]}");
        assertEquals("FAILED", job.getStatus()); assertEquals("old", session.getVideoPublicId());
    }
    @Test void audioOnlyFileCannotReplaceVideo() {
        service.complete(job.getId(), ready().replace("\"width\":360", "\"width\":0"));
        assertEquals("FAILED", job.getStatus()); assertEquals("old", session.getVideoPublicId());
    }
    @Test void lateCallbackCannotReplaceNewerVideo() {
        job.setCreatedAt(OffsetDateTime.now().minusMinutes(31));
        service.complete(job.getId(), ready());
        assertEquals("FAILED", job.getStatus()); assertEquals("old", session.getVideoPublicId());
        verify(cleanup).register("new", null, "video");
    }
    @Test void notificationForDifferentAssetIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.complete(job.getId(), ready().replace("\"new\"", "\"other\"")));
        assertEquals("old", session.getVideoPublicId()); verifyNoInteractions(cleanup);
    }
    @Test void statusSurvivesPageReloadAndTimesOut() {
        assertEquals("PROCESSING", service.latest(sessionId).getStatus());
        job.setCreatedAt(OffsetDateTime.now().minusMinutes(31));
        assertEquals("FAILED", service.latest(sessionId).getStatus());
    }
    @Test void deletingVideoCancelsReplacement() {
        service.deleteVideo(sessionId); service.complete(job.getId(), ready());
        assertNull(session.getVideoPublicId()); assertEquals("FAILED", job.getStatus());
    }
}
