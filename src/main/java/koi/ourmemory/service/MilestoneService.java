package koi.ourmemory.service;

import koi.ourmemory.dto.request.CreateMilestoneRequest;
import koi.ourmemory.dto.response.MilestoneResponse;
import koi.ourmemory.entity.Milestone;
import koi.ourmemory.entity.User;
import koi.ourmemory.exception.ResourceNotFoundException;
import koi.ourmemory.mapper.MilestoneMapper;
import koi.ourmemory.repository.MilestoneRepository;
import koi.ourmemory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final UserRepository userRepository;
    private final MilestoneMapper milestoneMapper;
    private final AuthService authService;

    public MilestoneResponse createMilestone(CreateMilestoneRequest request) {
        UUID userId = authService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Milestone milestone = milestoneMapper.toEntity(request);
        milestone.setCreatedBy(user);

        Milestone saved = milestoneRepository.save(milestone);
        return milestoneMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MilestoneResponse> getAllMilestones() {
        return milestoneRepository.findAllByOrderByTargetDateAsc()
                .stream().map(milestoneMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MilestoneResponse> getUpcomingMilestones() {
        return milestoneRepository.findUpcoming(LocalDate.now())
                .stream().map(milestoneMapper::toResponse).collect(Collectors.toList());
    }

    public void deleteMilestone(UUID milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", milestoneId));
        milestoneRepository.delete(milestone);
    }
}
