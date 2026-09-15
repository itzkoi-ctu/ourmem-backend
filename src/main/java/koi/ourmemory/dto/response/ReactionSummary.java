package koi.ourmemory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionSummary {
    private String emoji;
    private long count;
    private boolean reactedByCurrentUser;
}
