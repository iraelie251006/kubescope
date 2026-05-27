package tech.iraelie.kubescope.domain.refreshToken;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tech.iraelie.kubescope.domain.user.Role;
import tech.iraelie.kubescope.domain.user.User;
import tech.iraelie.kubescope.security.JwtService;
import tech.iraelie.kubescope.security.dto.TokenPair;
import tech.iraelie.kubescope.security.exception.TokenException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @InjectMocks private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshExpirationMs", 60_000L);
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Test")
                .email("user@example.com")
                .password("hash")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    void createRefreshTokenPersistsHashedToken() {
        User user = user();

        String raw = service.createRefreshToken(user);

        assertThat(raw).isNotBlank();
        ArgumentCaptor<RefreshToken> cap = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(cap.capture());
        RefreshToken saved = cap.getValue();
        assertThat(saved.getTokenHash()).isEqualTo(DigestUtils.sha256Hex(raw));
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getFamily()).isNotBlank();
    }

    @Test
    void rotateUnknownTokenThrowsTokenException() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotateRefreshToken("bogus"))
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void rotateRevokedTokenTriggersFamilyRevocation() {
        User user = user();
        String raw = "raw-token";
        RefreshToken existing = RefreshToken.builder()
                .id(UUID.randomUUID())
                .tokenHash(DigestUtils.sha256Hex(raw))
                .user(user)
                .family("fam-1")
                .revoked(true)
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(refreshTokenRepository.findByTokenHash(DigestUtils.sha256Hex(raw))).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.rotateRefreshToken(raw))
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("reuse");
        verify(refreshTokenRepository).revokeAllByFamily("fam-1");
    }

    @Test
    void rotateExpiredTokenIsRevokedAndThrows() {
        User user = user();
        String raw = "raw-expired";
        RefreshToken existing = RefreshToken.builder()
                .id(UUID.randomUUID())
                .tokenHash(DigestUtils.sha256Hex(raw))
                .user(user)
                .family("fam-2")
                .revoked(false)
                .issuedAt(Instant.now().minusSeconds(120))
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        when(refreshTokenRepository.findByTokenHash(DigestUtils.sha256Hex(raw))).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.rotateRefreshToken(raw))
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("expired");
        assertThat(existing.isRevoked()).isTrue();
    }

    @Test
    void rotateValidTokenRevokesOldAndIssuesNewPair() {
        User user = user();
        String raw = "raw-valid";
        RefreshToken existing = RefreshToken.builder()
                .id(UUID.randomUUID())
                .tokenHash(DigestUtils.sha256Hex(raw))
                .user(user)
                .family("fam-3")
                .revoked(false)
                .issuedAt(Instant.now().minusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(refreshTokenRepository.findByTokenHash(DigestUtils.sha256Hex(raw))).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(user)).thenReturn("new-access");

        TokenPair pair = service.rotateRefreshToken(raw);

        assertThat(existing.isRevoked()).isTrue();
        assertThat(pair.accessToken()).isEqualTo("new-access");
        assertThat(pair.refreshToken()).isNotBlank().isNotEqualTo(raw);

        ArgumentCaptor<RefreshToken> cap = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(cap.capture());
        // New token saved with same family
        assertThat(cap.getValue().getFamily()).isEqualTo("fam-3");
        assertThat(cap.getValue().isRevoked()).isFalse();
    }

    @Test
    void revokeAllUserTokensDelegatesToRepository() {
        service.revokeAllUserTokens("user-id-1");

        verify(refreshTokenRepository).revokeAllByUserId("user-id-1");
    }
}
