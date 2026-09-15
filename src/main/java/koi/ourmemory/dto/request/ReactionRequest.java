package koi.ourmemory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReactionRequest {

    @NotBlank(message = "Emoji is required")
    @Pattern(regexp = "[❤️🥰😂😢]", message = "Invalid emoji. Allowed: ❤️ 🥰 😂 😢")
    private String emoji;
}
