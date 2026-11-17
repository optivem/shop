package com.mycompany.myshop.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycompany.myshop.api.controller.HealthController;
import com.mycompany.myshop.core.services.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** B-PRG 4.3: every response carries the security headers set in SecurityConfig. */
@WebMvcTest(HealthController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, ProblemDetailSecurityHandlers.class})
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    // StorefrontWebConfig's interceptor needs it; the health route never calls it.
    @MockitoBean
    private ProductService productService;

    @Test
    void responsesCarryTheSecurityHeaders() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Security-Policy", SecurityConfig.CONTENT_SECURITY_POLICY))
            .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
            .andExpect(header().string("Strict-Transport-Security", "max-age=31536000"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
