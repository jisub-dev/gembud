package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.entity.User;
import com.gembud.repository.UserRepository;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final UserService userService;

    @Value("${app.feature.premium.enabled:false}")
    private boolean premiumFeatureEnabled;

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getEmail())
            .orElseThrow(() -> new IllegalStateException("User not found"));

        return ResponseEntity.ok(ApiResponse.success(buildUserResponse(user)));
    }

    /**
     * Search users for friend request.
     *
     * @param userDetails authenticated user details
     * @param query nickname/email query
     * @param limit max result count
     * @return user summaries
     */
    @Operation(summary = "Search users", description = "친구 추가용 사용자 검색")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchUsers(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam("q") String query,
        @RequestParam(defaultValue = "10") int limit
    ) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < 2) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        int safeLimit = Math.min(Math.max(limit, 1), 20);
        List<User> users = userRepository
            .findByIdNotAndNicknameContainingIgnoreCaseOrIdNotAndEmailContainingIgnoreCase(
                userDetails.getUserId(),
                normalized,
                userDetails.getUserId(),
                normalized,
                PageRequest.of(0, safeLimit)
            );

        List<Map<String, Object>> response = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", user.getId());
            row.put("nickname", user.getNickname());
            row.put("email", user.getEmail());
            response.add(row);
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update current user profile (nickname).
     *
     * @param userDetails authenticated user details
     * @param request update request
     * @return updated user info
     */
    @Operation(summary = "Update current user profile", description = "현재 로그인한 사용자 프로필 수정")
    @PatchMapping("/me")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCurrentUser(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        User user = userService.updateNickname(userDetails.getUserId(), request.getNickname());

        return ResponseEntity.ok(ApiResponse.success(buildUserResponse(user)));
    }

    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("email", user.getEmail() != null ? user.getEmail() : "");
        data.put("nickname", user.getNickname());
        data.put("temperature", user.getTemperature());
        data.put("isPremium", premiumFeatureEnabled && user.isPremium());
        data.put("premiumExpiresAt", premiumFeatureEnabled ? user.getPremiumExpiresAt() : null);
        return data;
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다")
        private String nickname;
    }
}
