package tech.iraelie.kubescope.security.dto;

import lombok.Builder;

@Builder
public record AuthResponse (String accessToken, String refreshToken){
}
