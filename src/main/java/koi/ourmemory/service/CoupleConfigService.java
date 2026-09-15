package koi.ourmemory.service;

import koi.ourmemory.dto.response.CoupleConfigResponse;
import koi.ourmemory.dto.response.CountdownResponse;
import koi.ourmemory.dto.response.MilestoneResponse;
import koi.ourmemory.entity.CoupleConfig;
import koi.ourmemory.exception.ResourceNotFoundException;
import koi.ourmemory.repository.CoupleConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CoupleConfigService {

    private final CoupleConfigRepository configRepository;
    private final MilestoneService milestoneService;

    @Transactional(readOnly = true)
    public CountdownResponse getCountdown() {
        CoupleConfig anniversaryConfig = configRepository.findByConfigKey("anniversary_date")
                .orElseThrow(() -> new ResourceNotFoundException("Config", "key", "anniversary_date"));
        CoupleConfig coupleNameConfig = configRepository.findByConfigKey("couple_name")
                .orElse(CoupleConfig.builder().configValue("Our Photobooth Memories").build());

        LocalDate anniversaryDate = LocalDate.parse(anniversaryConfig.getConfigValue());
        long daysTogether = ChronoUnit.DAYS.between(anniversaryDate, LocalDate.now());

        // Get next upcoming milestone
        List<MilestoneResponse> upcoming = milestoneService.getUpcomingMilestones();
        MilestoneResponse nextMilestone = upcoming.isEmpty() ? null : upcoming.get(0);

        return CountdownResponse.builder()
                .daysTogether(daysTogether)
                .anniversaryDate(anniversaryDate)
                .coupleName(coupleNameConfig.getConfigValue())
                .nextMilestone(nextMilestone)
                .build();
    }

    public CoupleConfigResponse updateConfig(String key, String value) {
        CoupleConfig config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Config", "key", key));
        config.setConfigValue(value);
        CoupleConfig saved = configRepository.save(config);
        return CoupleConfigResponse.builder()
                .configKey(saved.getConfigKey())
                .configValue(saved.getConfigValue())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CoupleConfigResponse> getAllConfigs() {
        return configRepository.findAll().stream()
                .map(c -> CoupleConfigResponse.builder()
                        .configKey(c.getConfigKey())
                        .configValue(c.getConfigValue())
                        .build())
                .collect(Collectors.toList());
    }
}
