package koi.ourmemory.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimitTest {
    @Test void loginIsBoundedAndWindowRecoversWithoutBlockingWebhook() throws Exception {
        AtomicLong clock = new AtomicLong(1000);
        LoginRateLimitFilter filter = new LoginRateLimitFilter(clock::get);
        for (int i = 0; i < 20; i++) assertEquals(200, request(filter, "/api/auth/login"));
        assertEquals(429, request(filter, "/api/auth/login"));
        assertEquals(200, request(filter, "/api/webhooks/cloudinary/video/test"));
        clock.addAndGet(60000);
        assertEquals(200, request(filter, "/api/auth/login"));
    }
    private int request(LoginRateLimitFilter filter, String path) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }
}
