package koi.ourmemory.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import koi.ourmemory.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component @RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final AuthSessions sessions;

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/api/public/") || path.startsWith("/api/webhooks/")
                || path.equals("/api/health") || (path.startsWith("/api/auth/") && !path.equals("/api/auth/me"));
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String token = cookie(request, "access_token");
        if (token != null) {
            try {
                UserPrincipal principal = sessions.authenticate(token);
                var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (UnauthorizedException | io.jsonwebtoken.JwtException | IllegalArgumentException invalid) {
                SecurityContextHolder.clearContext();
            } catch (org.springframework.dao.DataAccessException unavailable) {
                response.setStatus(503); response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Authentication service temporarily unavailable\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    public static String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
