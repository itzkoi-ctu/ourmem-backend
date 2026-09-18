package koi.ourmemory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "auth_sessions") @Getter @Setter
public class AuthSession {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "refresh_hash", nullable = false, length = 64) private String refreshHash;
    @Column(name = "credential_hash", nullable = false, length = 64) private String credentialHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(nullable = false) private boolean revoked;
}
