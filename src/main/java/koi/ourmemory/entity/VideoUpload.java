package koi.ourmemory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "video_uploads") @Getter @Setter
public class VideoUpload {
    @Id private UUID id;
    @Column(nullable = false) private UUID sessionId;
    @Column(nullable = false, length = 255) private String publicId;
    @Column(nullable = false, length = 20) private String status;
    @Column(length = 500) private String message;
    @Column(nullable = false) private OffsetDateTime createdAt;
}
