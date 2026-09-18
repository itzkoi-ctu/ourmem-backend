package koi.ourmemory.controller;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import koi.ourmemory.dto.request.LoginRequest;
import koi.ourmemory.dto.response.*;
import koi.ourmemory.exception.UnauthorizedException;
import koi.ourmemory.security.JwtAuthenticationFilter;
import koi.ourmemory.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @Value("${app.jwt.access-token-expiration}") private long accessExpiration;
    @Value("${app.jwt.refresh-token-expiration}") private long refreshExpiration;
    @Value("${app.security.cookie-secure:true}") private boolean secure;
    @Value("${app.security.cookie-same-site:Lax}") private String sameSite;

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(java.util.Map.of("token", token.getToken(), "headerName", token.getHeaderName())));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest, HttpServletResponse response) {
        AuthResponse result = authService.login(request);
        // End the previous browser session when switching accounts.
        authService.logout(cookie(servletRequest, "refresh_token"), cookie(servletRequest, "access_token"));
        return authenticated(response, result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            return authenticated(response, authService.refreshToken(cookie(request, "refresh_token")));
        } catch (UnauthorizedException invalid) {
            clearCookies(response);
            throw invalid;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(cookie(request, "refresh_token"), cookie(request, "access_token"));
        clearCookies(response);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(authService.getCurrentUser()));
    }

    private ResponseEntity<ApiResponse<AuthResponse>> authenticated(HttpServletResponse response, AuthResponse result) {
        // Remove old root-path cookies before setting narrowly scoped replacements.
        setCookie(response, "access_token", "", "/", 0);
        setCookie(response, "refresh_token", "", "/", 0);
        setCookie(response, "access_token", result.getAccessToken(), "/api", accessExpiration / 1000);
        setCookie(response, "refresh_token", result.getRefreshToken(), "/api/auth", refreshExpiration / 1000);
        result.setAccessToken(null); result.setRefreshToken(null);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(result));
    }

    private void clearCookies(HttpServletResponse response) {
        setCookie(response, "access_token", "", "/", 0);
        setCookie(response, "refresh_token", "", "/", 0);
        setCookie(response, "access_token", "", "/api", 0);
        setCookie(response, "refresh_token", "", "/api/auth", 0);
    }

    private void setCookie(HttpServletResponse response, String name, String value, String path, long age) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, value).httpOnly(true)
                .secure(secure).sameSite(sameSite).path(path).maxAge(age).build().toString());
    }
    private String cookie(HttpServletRequest request, String name) { return JwtAuthenticationFilter.cookie(request, name); }
}
