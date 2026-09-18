package koi.ourmemory.security;

import koi.ourmemory.entity.AuthSession;
import koi.ourmemory.entity.User;
import koi.ourmemory.exception.UnauthorizedException;
import koi.ourmemory.repository.AuthSessionRepository;
import koi.ourmemory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class AuthSessions {
    private final AuthSessionRepository sessions;
    private final UserRepository users;
    private final JwtTokenProvider tokens;
    public record Issued(User user, String access, String refresh) {}

    @Transactional
    public Issued create(UUID userId) {
        User user = users.findById(userId).orElseThrow(AuthSessions::unauthorized);
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID()); session.setUserId(userId);
        session.setExpiresAt(tokens.newSessionExpiry());
        session.setCredentialHash(hash(user.getPassword()));
        Issued issued = issue(user, session);
        sessions.save(session);
        return issued;
    }

    // Commit revocation even when rejecting a replayed refresh token.
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Issued refresh(String token) {
        requireType(token, "REFRESH");
        AuthSession session = sessions.findLockedById(sessionId(token)).orElseThrow(AuthSessions::unauthorized);
        User user = validate(session, token);
        if (!MessageDigest.isEqual(hash(token).getBytes(StandardCharsets.US_ASCII),
                session.getRefreshHash().getBytes(StandardCharsets.US_ASCII))) {
            session.setRevoked(true);
            throw unauthorized();
        }
        return issue(user, session);
    }

    @Transactional(readOnly = true)
    public UserPrincipal authenticate(String token) {
        requireType(token, "ACCESS");
        AuthSession session = sessions.findById(sessionId(token)).orElseThrow(AuthSessions::unauthorized);
        return UserPrincipal.create(validate(session, token));
    }

    @Transactional
    public void revoke(String token) {
        if (token == null || !tokens.validateToken(token)) return;
        UUID id;
        try { id = tokens.getSessionIdFromToken(token); }
        catch (RuntimeException invalid) { return; }
        sessions.findLockedById(id).ifPresent(session -> {
            if (session.getUserId().equals(tokens.getUserIdFromToken(token))) session.setRevoked(true);
        });
    }

    private User validate(AuthSession session, String token) {
        if (session.isRevoked() || !session.getExpiresAt().isAfter(Instant.now())
                || !session.getUserId().equals(tokens.getUserIdFromToken(token))) throw unauthorized();
        User user = users.findById(session.getUserId()).orElseThrow(AuthSessions::unauthorized);
        if (!session.getCredentialHash().equals(hash(user.getPassword()))) throw unauthorized();
        return user;
    }

    private Issued issue(User user, AuthSession session) {
        String refresh = tokens.generateRefreshToken(user.getId(), session.getId(), session.getExpiresAt());
        session.setRefreshHash(hash(refresh));
        return new Issued(user, tokens.generateAccessToken(user.getId(), session.getId()), refresh);
    }

    private void requireType(String token, String type) {
        if (token == null || !tokens.validateToken(token) || !type.equals(tokens.getTokenType(token))) throw unauthorized();
    }
    private UUID sessionId(String token) {
        try { return tokens.getSessionIdFromToken(token); }
        catch (RuntimeException invalid) { throw unauthorized(); }
    }
    private static UnauthorizedException unauthorized() { return new UnauthorizedException("Session expired. Please sign in again."); }
    static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
