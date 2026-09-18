package koi.ourmemory.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @jakarta.validation.constraints.Size(max = 254)
    private String email;

    @NotBlank(message = "Password is required")
    @jakarta.validation.constraints.Size(max = 256)
    private String password;
}
