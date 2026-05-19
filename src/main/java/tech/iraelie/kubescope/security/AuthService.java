package tech.iraelie.kubescope.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.iraelie.kubescope.domain.user.User;
import tech.iraelie.kubescope.domain.user.UserRepository;
import tech.iraelie.kubescope.security.dto.AuthResponse;
import tech.iraelie.kubescope.security.dto.LoginRequest;
import tech.iraelie.kubescope.security.dto.RegisterRequest;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthInterface {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration attempt with existing email");  // never log the email
            throw new UserEmailAlreadyExistException();
        }

        User user = User.builder()
                .name(request.username())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);
        log.info("User registered userId={}", user.getId());

        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(refreshTokenService.createRefreshToken(user))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(),
                        request.password()
                )
        );

        User user = (User) auth.getPrincipal();
        log.info("User logged in userId={}", user.getId());

        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(refreshTokenService.createRefreshToken(user))
                .build();
    }

    @Override
    public void logout(User user) {
        // Principal is already the authenticated User entity — no DB call needed
        refreshTokenService.revokeAllUserTokens(user.getId());
        log.info("User logged out userId={}", user.getId());
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        TokenPair pair = refreshTokenService.rotateRefreshToken(request.refreshToken());
        return AuthResponse.builder()
                .accessToken(pair.accessToken())
                .refreshToken(pair.refreshToken())
                .build();
    }
}