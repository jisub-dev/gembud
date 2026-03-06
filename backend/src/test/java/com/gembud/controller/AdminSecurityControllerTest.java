package com.gembud.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.gembud.entity.SecurityEvent;
import com.gembud.security.CustomAuthenticationEntryPoint;
import com.gembud.security.JwtAuthenticationFilter;
import com.gembud.security.JwtTokenProvider;
import com.gembud.security.OAuth2SuccessHandler;
import com.gembud.service.RefreshTokenStore;
import com.gembud.service.SecurityEventService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminSecurityController.class)
@AutoConfigureMockMvc
class AdminSecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityEventService securityEventService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RefreshTokenStore refreshTokenStore;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("GET /admin/security-events - size 기본값은 20")
    @WithMockUser(roles = "ADMIN")
    void search_DefaultSize_20() throws Exception {
        Page<SecurityEvent> page = new PageImpl<>(java.util.List.of());
        org.mockito.Mockito.when(securityEventService.search(
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.argThat(p -> p.getPageSize() == 20)
        )).thenReturn(page);

        mockMvc.perform(get("/admin/security-events"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /admin/security-events - size 101이면 400")
    @WithMockUser(roles = "ADMIN")
    void search_SizeOver100_400() throws Exception {
        mockMvc.perform(get("/admin/security-events").param("size", "101"))
            .andExpect(status().isBadRequest());
    }
}
