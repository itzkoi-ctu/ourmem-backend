package koi.ourmemory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private long totalSessions;
    private long totalPhotos;
    private List<Map<String, Object>> topLocations;
    private List<Map<String, Object>> sessionsByMonth;
}
