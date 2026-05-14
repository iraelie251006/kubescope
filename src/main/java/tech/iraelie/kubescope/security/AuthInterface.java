package tech.iraelie.kubescope.security;

import tech.iraelie.kubescope.domain.user.User;
import tech.iraelie.kubescope.security.dto.AuthResponse;
import tech.iraelie.kubescope.security.dto.LoginRequest;
import tech.iraelie.kubescope.security.dto.RefreshRequest;
import tech.iraelie.kubescope.security.dto.RegisterRequest;

public interface AuthInterface {
    AuthResponse register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    void logout(User userDetails);

    AuthResponse refresh(RefreshRequest request);
}
