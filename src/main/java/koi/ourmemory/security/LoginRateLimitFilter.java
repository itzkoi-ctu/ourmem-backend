package koi.ourmemory.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Small, bounded per-process guard. Uses socket peer IP, never untrusted forwarded headers. */
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private record Window(long start, int attempts) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final LongSupplier clock;
    public LoginRateLimitFilter() { this(System::currentTimeMillis); }
    LoginRateLimitFilter(LongSupplier clock) { this.clock = clock; }

    private synchronized boolean allow(String peer) {
        long now = clock.getAsLong();
        windows.entrySet().removeIf(entry -> now - entry.getValue().start() >= 60_000);
        Window window = windows.get(peer);
        if (window == null && windows.size() >= 10_000) return false;
        if (window == null) window = new Window(now, 0);
        if (window.attempts() >= 20) return false;
        windows.put(peer, new Window(window.start(), window.attempts() + 1));
        return true;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equals(request.getMethod()) && request.getRequestURI().equals(request.getContextPath() + "/api/auth/login")
                && !allow(request.getRemoteAddr())) {
            response.setStatus(429); response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many login attempts. Please wait a minute.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
