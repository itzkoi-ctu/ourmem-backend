package koi.ourmemory.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReorderPhotosRequest {

    @NotEmpty(message = "Photo IDs list is required")
    private List<UUID> photoIds;
}
