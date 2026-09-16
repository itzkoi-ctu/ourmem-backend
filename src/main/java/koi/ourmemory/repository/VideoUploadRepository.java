package koi.ourmemory.repository;

import koi.ourmemory.entity.VideoUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface VideoUploadRepository extends JpaRepository<VideoUpload, UUID> {
    @org.springframework.data.jpa.repository.Query("select v.sessionId from VideoUpload v where v.id = :id")
    Optional<UUID> findSessionId(@org.springframework.data.repository.query.Param("id") UUID id);
    Optional<VideoUpload> findFirstBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}
