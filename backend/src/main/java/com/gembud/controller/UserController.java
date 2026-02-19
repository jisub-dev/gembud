package com.gembud.controller;

import com.gembud.entity.User;
import com.gembud.repository.UserRepository;
import com.gembud.security.CustomUserDetails;
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
@RestController
@RequestMapping("/api/users")
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
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getCurrentUser(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getEmail())
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Map<String, String> response = new HashMap<>();
        response.put("email", user.getEmail() != null ? user.getEmail() : "");
        response.put("nickname", user.getNickname());

        return ResponseEntity.ok(response);
    }
}
