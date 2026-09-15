package koi.ourmemory.repository;

import koi.ourmemory.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, UUID> {
    List<Reaction> findByPhotoId(UUID photoId);
    Optional<Reaction> findByPhotoIdAndEmojiAndReactedById(UUID photoId, String emoji, UUID userId);
    List<Reaction> findByPhotoIdAndReactedById(UUID photoId, UUID userId);
    void deleteByPhotoIdAndEmojiAndReactedById(UUID photoId, String emoji, UUID userId);
}
