package koi.ourmemory.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import koi.ourmemory.dto.request.LoginRequest;
import koi.ourmemory.dto.response.ApiResponse;
import koi.ourmemory.dto.response.AuthResponse;
import koi.ourmemory.dto.response.UserResponse;
import koi.ourmemory.exception.UnauthorizedException;
import koi.ourmemory.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request);
        setAuthCookies(response, authResponse);
        authResponse.setRefreshToken(null); // Strip from response body

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractRefreshToken(request);
        if (refreshToken == null) {
            throw new UnauthorizedException("Refresh token not found");
        }

        AuthResponse authResponse = authService.refreshToken(refreshToken);
        setAuthCookies(response, authResponse);
        authResponse.setRefreshToken(null); // Strip from response body

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        org.springframework.http.ResponseCookie accessCookie = org.springframework.http.ResponseCookie
                .from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, accessCookie.toString());

        org.springframework.http.ResponseCookie refreshCookie = org.springframework.http.ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    private void setAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
        org.springframework.http.ResponseCookie accessCookie = org.springframework.http.ResponseCookie
                .from("access_token", authResponse.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(accessTokenExpiration / 1000)
                .sameSite("None")
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, accessCookie.toString());

        if (authResponse.getRefreshToken() != null) {
            org.springframework.http.ResponseCookie refreshCookie = org.springframework.http.ResponseCookie
                    .from("refresh_token", authResponse.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(refreshTokenExpiration / 1000)
                    .sameSite("None")
                    .build();
            response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        // Fallback: check request body or header
        String header = request.getHeader("X-Refresh-Token");
        return header;
    }
}
