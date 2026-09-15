package koi.ourmemory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private UUID id;
    private String title;
    private LocalDate sessionDate;
    private String location;
    private String moodTag;
    private String spotifyLink;
    private String description;
    private String coverPhotoUrl;
    private Boolean isPublic;
    private String videoUrl;
    private String videoThumbnailUrl;
    private String createdByName;
    private long photoCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
