package com.gembud.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gembud.entity.SecurityEvent;
import com.gembud.entity.User;
import com.gembud.security.CustomAuthenticationEntryPoint;
import com.gembud.security.CustomUserDetails;
import com.gembud.security.JwtAuthenticationFilter;
import com.gembud.security.JwtTokenProvider;
import com.gembud.security.OAuth2SuccessHandler;
import com.gembud.service.RefreshTokenStore;
import com.gembud.service.SecurityEventService;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    void search_DefaultSize_20() throws Exception {
        Page<SecurityEvent> page = new PageImpl<>(java.util.List.of(), PageRequest.of(0, 20), 0);
        org.mockito.Mockito.when(securityEventService.search(
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.argThat(p -> p.getPageSize() == 20)
        )).thenReturn(page);

        mockMvc.perform(get("/admin/security-events")
                .with(authentication(adminAuth())))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /admin/security-events - size 101이면 400")
    void search_SizeOver100_400() throws Exception {
        mockMvc.perform(get("/admin/security-events")
                .with(authentication(adminAuth()))
                .param("size", "101"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /admin/security-events - from이 to보다 늦으면 400")
    void search_InvalidDateRange_400() throws Exception {
        mockMvc.perform(get("/admin/security-events")
                .with(authentication(adminAuth()))
                .param("from", "2026-03-10T14:00:00")
                .param("to", "2026-03-10T13:00:00"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /admin/security-events/summary - 요약 응답 반환")
    void summary_ReturnsData() throws Exception {
        org.mockito.Mockito.when(securityEventService.summary(120))
            .thenReturn(com.gembud.dto.response.SecurityEventSummaryResponse.builder()
                .loginFailCount(3)
                .loginLockedCount(1)
                .refreshReuseCount(2)
                .rateLimitHitCount(4)
                .build());

        mockMvc.perform(get("/admin/security-events/summary")
                .with(authentication(adminAuth()))
                .param("windowMinutes", "120"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.loginFailCount").value(3))
            .andExpect(jsonPath("$.data.loginLockedCount").value(1))
            .andExpect(jsonPath("$.data.refreshReuseCount").value(2))
            .andExpect(jsonPath("$.data.rateLimitHitCount").value(4));
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        CustomUserDetails principal = new CustomUserDetails(
            99L,
            "admin@gembud.com",
            "",
            User.UserRole.ADMIN
        );
        return new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
