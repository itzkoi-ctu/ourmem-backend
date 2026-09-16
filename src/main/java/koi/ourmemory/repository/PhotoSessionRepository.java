package koi.ourmemory.repository;

import koi.ourmemory.entity.PhotoSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PhotoSessionRepository extends JpaRepository<PhotoSession, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM PhotoSession ps WHERE ps.id = :id")
    java.util.Optional<PhotoSession> findLockedById(@Param("id") UUID id);

    Page<PhotoSession> findAllByOrderBySessionDateDesc(Pageable pageable);

    Page<PhotoSession> findByIsPublicTrueOrderBySessionDateDesc(Pageable pageable);

    @Query("SELECT ps FROM PhotoSession ps WHERE EXTRACT(MONTH FROM ps.sessionDate) = :month AND EXTRACT(DAY FROM ps.sessionDate) = :day")
    List<PhotoSession> findOnThisDay(@Param("month") int month, @Param("day") int day);

    @Query("SELECT ps FROM PhotoSession ps WHERE ps.isPublic = true AND EXTRACT(MONTH FROM ps.sessionDate) = :month AND EXTRACT(DAY FROM ps.sessionDate) = :day")
    List<PhotoSession> findPublicOnThisDay(@Param("month") int month, @Param("day") int day);

    @Query(value = "SELECT * FROM photo_sessions ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    PhotoSession findRandomSession();

    @Query(value = "SELECT * FROM photo_sessions WHERE is_public = true ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    PhotoSession findRandomPublicSession();

    List<PhotoSession> findByLocationContainingIgnoreCase(String location);

    List<PhotoSession> findByMoodTagContainingIgnoreCase(String moodTag);

    List<PhotoSession> findBySessionDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT ps FROM PhotoSession ps WHERE ps.isPublic = true AND ps.sessionDate BETWEEN :start AND :end")
    List<PhotoSession> findPublicByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // Stats queries
    @Query("SELECT ps.location, COUNT(ps) FROM PhotoSession ps WHERE ps.location IS NOT NULL GROUP BY ps.location ORDER BY COUNT(ps) DESC")
    List<Object[]> findTopLocations();

    @Query("SELECT EXTRACT(MONTH FROM ps.sessionDate), COUNT(ps) FROM PhotoSession ps GROUP BY EXTRACT(MONTH FROM ps.sessionDate) ORDER BY COUNT(ps) DESC")
    List<Object[]> findSessionCountByMonth();

    long count();
}
