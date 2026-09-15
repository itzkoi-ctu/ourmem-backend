package koi.ourmemory.service;

import koi.ourmemory.dto.response.StatsResponse;
import koi.ourmemory.repository.PhotoRepository;
import koi.ourmemory.repository.PhotoSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final PhotoSessionRepository sessionRepository;
    private final PhotoRepository photoRepository;

    public StatsResponse getStats() {
        long totalSessions = sessionRepository.count();
        long totalPhotos = photoRepository.count();

        List<Map<String, Object>> topLocations = sessionRepository.findTopLocations().stream()
                .map(row -> Map.<String, Object>of("location", row[0] != null ? row[0] : "Unknown", "count", row[1]))
                .collect(Collectors.toList());

        List<Map<String, Object>> sessionsByMonth = sessionRepository.findSessionCountByMonth().stream()
                .map(row -> Map.<String, Object>of("month", row[0] != null ? row[0] : 0, "count", row[1]))
                .collect(Collectors.toList());

        return StatsResponse.builder()
                .totalSessions(totalSessions)
                .totalPhotos(totalPhotos)
                .topLocations(topLocations)
                .sessionsByMonth(sessionsByMonth)
                .build();
    }
}
