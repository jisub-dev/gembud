package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.entity.User;
import com.gembud.repository.UserRepository;
import com.gembud.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User management controller.
 *
 * @author Gembud Team
 * @since 2026-02-19 (Phase 12)
 */
@Tag(name = "User", description = "사용자 정보 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Get current user information.
     *
     * Phase 12: Used by frontend to fetch user info after OAuth callback
     * (since tokens are in HTTP-only cookies, not accessible from JavaScript)
     *
     * @param userDetails authenticated user details
     * @return user information (email, nickname)
     */
    @Operation(summary = "Get current user", description = "현재 로그인한 사용자 정보 조회 (OAuth 콜백 후 사용)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCurrentUser(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getEmail())
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Map<String, String> data = new HashMap<>();
        data.put("email", user.getEmail() != null ? user.getEmail() : "");
        data.put("nickname", user.getNickname());

        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
