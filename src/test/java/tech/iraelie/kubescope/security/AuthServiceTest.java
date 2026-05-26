package tech.iraelie.kubescope.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.iraelie.kubescope.domain.refreshToken.RefreshTokenService;
import tech.iraelie.kubescope.domain.user.Role;
import tech.iraelie.kubescope.domain.user.User;
import tech.iraelie.kubescope.domain.user.UserRepository;
import tech.iraelie.kubescope.security.dto.AuthResponse;
import tech.iraelie.kubescope.security.dto.LoginRequest;
import tech.iraelie.kubescope.security.dto.RefreshRequest;
import tech.iraelie.kubescope.security.dto.RegisterRequest;
import tech.iraelie.kubescope.security.dto.TokenPair;
import tech.iraelie.kubescope.security.exception.UserEmailAlreadyExistException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AuthService authService;

    private User newUser(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Sam")
                .email(email)
                .password("hashed")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    void registerNormalizesEmailAndIssuesTokens() {
        RegisterRequest req = new RegisterRequest("Sam", "  Sam@Example.COM ", "password123");
        when(userRepository.existsByEmail("sam@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("access");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("refresh");

        AuthResponse response = authService.register(req);

        ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCap.capture());
        assertThat(userCap.getValue().getEmail()).isEqualTo("sam@example.com");
        assertThat(userCap.getValue().getPassword()).isEqualTo("hashed");
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest req = new RegisterRequest("Sam", "dup@example.com", "password123");
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(UserEmailAlreadyExistException.class);

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginAuthenticatesAndIssuesTokens() {
        LoginRequest req = new LoginRequest("Alice@Example.com", "password123");
        User principal = newUser("alice@example.com");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(principal)).thenReturn("access");
        when(refreshTokenService.createRefreshToken(principal)).thenReturn("refresh");

        AuthResponse response = authService.login(req);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCap =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCap.capture());
        assertThat(tokenCap.getValue().getPrincipal()).isEqualTo("alice@example.com");
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void logoutRevokesAllTokensForUser() {
        User principal = newUser("logout@example.com");

        authService.logout(principal);

        verify(refreshTokenService, times(1)).revokeAllUserTokens(principal.getId().toString());
    }

    @Test
    void refreshDelegatesToRefreshTokenServiceAndReturnsBothTokens() {
        when(refreshTokenService.rotateRefreshToken("incoming"))
                .thenReturn(new TokenPair("new-access", "new-refresh"));

        AuthResponse response = authService.refresh(new RefreshRequest("incoming"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenService).rotateRefreshToken(eq("incoming"));
    }
}
