package com.gembud.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gembud.dto.response.AdminUserSecurityStatusResponse;
import com.gembud.security.CustomAuthenticationEntryPoint;
import com.gembud.security.JwtAuthenticationFilter;
import com.gembud.security.JwtTokenProvider;
import com.gembud.security.OAuth2SuccessHandler;
import com.gembud.service.AdminUserService;
import com.gembud.service.RefreshTokenStore;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

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
    @DisplayName("DELETE /admin/users/{id}/login-lock - ADMIN이면 204")
    @WithMockUser(roles = "ADMIN")
    void unlockLoginLock_AsAdmin_204() throws Exception {
        mockMvc.perform(delete("/admin/users/7/login-lock").with(csrf()))
            .andExpect(status().isNoContent());

        verify(adminUserService).unlockLoginLock(7L);
    }

    @Test
    @DisplayName("DELETE /admin/users/{id}/login-lock - USER면 403")
    @WithMockUser(roles = "USER")
    void unlockLoginLock_AsUser_403() throws Exception {
        mockMvc.perform(delete("/admin/users/7/login-lock").with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/users/{id}/security-status - ADMIN이면 200")
    @WithMockUser(roles = "ADMIN")
    void getSecurityStatus_AsAdmin_200() throws Exception {
        AdminUserSecurityStatusResponse response = AdminUserSecurityStatusResponse.builder()
            .userId(7L)
            .email("u@example.com")
            .loginLocked(false)
            .failedLoginCountInWindow(0)
            .windowMinutes(10)
            .build();
        org.mockito.Mockito.when(adminUserService.getSecurityStatus(7L)).thenReturn(response);

        mockMvc.perform(get("/admin/users/7/security-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value(7))
            .andExpect(jsonPath("$.data.email").value("u@example.com"))
            .andExpect(jsonPath("$.data.windowMinutes").value(10));
    }
}
