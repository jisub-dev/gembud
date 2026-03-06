package com.gembud.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gembud.security.JwtTokenProvider;
import com.gembud.service.RefreshTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = SubscriptionController.class,
    properties = "app.feature.premium.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class SubscriptionControllerToggleTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RefreshTokenStore refreshTokenStore;

    @Test
    @DisplayName("GET /subscriptions/status - premium feature off then route should be 404")
    void subscriptionRoute_NotRegistered_WhenPremiumFeatureOff() throws Exception {
        mockMvc.perform(get("/subscriptions/status"))
            .andExpect(status().isNotFound());
    }
}
