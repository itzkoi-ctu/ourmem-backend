package koi.ourmemory.repository;

import koi.ourmemory.entity.CoupleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoupleConfigRepository extends JpaRepository<CoupleConfig, UUID> {
    Optional<CoupleConfig> findByConfigKey(String configKey);
}
