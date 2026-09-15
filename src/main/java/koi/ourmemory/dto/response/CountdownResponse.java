package koi.ourmemory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountdownResponse {
    private long daysTogether;
    private LocalDate anniversaryDate;
    private String coupleName;
    private MilestoneResponse nextMilestone;
}
