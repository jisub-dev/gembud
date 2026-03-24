package com.gembud.controller;

import com.gembud.config.JwtConfig;
import com.gembud.dto.ApiResponse;
import com.gembud.dto.request.LoginRequest;
import com.gembud.dto.request.RefreshTokenRequest;
import com.gembud.dto.request.SignupRequest;
import com.gembud.dto.response.AuthResponse;
import com.gembud.service.AuthService;
import com.gembud.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
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
@Tag(name = "Authentication", description = "인증 API (회원가입, 로그인, 토큰 갱신)")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtConfig jwtConfig;
    private final RateLimitService rateLimitService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * Bootsraps a CSRF token for anonymous clients before the first POST.
     *
     * @param csrfToken resolved csrf token
     * @return token metadata (cookie carries the actual token value)
     */
    @Operation(summary = "Bootstrap CSRF token", description = "비로그인 상태에서 첫 POST 전에 CSRF 쿠키를 발급받습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CSRF 토큰 부트스트랩 성공")
    })
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> bootstrapCsrf(
        CsrfToken csrfToken,
        HttpServletResponse response
    ) {
        clearLegacyCsrfCookie(response);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "headerName", csrfToken.getHeaderName(),
            "parameterName", csrfToken.getParameterName()
        )));
    }

    /**
     * Registers a new user.
     *
     * Phase 12: Tokens delivered via HTTP-only cookies
     *
     * @param request signup request
     * @param response HTTP response for setting cookies
     * @return authentication response (email, nickname only)
     */
    @Operation(summary = "Sign up", description = "신규 회원 가입 (토큰은 HTTP-only 쿠키로 전달)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 이메일")
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(
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

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(responseBody));
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
    @Operation(summary = "Login", description = "사용자 로그인 (토큰은 HTTP-only 쿠키로 전달)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 불일치)")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse response
    ) {
        String ip = getClientIp(httpRequest);
        AuthResponse authResponse = authService.login(request, ip);
        setTokenCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());

        // Remove tokens from response body (Phase 12 security)
        AuthResponse responseBody = AuthResponse.builder()
            .email(authResponse.getEmail())
            .nickname(authResponse.getNickname())
            .build();

        return ResponseEntity.ok(ApiResponse.success(responseBody));
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
    @Operation(summary = "Refresh token", description = "Access 토큰 갱신 (쿠키에서 refresh 토큰 읽어 새 access 토큰 발급)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 갱신 성공 (새 토큰은 쿠키로 전달)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh 토큰 없음 또는 만료됨")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletRequest httpRequest,
        HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String ip = getClientIp(httpRequest);
        rateLimitService.checkRefreshLimit(ip);

        RefreshTokenRequest request = RefreshTokenRequest.builder()
            .refreshToken(refreshToken)
            .build();

        AuthResponse authResponse = authService.refreshToken(request, ip);

        // Set new tokens via cookies (rotation: both access and refresh are replaced)
        setTokenCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Logs out user by clearing authentication cookies and invalidating refresh token.
     *
     * Phase 12: Clears HTTP-only cookies and removes Redis session
     *
     * @param userDetails authenticated user (may be null if already expired)
     * @param response HTTP response for clearing cookies
     * @return empty response
     */
    @Operation(summary = "Logout", description = "로그아웃 (인증 쿠키 제거 + Redis 세션 삭제)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
        @AuthenticationPrincipal UserDetails userDetails,
        HttpServletResponse response
    ) {
        if (userDetails != null) {
            authService.invalidateRefreshToken(userDetails.getUsername());
        }
        // Clear access token cookie
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(0)
            .sameSite(cookieSameSite)
            .build();

        // Clear refresh token cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(0)
            .sameSite(cookieSameSite)
            .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Extract client IP from request, checking X-Forwarded-For header.
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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
            .secure(cookieSecure)
            .path("/")
            .maxAge(jwtConfig.getAccessTokenExpiration() / 1000)
            .sameSite(cookieSameSite)
            .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(jwtConfig.getRefreshTokenExpiration() / 1000)
            .sameSite(cookieSameSite)
            .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    private void clearLegacyCsrfCookie(HttpServletResponse response) {
        ResponseCookie legacyCsrfCookie = ResponseCookie.from("XSRF-TOKEN", "")
            .httpOnly(false)
            .secure(cookieSecure)
            .path("/api")
            .maxAge(0)
            .sameSite(cookieSameSite)
            .build();

        response.addHeader("Set-Cookie", legacyCsrfCookie.toString());
    }
}
