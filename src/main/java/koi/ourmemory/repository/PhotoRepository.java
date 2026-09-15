package koi.ourmemory.repository;

import koi.ourmemory.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    List<Photo> findBySessionIdOrderBySortOrderAsc(UUID sessionId);

    List<Photo> findBySessionIdAndIsPublicTrueOrderBySortOrderAsc(UUID sessionId);

    Optional<Photo> findByIdAndSessionId(UUID id, UUID sessionId);

    long countBySessionId(UUID sessionId);

    long count();

    void deleteAllBySessionId(UUID sessionId);
}
