package com.habfitt.dashboard;

import com.habfitt.dashboard.controller.DashboardController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(com.habfitt.dashboard.config.SecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @WithMockUser
    void getDashboard_returnsNoPlanState() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")
                .header("X-Timezone", "Asia/Kolkata"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("NO_PLAN"))
            .andExpect(jsonPath("$.streak.current").value(0))
            .andExpect(jsonPath("$.weeklyAdherence").isArray())
            .andExpect(jsonPath("$.weeklyAdherence.length()").value(7))
            .andExpect(jsonPath("$.coachNote").isNotEmpty());
    }

    @Test
    @WithMockUser
    void getDashboard_defaultsToUtcWhenInvalidTimezone() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")
                .header("X-Timezone", "Invalid/Zone"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("NO_PLAN"));
    }

    @Test
    void getDashboard_returns401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
            .andExpect(status().isUnauthorized());
    }

    // Note: /actuator/health is not available in @WebMvcTest slice (actuator
    // autoconfiguration is excluded). Health is verified via live curl in integration testing.
}
