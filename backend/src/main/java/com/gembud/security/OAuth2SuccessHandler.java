package com.gembud.security;

import com.gembud.entity.User;
import com.gembud.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth2 authentication success handler.
 * Creates or updates user and generates JWT tokens.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.oauth2.redirect-uri:http://localhost:5173/auth/callback}")
    private String redirectUri;

    /**
     * Handles successful OAuth2 authentication.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param authentication authentication object
     * @throws IOException if redirect fails
     */
    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String registrationId = extractRegistrationId(request);
        User user = processOAuth2User(registrationId, oAuth2User);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        String targetUrl = String.format(
            "%s?accessToken=%s&refreshToken=%s&email=%s&nickname=%s",
            redirectUri,
            URLEncoder.encode(accessToken, StandardCharsets.UTF_8),
            URLEncoder.encode(refreshToken, StandardCharsets.UTF_8),
            URLEncoder.encode(user.getEmail() != null ? user.getEmail() : "", StandardCharsets.UTF_8),
            URLEncoder.encode(user.getNickname(), StandardCharsets.UTF_8)
        );

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Processes OAuth2 user data and creates/updates user.
     *
     * @param registrationId OAuth2 provider (google, discord)
     * @param oAuth2User OAuth2 user data
     * @return created or updated user
     */
    private User processOAuth2User(String registrationId, OAuth2User oAuth2User) {
        String oauthId;
        String email;
        String nickname;

        Map<String, Object> attributes = oAuth2User.getAttributes();

        if ("google".equalsIgnoreCase(registrationId)) {
            oauthId = (String) attributes.get("sub");
            email = (String) attributes.get("email");
            nickname = (String) attributes.get("name");
        } else if ("discord".equalsIgnoreCase(registrationId)) {
            oauthId = String.valueOf(attributes.get("id"));
            email = (String) attributes.get("email");
            nickname = (String) attributes.get("username");
        } else {
            throw new IllegalArgumentException("Unsupported OAuth2 provider: " + registrationId);
        }

        User.OAuthProvider provider = User.OAuthProvider.valueOf(registrationId.toUpperCase());

        return userRepository.findByOauthProviderAndOauthId(provider, oauthId)
            .orElseGet(() -> {
                // Check if email already exists with different provider
                if (email != null && userRepository.existsByEmail(email)) {
                    throw new IllegalStateException(
                        "Email already registered with different provider"
                    );
                }

                // Generate unique nickname if exists
                String uniqueNickname = generateUniqueNickname(nickname);

                User newUser = User.builder()
                    .oauthProvider(provider)
                    .oauthId(oauthId)
                    .email(email)
                    .nickname(uniqueNickname)
                    .build();

                return userRepository.save(newUser);
            });
    }

    /**
     * Generates unique nickname by appending number if exists.
     *
     * @param baseNickname base nickname
     * @return unique nickname
     */
    private String generateUniqueNickname(String baseNickname) {
        String nickname = baseNickname;
        int counter = 1;

        while (userRepository.existsByNickname(nickname)) {
            nickname = baseNickname + counter;
            counter++;
        }

        return nickname;
    }

    /**
     * Extracts OAuth2 provider registration ID from request URI.
     *
     * @param request HTTP request
     * @return registration ID (google, discord)
     */
    private String extractRegistrationId(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        // Expected URI: /login/oauth2/code/{registrationId}
        String[] parts = requestUri.split("/");
        if (parts.length >= 4) {
            return parts[parts.length - 1];
        }
        throw new IllegalArgumentException("Cannot extract registration ID from URI");
    }
}
