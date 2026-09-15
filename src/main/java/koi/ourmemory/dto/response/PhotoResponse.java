package koi.ourmemory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoResponse {
    private UUID id;
    private UUID sessionId;
    private String originalUrl;
    private String thumbnailUrl;
    private String caption;
    private Integer sortOrder;
    private Boolean isPublic;
    private String uploadedByName;
    private List<NoteResponse> loveNotes;
    private List<ReactionSummary> reactions;
    private OffsetDateTime createdAt;
}
