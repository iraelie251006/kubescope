package tech.iraelie.kubescope.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RegisterRequest(
        @NotBlank String username,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8) String password) {
}
