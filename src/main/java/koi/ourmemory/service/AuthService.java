package koi.ourmemory.service;

import koi.ourmemory.dto.request.LoginRequest;
import koi.ourmemory.dto.response.AuthResponse;
import koi.ourmemory.dto.response.UserResponse;
import koi.ourmemory.entity.User;
import koi.ourmemory.exception.UnauthorizedException;
import koi.ourmemory.repository.UserRepository;
import koi.ourmemory.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final koi.ourmemory.security.AuthSessions sessions;
    private final UserRepository userRepository;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return response(sessions.create(userPrincipal.getId()));
    }

    public AuthResponse refreshToken(String refreshToken) {
        return response(sessions.refresh(refreshToken));
    }

    public void logout(String refreshToken, String accessToken) {
        sessions.revoke(refreshToken);
        sessions.revoke(accessToken);
    }

    private AuthResponse response(koi.ourmemory.security.AuthSessions.Issued issued) {
        User user = issued.user();
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .accessToken(issued.access())
                .refreshToken(issued.refresh())
                .tokenType("Cookie")
                .build();
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal)) {
            throw new UnauthorizedException("Not authenticated");
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal)) {
            throw new UnauthorizedException("Not authenticated");
        }

        return ((UserPrincipal) principal).getId();
    }
}
