package koi.ourmemory.repository;

import koi.ourmemory.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {
    List<Milestone> findAllByOrderByTargetDateAsc();

    @Query("SELECT m FROM Milestone m WHERE m.targetDate >= :today ORDER BY m.targetDate ASC")
    List<Milestone> findUpcoming(@org.springframework.data.repository.query.Param("today") LocalDate today);
}
