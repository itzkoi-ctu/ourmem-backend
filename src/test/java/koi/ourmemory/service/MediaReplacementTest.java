package koi.ourmemory.service;

import koi.ourmemory.dto.response.CloudinaryUploadResult;
import koi.ourmemory.dto.response.SessionResponse;
import koi.ourmemory.dto.response.PhotoResponse;
import koi.ourmemory.entity.Photo;
import koi.ourmemory.mapper.PhotoMapper;
import koi.ourmemory.repository.PhotoRepository;
import koi.ourmemory.repository.UserRepository;
import koi.ourmemory.repository.ReactionRepository;
import koi.ourmemory.entity.PhotoSession;
import koi.ourmemory.mapper.SessionMapper;
import koi.ourmemory.repository.PhotoSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MediaReplacementTest {
    private final CloudinaryService cloud = mock(CloudinaryService.class);
    private final MediaReplacementCleanup cleanup = new MediaReplacementCleanup(cloud);

    @BeforeEach void begin() { TransactionSynchronizationManager.initSynchronization(); }
    @AfterEach void end() { TransactionSynchronizationManager.clearSynchronization(); }

    private void complete(int status) {
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(status));
    }

    @Test void oldAssetIsOnlyDeletedAfterCommit() {
        cleanup.register("old", "new", "video");
        verifyNoInteractions(cloud);
        complete(TransactionSynchronization.STATUS_COMMITTED);
        verify(cloud).deleteResource("old", "video");
        verify(cloud, never()).deleteResource("new", "video");
    }

    @Test void rollbackCleansNewAssetAndKeepsOriginal() {
        cleanup.register("old", "new", "image");
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(cloud).deleteResource("new", "image");
        verify(cloud, never()).deleteResource("old", "image");
    }

    @Test void failedVideoUploadDoesNotDeleteOrChangeOriginal() {
        var repository = mock(PhotoSessionRepository.class);
        var mapper = mock(SessionMapper.class);
        var session = new PhotoSession();
        var id = UUID.randomUUID();
        session.setVideoPublicId("old"); session.setVideoUrl("old-url");
        when(repository.findById(id)).thenReturn(Optional.of(session));
        when(cloud.uploadVideo(any())).thenThrow(new IllegalStateException("Upload failed"));
        var service = new VideoService(repository, cloud, mapper, cleanup);
        var file = new MockMultipartFile("file", "new.mp4", "video/mp4", new byte[]{1});
        assertThrows(IllegalStateException.class, () -> service.uploadTimelapse(id, file));
        assertEquals("old", session.getVideoPublicId());
        assertEquals("old-url", session.getVideoUrl());
        verify(cloud, never()).deleteResource(anyString(), anyString());
        verify(repository, never()).save(any());
    }

    @Test void successfulVideoReplacementDefersOldAssetDeletion() {
        var repository = mock(PhotoSessionRepository.class);
        var mapper = mock(SessionMapper.class);
        var session = new PhotoSession();
        var id = UUID.randomUUID();
        session.setVideoPublicId("old");
        when(repository.findById(id)).thenReturn(Optional.of(session));
        when(cloud.uploadVideo(any())).thenReturn(CloudinaryUploadResult.builder()
                .publicId("new").url("new-url").thumbnailUrl("new-thumb").build());
        when(repository.save(session)).thenReturn(session);
        when(mapper.toResponse(session)).thenReturn(new SessionResponse());
        var service = new VideoService(repository, cloud, mapper, cleanup);
        service.uploadTimelapse(id, new MockMultipartFile("file", "new.mp4", "video/mp4", new byte[]{1}));
        assertEquals("new", session.getVideoPublicId());
        verify(cloud, never()).deleteResource(anyString(), anyString());
        complete(TransactionSynchronization.STATUS_COMMITTED);
        verify(cloud).deleteResource("old", "video");
    }

    @Test void editingCoverPreservesPhotoIdentityAndCaptionAndUpdatesCover() {
        var photos = mock(PhotoRepository.class);
        var sessions = mock(PhotoSessionRepository.class);
        var reactions = mock(ReactionRepository.class);
        var mapper = mock(PhotoMapper.class);
        var auth = mock(AuthService.class);
        var sessionId = UUID.randomUUID(); var photoId = UUID.randomUUID();
        var session = new PhotoSession(); session.setCoverPhotoUrl("old-thumb");
        var photo = new Photo(); photo.setId(photoId); photo.setSession(session);
        photo.setCloudinaryPublicId("old"); photo.setThumbnailUrl("old-thumb");
        photo.setCaption("Our memory"); photo.setSortOrder(3);
        when(photos.findByIdAndSessionId(photoId, sessionId)).thenReturn(Optional.of(photo));
        when(cloud.uploadImage(any())).thenReturn(CloudinaryUploadResult.builder()
                .publicId("new").url("new-url").thumbnailUrl("new-thumb").build());
        when(photos.save(photo)).thenReturn(photo);
        when(mapper.toResponse(photo)).thenReturn(new PhotoResponse());
        when(reactions.findByPhotoId(photoId)).thenReturn(List.of());
        var service = new PhotoService(photos, sessions, mock(UserRepository.class), reactions, cloud, mapper, auth, cleanup);
        service.replaceImage(sessionId, photoId, new MockMultipartFile("file", "edited.png", "image/png", new byte[]{1}));
        assertEquals(photoId, photo.getId()); assertEquals("Our memory", photo.getCaption());
        assertEquals(3, photo.getSortOrder()); assertEquals("new-thumb", session.getCoverPhotoUrl());
        verify(cloud, never()).deleteResource(anyString(), anyString());
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(cloud).deleteResource("new", "image");
        verify(cloud, never()).deleteResource("old", "image");
    }

    @Test void imageFromAnotherSessionCannotBeReplaced() {
        var photos = mock(PhotoRepository.class);
        var sessionId = UUID.randomUUID(); var photoId = UUID.randomUUID();
        when(photos.findByIdAndSessionId(photoId, sessionId)).thenReturn(Optional.empty());
        var service = new PhotoService(photos, mock(PhotoSessionRepository.class), mock(UserRepository.class),
                mock(ReactionRepository.class), cloud, mock(PhotoMapper.class), mock(AuthService.class), cleanup);
        assertThrows(koi.ourmemory.exception.ResourceNotFoundException.class, () -> service.replaceImage(sessionId, photoId,
                new MockMultipartFile("file", "edited.png", "image/png", new byte[]{1})));
        verifyNoInteractions(cloud);
    }
}
