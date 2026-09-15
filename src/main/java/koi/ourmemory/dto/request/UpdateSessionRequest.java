package koi.ourmemory.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateSessionRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private LocalDate sessionDate;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Size(max = 100, message = "Mood tag must not exceed 100 characters")
    private String moodTag;

    @Size(max = 500, message = "Spotify link must not exceed 500 characters")
    private String spotifyLink;

    private String description;
}
