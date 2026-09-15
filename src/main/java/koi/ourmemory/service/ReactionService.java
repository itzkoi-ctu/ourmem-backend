package koi.ourmemory.service;

import koi.ourmemory.dto.request.ReactionRequest;
import koi.ourmemory.dto.response.ReactionSummary;
import koi.ourmemory.entity.Photo;
import koi.ourmemory.entity.Reaction;
import koi.ourmemory.entity.User;
import koi.ourmemory.exception.ResourceNotFoundException;
import koi.ourmemory.repository.PhotoRepository;
import koi.ourmemory.repository.ReactionRepository;
import koi.ourmemory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public List<ReactionSummary> toggleReaction(UUID photoId, ReactionRequest request) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));

        UUID userId = authService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Optional<Reaction> existing = reactionRepository
                .findByPhotoIdAndEmojiAndReactedById(photoId, request.getEmoji(), userId);

        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            Reaction reaction = Reaction.builder()
                    .photo(photo)
                    .emoji(request.getEmoji())
                    .reactedBy(user)
                    .build();
            reactionRepository.save(reaction);
        }

        return getReactionSummary(photoId, userId);
    }

    @Transactional(readOnly = true)
    public List<ReactionSummary> getReactionSummary(UUID photoId, UUID currentUserId) {
        List<Reaction> reactions = reactionRepository.findByPhotoId(photoId);
        Map<String, List<Reaction>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji));

        return grouped.entrySet().stream()
                .map(entry -> ReactionSummary.builder()
                        .emoji(entry.getKey())
                        .count(entry.getValue().size())
                        .reactedByCurrentUser(currentUserId != null && entry.getValue().stream()
                                .anyMatch(r -> r.getReactedBy().getId().equals(currentUserId)))
                        .build())
                .collect(Collectors.toList());
    }
}
