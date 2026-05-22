package tech.iraelie.kubescope.domain.refreshToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.iraelie.kubescope.domain.user.User;
import tech.iraelie.kubescope.security.JwtService;
import tech.iraelie.kubescope.security.dto.TokenPair;
import tech.iraelie.kubescope.security.exception.TokenException;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${spring.security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public String createRefreshToken(User user) {
        String family = UUID.randomUUID().toString();
        return createRefreshToken(user, family);
    }

    private String createRefreshToken(User user, String family) {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken token = RefreshToken.builder()
                .tokenHash(DigestUtils.sha256Hex(rawToken))
                .user(user)
                .family(family)
                .revoked(false)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .build();
        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Transactional
    public TokenPair rotateRefreshToken(String incomingToken) {
        String hash = DigestUtils.sha256Hex(incomingToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found — possible replay or token scraping");
                    return new TokenException("Refresh token not found");
                });

        if (stored.isRevoked()) {
            // Security event: revoked token presented — nuke the entire family
            log.error("REFRESH TOKEN REUSE DETECTED family={} userId={}",
                    stored.getFamily(), stored.getUser().getId());
            refreshTokenRepository.revokeAllByFamily(stored.getFamily());
            throw new TokenException("Refresh token reuse detected. All sessions invalidated.");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            // Dirty flag — @Transactional will flush automatically, no explicit save needed
            stored.setRevoked(true);
            log.info("Refresh token expired userId={}", stored.getUser().getId());
            throw new TokenException("Refresh token expired. Please log in again.");
        }

        // Rotate: mark current as revoked, issue new one in the same family
        stored.setRevoked(true);
        // Dirty check flushes the revocation — explicit save removed

        String newRefreshToken = createRefreshToken(stored.getUser(), stored.getFamily());
        String newAccessToken = jwtService.generateToken(stored.getUser());

        log.info("Refresh token rotated userId={} family={}", stored.getUser(), stored.getFamily());
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void revokeAllUserTokens(String userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("All refresh tokens revoked userId={}", userId);
    }
}