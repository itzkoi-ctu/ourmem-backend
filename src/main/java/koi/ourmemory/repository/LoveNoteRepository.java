package koi.ourmemory.repository;

import koi.ourmemory.entity.LoveNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoveNoteRepository extends JpaRepository<LoveNote, UUID> {
    List<LoveNote> findByPhotoIdOrderByCreatedAtDesc(UUID photoId);
}
