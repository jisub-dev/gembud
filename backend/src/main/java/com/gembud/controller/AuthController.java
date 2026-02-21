package com.gembud.controller;

import com.gembud.config.JwtConfig;
import com.gembud.dto.request.LoginRequest;
import com.gembud.dto.request.RefreshTokenRequest;
import com.gembud.dto.request.SignupRequest;
import com.gembud.dto.response.AuthResponse;
import com.gembud.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 *
 * Phase 12: Tokens delivered via HTTP-only cookies (not in response body)
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtConfig jwtConfig;

    /**
     * Registers a new user.
     *
     * Phase 12: Tokens delivered via HTTP-only cookies
     *
     * @param request signup request
     * @param response HTTP response for setting cookies
     * @return authentication response (email, nickname only)
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
        @Valid @RequestBody SignupRequest request,
        HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.signup(request);
        setTokenCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());

        // Remove tokens from response body (Phase 12 security)
        AuthResponse responseBody = AuthResponse.builder()
            .email(authResponse.getEmail())
            .nickname(authResponse.getNickname())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * Authenticates user and returns tokens.
     *
     * Phase 12: Tokens delivered via HTTP-only cookies
     *
     * @param request login request
     * @param response HTTP response for setting cookies
     * @return authentication response (email, nickname only)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request);
        setTokenCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());

        // Remove tokens from response body (Phase 12 security)
        AuthResponse responseBody = AuthResponse.builder()
            .email(authResponse.getEmail())
            .nickname(authResponse.getNickname())
            .build();

        return ResponseEntity.ok(responseBody);
    }

    /**
     * Refreshes access token.
     *
     * Phase 12: Reads refresh token from cookie, sets new access token cookie
     *
     * @param refreshToken refresh token from HTTP-only cookie
     * @param response HTTP response for setting new access token cookie
     * @return empty response (new token in cookie)
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshTokenRequest request = RefreshTokenRequest.builder()
            .refreshToken(refreshToken)
            .build();

        AuthResponse authResponse = authService.refreshToken(request);

        // Set new access token cookie
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", authResponse.getAccessToken())
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(jwtConfig.getAccessTokenExpiration() / 1000)
            .sameSite("Strict")
            .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        return ResponseEntity.ok().build();
    }

    /**
     * Logs out user by clearing authentication cookies.
     *
     * Phase 12: Clears HTTP-only cookies
     *
     * @param response HTTP response for clearing cookies
     * @return empty response
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Clear access token cookie
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .sameSite("Strict")
            .build();

        // Clear refresh token cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .sameSite("Strict")
            .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok().build();
    }

    /**
     * Helper method to set token cookies.
     *
     * @param response HTTP response
     * @param accessToken access token
     * @param refreshToken refresh token
     */
    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(jwtConfig.getAccessTokenExpiration() / 1000)
            .sameSite("Strict")
            .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(jwtConfig.getRefreshTokenExpiration() / 1000)
            .sameSite("Strict")
            .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }
}
