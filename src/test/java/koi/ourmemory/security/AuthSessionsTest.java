package koi.ourmemory.security;

import koi.ourmemory.entity.*;
import koi.ourmemory.repository.*;
import koi.ourmemory.exception.UnauthorizedException;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthSessionsTest {
    AuthSessionRepository repository;
    UserRepository users;
    JwtTokenProvider tokens;
    AuthSessions service;
    AuthSession stored;
    User user;

    @BeforeEach void setup() {
        repository = mock(AuthSessionRepository.class); users = mock(UserRepository.class);
        tokens = new JwtTokenProvider(Base64.getEncoder().encodeToString(new byte[32]), 900000, 604800000);
        service = new AuthSessions(repository, users, tokens);
        user = User.builder().id(UUID.randomUUID()).email("owner@example.test")
                .password("hashed-password").displayName("Owner").role(Role.OWNER).build();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(call -> { stored = call.getArgument(0); return stored; });
        when(repository.findById(any())).thenAnswer(call -> stored != null && stored.getId().equals(call.getArgument(0)) ? Optional.of(stored) : Optional.empty());
        when(repository.findLockedById(any())).thenAnswer(call -> stored != null && stored.getId().equals(call.getArgument(0)) ? Optional.of(stored) : Optional.empty());
    }

    @Test void loginPersistsOnlyHashAndAuthenticates() {
        var issued = service.create(user.getId());
        assertEquals(64, stored.getRefreshHash().length());
        assertNotEquals(issued.refresh(), stored.getRefreshHash());
        assertEquals(user.getId(), service.authenticate(issued.access()).getId());
        assertEquals("ACCESS", tokens.getTokenType(issued.access()));
        assertEquals("REFRESH", tokens.getTokenType(issued.refresh()));
    }
    @Test void refreshRotatesWithoutExtendingAbsoluteLifetime() {
        var first = service.create(user.getId()); Instant expiry = stored.getExpiresAt();
        var second = service.refresh(first.refresh());
        assertNotEquals(first.refresh(), second.refresh());
        assertEquals(expiry, stored.getExpiresAt());
        assertEquals(user.getId(), service.authenticate(second.access()).getId());
    }
    @Test void replayRevokesWholeSession() {
        var first = service.create(user.getId()); var second = service.refresh(first.refresh());
        assertThrows(UnauthorizedException.class, () -> service.refresh(first.refresh()));
        assertTrue(stored.isRevoked());
        assertThrows(UnauthorizedException.class, () -> service.authenticate(second.access()));
        assertThrows(UnauthorizedException.class, () -> service.refresh(second.refresh()));
    }
    @Test void logoutImmediatelyRevokesAccessAndRefresh() {
        var issued = service.create(user.getId()); service.revoke(issued.refresh());
        assertThrows(UnauthorizedException.class, () -> service.authenticate(issued.access()));
        assertThrows(UnauthorizedException.class, () -> service.refresh(issued.refresh()));
    }
    @Test void tokenTypesCannotBeSwapped() {
        var issued = service.create(user.getId());
        assertThrows(UnauthorizedException.class, () -> service.authenticate(issued.refresh()));
        assertThrows(UnauthorizedException.class, () -> service.refresh(issued.access()));
    }
    @Test void expiredSessionRejectsOtherwiseValidJwt() {
        var issued = service.create(user.getId()); stored.setExpiresAt(Instant.now().minusSeconds(1));
        assertThrows(UnauthorizedException.class, () -> service.authenticate(issued.access()));
        assertThrows(UnauthorizedException.class, () -> service.refresh(issued.refresh()));
    }
    @Test void passwordChangeAndDeletedAccountInvalidateSession() {
        var issued = service.create(user.getId()); user.setPassword("changed-password-hash");
        assertThrows(UnauthorizedException.class, () -> service.authenticate(issued.access()));
        assertThrows(UnauthorizedException.class, () -> service.refresh(issued.refresh()));
        when(users.findById(user.getId())).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> service.authenticate(issued.access()));
    }
    @Test void invalidSignatureAndUnknownSessionAreRejected() {
        var issued = service.create(user.getId());
        assertThrows(UnauthorizedException.class, () -> service.refresh(issued.refresh() + "x"));
        String unknown = tokens.generateAccessToken(user.getId(), UUID.randomUUID());
        assertThrows(UnauthorizedException.class, () -> service.authenticate(unknown));
    }
    @Test void expiredAccessCanStillUseValidRefresh() {
        var issued = service.create(user.getId());
        var expiredIssuer = new JwtTokenProvider(Base64.getEncoder().encodeToString(new byte[32]), -1000, 604800000);
        assertThrows(UnauthorizedException.class, () -> service.authenticate(expiredIssuer.generateAccessToken(user.getId(), stored.getId())));
        assertNotNull(service.refresh(issued.refresh()));
    }
}
