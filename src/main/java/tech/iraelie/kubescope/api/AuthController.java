package tech.iraelie.kubescope.api;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tech.iraelie.kubescope.domain.user.User;
import tech.iraelie.kubescope.security.AuthInterface;
import tech.iraelie.kubescope.security.dto.AuthResponse;
import tech.iraelie.kubescope.security.dto.LoginRequest;
import tech.iraelie.kubescope.security.dto.RefreshRequest;
import tech.iraelie.kubescope.security.dto.RegisterRequest;
import tech.iraelie.kubescope.security.exception.TokenException;

import java.time.Duration;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthInterface authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        MDC.put("endpoint", "POST /api/auth/register");
        try {
            log.info("Request received");
            AuthResponse auth = authService.register(request);
            setAuthCookies(response, auth.accessToken(), auth.refreshToken());
            log.info("Request completed");
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } finally {
            MDC.remove("endpoint");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        MDC.put("endpoint", "POST /api/auth/login");
        try {
            log.info("Request received");
            AuthResponse auth = authService.login(request);
            setAuthCookies(response, auth.accessToken(), auth.refreshToken());
            log.info("Request completed");
            return ResponseEntity.ok().build();
        } finally {
            MDC.remove("endpoint");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        MDC.put("endpoint", "POST /api/auth/refresh");
        try {
            if (refreshToken == null) {
                // Throw so GlobalExceptionHandler returns a properly shaped ErrorResponse
                throw new TokenException("No refresh token provided");
            }
            log.info("Request received");
            AuthResponse auth = authService.refresh(new RefreshRequest(refreshToken));
            setAuthCookies(response, auth.accessToken(), auth.refreshToken());
            log.info("Request completed");
            return ResponseEntity.ok().build();
        } finally {
            MDC.remove("endpoint");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal User user,
            HttpServletResponse response) {

        MDC.put("endpoint", "POST /api/auth/logout");
        if (user != null) MDC.put("userId", String.valueOf(user.getId()));
        try {
            log.info("Request received");
            authService.logout(user);
            clearAuthCookies(response);
            log.info("Request completed");
            return ResponseEntity.noContent().build();
        } finally {
            MDC.remove("userId");
            MDC.remove("endpoint");
        }
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("access_token", accessToken)
                        .httpOnly(true).secure(true).path("/")
                        .maxAge(Duration.ofMinutes(15)).sameSite("None")
                        .build().toString());

        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("refresh_token", refreshToken)
                        .httpOnly(true).secure(true).path("/api/auth/refresh")
                        .maxAge(Duration.ofDays(7)).sameSite("None")
                        .build().toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("access_token", "")
                        .httpOnly(true).secure(true).path("/")
                        .maxAge(Duration.ZERO).sameSite("None")
                        .build().toString());

        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("refresh_token", "")
                        .httpOnly(true).secure(true).path("/api/auth/refresh")
                        .maxAge(Duration.ZERO).sameSite("None")
                        .build().toString());
    }
}