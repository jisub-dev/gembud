package com.gembud.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.config.JwtConfig;
import com.gembud.dto.request.RefreshTokenRequest;
import com.gembud.dto.response.AuthResponse;
import com.gembud.security.JwtTokenProvider;
import com.gembud.service.AuthService;
import com.gembud.service.RateLimitService;
import com.gembud.service.RefreshTokenStore;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtConfig jwtConfig;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RefreshTokenStore refreshTokenStore;

    @BeforeEach
    void setUp() {
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L);
    }

    @Test
    @DisplayName("GET /auth/csrf - should return csrf metadata for bootstrap")
    void bootstrapCsrf_Success() throws Exception {
        CsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "test-token");

        mockMvc.perform(get("/auth/csrf").requestAttr(CsrfToken.class.getName(), csrfToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("Success"))
            .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"))
            .andExpect(jsonPath("$.data.parameterName").value("_csrf"))
            .andExpect(result -> {
                assertThat(result.getResponse().getHeaders("Set-Cookie"))
                    .anySatisfy(header -> {
                        assertThat(header).contains("XSRF-TOKEN=");
                        assertThat(header).contains("Path=/api");
                        assertThat(header).contains("Max-Age=0");
                    });
            });
    }

    @Test
    @DisplayName("POST /auth/refresh - should rotate access and refresh cookies")
    void refresh_Success() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .email("test@example.com")
            .nickname("tester")
            .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class), eq("127.0.0.1")))
            .thenReturn(authResponse);

        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", "old-refresh-token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(result -> {
                Cookie[] cookies = result.getResponse().getCookies();
                Cookie accessCookie = findCookie(cookies, "accessToken");
                Cookie refreshCookie = findCookie(cookies, "refreshToken");

                assertThat(accessCookie).isNotNull();
                assertThat(accessCookie.getValue()).isEqualTo("new-access-token");
                assertThat(accessCookie.getMaxAge()).isEqualTo(3600);
                assertThat(refreshCookie).isNotNull();
                assertThat(refreshCookie.getValue()).isEqualTo("new-refresh-token");
                assertThat(refreshCookie.getMaxAge()).isEqualTo(604800);
            });

        verify(authService).refreshToken(any(RefreshTokenRequest.class), eq("127.0.0.1"));
        verify(rateLimitService).checkRefreshLimit("127.0.0.1");
    }

    @Test
    @DisplayName("POST /auth/refresh - should return 401 when refresh cookie is missing")
    void refresh_MissingCookie() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/logout - should clear auth cookies and invalidate refresh token")
    @WithMockUser(username = "test@example.com")
    void logout_Success() throws Exception {
        mockMvc.perform(post("/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(result -> {
                Cookie[] cookies = result.getResponse().getCookies();
                Cookie accessCookie = findCookie(cookies, "accessToken");
                Cookie refreshCookie = findCookie(cookies, "refreshToken");

                assertThat(accessCookie).isNotNull();
                assertThat(accessCookie.getMaxAge()).isZero();
                assertThat(refreshCookie).isNotNull();
                assertThat(refreshCookie.getMaxAge()).isZero();
            });

        verify(authService).invalidateRefreshToken("test@example.com");
    }

    private Cookie findCookie(Cookie[] cookies, String name) {
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }
}
