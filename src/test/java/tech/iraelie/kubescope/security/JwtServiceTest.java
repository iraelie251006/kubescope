package tech.iraelie.kubescope.security;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tech.iraelie.kubescope.domain.user.Role;
import tech.iraelie.kubescope.domain.user.User;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // 32-byte base64-encoded key — required for HS256
    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LXRoaXJ0eXR3by1ieXRlcy1sb25nLWtleQ==";
    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKeyValue", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 60_000L);
        // Invoke the @PostConstruct manually since we're not in a Spring context
        Method init = JwtService.class.getDeclaredMethod("initSigningKey");
        init.setAccessible(true);
        init.invoke(jwtService);
    }

    private User user(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Test")
                .email(email)
                .password("hash")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    void generateAndExtractUsernameRoundTrip() {
        User user = user("alice@example.com");

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@example.com");
    }

    @Test
    void tokenIsValidForMatchingUser() {
        User user = user("bob@example.com");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void tokenIsInvalidForDifferentUser() {
        User issuedTo = user("issued@example.com");
        User other = user("other@example.com");
        String token = jwtService.generateToken(issuedTo);

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void expiredTokenReturnsFalse() throws Exception {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1L);
        User user = user("expired@example.com");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isFalse();
    }

    @Test
    void tamperedTokenFailsValidation() {
        User user = user("tamper@example.com");
        String token = jwtService.generateToken(user);
        String tampered = token.substring(0, token.length() - 2) + "AA";

        assertThat(jwtService.isTokenValid(tampered, user)).isFalse();
    }

    @Test
    void malformedTokenThrowsOnExtract() {
        assertThatThrownBy(() -> jwtService.extractUsername("not-a-jwt"))
                .isInstanceOfAny(io.jsonwebtoken.JwtException.class, SignatureException.class, IllegalArgumentException.class);
    }
}
