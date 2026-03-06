package com.gembud.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.service.RefreshTokenStore;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("valid token + session match => authentication set")
    void doFilterInternal_SessionMatch_SetsAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, refreshTokenStore);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserDetails userDetails = User.withUsername("test@example.com").password("x").authorities("ROLE_USER").build();

        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("access-token")).thenReturn("test@example.com");
        when(jwtTokenProvider.getSessionIdFromToken("access-token")).thenReturn("session-1");
        when(refreshTokenStore.getSession("test@example.com")).thenReturn("session-1");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("test@example.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("valid token + session mismatch => authentication not set")
    void doFilterInternal_SessionMismatch_SkipsAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, refreshTokenStore);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("access-token")).thenReturn("test@example.com");
        when(jwtTokenProvider.getSessionIdFromToken("access-token")).thenReturn("session-old");
        when(refreshTokenStore.getSession("test@example.com")).thenReturn("session-new");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername("test@example.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("expired token => AUTH_ERROR_CODE attribute set")
    void doFilterInternal_ExpiredToken_SetsAuthErrorCode() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, refreshTokenStore);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken("expired-token")).thenReturn(false);
        when(jwtTokenProvider.isTokenExpired("expired-token")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertThat(request.getAttribute("AUTH_ERROR_CODE")).isEqualTo("AUTH004");
        verify(filterChain).doFilter(request, response);
    }
}
