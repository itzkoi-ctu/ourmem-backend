package koi.ourmemory.config;

import koi.ourmemory.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.security.cookie-secure:true}")
    private boolean cookieSecure;
    @Value("${app.security.cookie-same-site:Lax}")
    private String cookieSameSite;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (!List.of("None", "Lax", "Strict").contains(cookieSameSite) || (!cookieSecure && "None".equals(cookieSameSite))) {
            throw new IllegalArgumentException("Cookie SameSite must be None/Lax/Strict; None requires Secure=true");
        }
        var csrfRepository = new org.springframework.security.web.csrf.CookieCsrfTokenRepository();
        csrfRepository.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(cookieSecure)
                .sameSite(cookieSameSite).path("/api"));
        var csrfHandler = new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository).csrfTokenRequestHandler(csrfHandler)
                .ignoringRequestMatchers(request -> "POST".equals(request.getMethod())
                    && request.getRequestURI().substring(request.getContextPath().length())
                        .matches("/api/webhooks/cloudinary/video/[^/]+")))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(401); response.setContentType("application/json");
                    response.getWriter().write("{\"success\":false,\"message\":\"Authentication required\"}");
                })
                .accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(403); response.setContentType("application/json");
                    String message = ex instanceof org.springframework.security.web.csrf.CsrfException
                        ? "CSRF_INVALID" : "Access denied";
                    response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
                }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/webhooks/cloudinary/video/*").permitAll()
                .anyRequest().hasRole("OWNER")
            )
            .addFilterBefore(new koi.ourmemory.security.LoginRateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false); // Run only inside Spring Security, not twice as a servlet filter.
        return registration;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim)
                .filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.validateAllowCredentials();
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
