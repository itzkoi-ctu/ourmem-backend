package koi.ourmemory.security;

import koi.ourmemory.config.SecurityConfig;
import koi.ourmemory.controller.AuthController;
import koi.ourmemory.controller.VideoWebhookController;
import koi.ourmemory.dto.response.AuthResponse;
import koi.ourmemory.exception.GlobalExceptionHandler;
import koi.ourmemory.exception.UnauthorizedException;
import koi.ourmemory.service.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityHttpTest {
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    @Configuration @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthController.class,
        VideoWebhookController.class, GlobalExceptionHandler.class, Endpoints.class})
    static class Config {
        @Bean AuthSessions sessions() { return mock(AuthSessions.class); }
        @Bean AuthService authService() { return mock(AuthService.class); }
        @Bean CloudinaryService cloud() { return mock(CloudinaryService.class); }
        @Bean VideoProcessingService processing() { return mock(VideoProcessingService.class); }
        @Bean org.springframework.security.core.userdetails.UserDetailsService users() {
            return name -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Not found"); };
        }
    }
    @RestController static class Endpoints {
        @GetMapping("/api/test") String privateGet() { return "ok"; }
        @PostMapping("/api/test") String privatePost() { return "ok"; }
        @GetMapping("/api/public/test") String publicGet() { return "public"; }
    }
    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
            "app.cors.allowed-origins=https://ourmem.example", "app.jwt.access-token-expiration=900000",
            "app.jwt.refresh-token-expiration=604800000");
        context.register(Config.class); context.refresh();
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(context.getBean(AuthSessions.class).authenticate(anyString())).thenThrow(new UnauthorizedException("Expired"));
    }
    @AfterEach void close() { context.close(); }
    @Test void missingExpiredAndBearerOnlyTokensReturn401() throws Exception {
        mvc.perform(get("/api/test")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/test").cookie(new Cookie("access_token", "expired"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/test").header("Authorization", "Bearer legacy")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test void publicEndpointIgnoresExpiredCookie() throws Exception {
        mvc.perform(get("/api/public/test").cookie(new Cookie("access_token", "expired"))).andExpect(status().isOk());
        verifyNoInteractions(context.getBean(AuthSessions.class));
    }
    @Test void ownerCanReadAndWriteButOtherRolesCannot() throws Exception {
        var principal = new UserPrincipal(UUID.randomUUID(), "owner@example.test", "", "Owner",
            List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_OWNER")));
        doReturn(principal).when(context.getBean(AuthSessions.class)).authenticate("valid");
        mvc.perform(get("/api/test").cookie(new Cookie("access_token", "valid"))).andExpect(status().isOk());
        mvc.perform(post("/api/test").cookie(new Cookie("access_token", "valid")).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/test").cookie(new Cookie("access_token", "valid"))).andExpect(status().isForbidden());
        var guest = new UserPrincipal(UUID.randomUUID(), "guest@example.test", "", "Guest", List.of());
        doReturn(guest).when(context.getBean(AuthSessions.class)).authenticate("guest");
        mvc.perform(get("/api/test").cookie(new Cookie("access_token", "guest"))).andExpect(status().isForbidden());
    }
    @Test void refreshWithExpiredAccessCookieStillReachesRefreshService() throws Exception {
        when(context.getBean(AuthService.class).refreshToken("refresh")).thenReturn(AuthResponse.builder()
            .accessToken("new-access").refreshToken("new-refresh").build());
        mvc.perform(post("/api/auth/refresh").with(csrf())
            .cookie(new Cookie("access_token", "expired"), new Cookie("refresh_token", "refresh")))
            .andExpect(status().isOk());
        verifyNoInteractions(context.getBean(AuthSessions.class));
    }
    @Test void csrfRequiredEvenForLoginAndLogout() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("CSRF_INVALID"));
        mvc.perform(post("/api/auth/logout")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isForbidden());
    }
    @Test void csrfBootstrapAndCookieLoginDoNotExposeJwt() throws Exception {
        var bootstrap = mvc.perform(get("/api/auth/csrf").header("Origin", "https://ourmem.example"))
            .andExpect(status().isOk()).andReturn();
        var json = tools.jackson.databind.json.JsonMapper.builder().build().readTree(bootstrap.getResponse().getContentAsString());
        String token = json.path("data").path("token").asText();
        assertFalse(token.isBlank());
        Cookie csrfCookie = bootstrap.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie); assertTrue(csrfCookie.isHttpOnly()); assertTrue(csrfCookie.getSecure());
        when(context.getBean(AuthService.class).login(any())).thenReturn(AuthResponse.builder()
            .accessToken("private-access").refreshToken("private-refresh").build());
        var result = mvc.perform(post("/api/auth/login").cookie(csrfCookie).header("X-XSRF-TOKEN", token)
            .contentType("application/json").content("{\"email\":\"owner@example.test\",\"password\":\"password\"}"))
            .andExpect(status().isOk()).andReturn();
        assertFalse(result.getResponse().getContentAsString().contains("private-access"));
        assertFalse(result.getResponse().getContentAsString().contains("private-refresh"));
        assertTrue(result.getResponse().getHeaders("Set-Cookie").stream()
            .anyMatch(value -> value.contains("refresh_token=private-refresh") && value.contains("Path=/api/auth")
                && value.contains("HttpOnly") && value.contains("Secure") && value.contains("SameSite=Lax")));
    }
    @Test void untrustedOriginCannotBootstrapCsrf() throws Exception {
        mvc.perform(get("/api/auth/csrf").header("Origin", "https://evil.example")).andExpect(status().isForbidden());
    }
    @Test void webhookRequiresProviderSignatureNotJwtOrCsrf() throws Exception {
        String path = "/api/webhooks/cloudinary/video/" + UUID.randomUUID();
        mvc.perform(post(path).servletPath(path).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        when(context.getBean(CloudinaryService.class).verifyVideoNotification("{}", "123", "valid")).thenReturn(true);
        mvc.perform(post(path).servletPath(path).contentType("application/json").content("{}")
            .header("X-Cld-Timestamp", "123").header("X-Cld-Signature", "valid")).andExpect(status().isNoContent());
        verify(context.getBean(VideoProcessingService.class)).complete(any(), eq("{}"));
    }
    @Test void logoutRevokesTokensAndClearsBothCookiePaths() throws Exception {
        var result = mvc.perform(post("/api/auth/logout").with(csrf())
            .cookie(new Cookie("access_token", "access"), new Cookie("refresh_token", "refresh")))
            .andExpect(status().isOk()).andReturn();
        verify(context.getBean(AuthService.class)).logout("refresh", "access");
        assertEquals(4, result.getResponse().getHeaders("Set-Cookie").size());
    }
}
