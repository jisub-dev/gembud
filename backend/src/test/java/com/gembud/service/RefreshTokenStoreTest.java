package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RefreshTokenStore refreshTokenStore;

    @BeforeEach
    void setUp() {
        refreshTokenStore = new RefreshTokenStore(stringRedisTemplate);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("save/matches/delete refresh token")
    void refreshTokenCrud() {
        String hashedToken = sha256("refresh-token");

        refreshTokenStore.save("user@example.com", "refresh-token", 60000L);
        verify(valueOperations).set("refresh:user@example.com", hashedToken, 60000L, TimeUnit.MILLISECONDS);

        when(valueOperations.get("refresh:user@example.com")).thenReturn(hashedToken);
        assertThat(refreshTokenStore.get("user@example.com")).isEqualTo(hashedToken);
        assertThat(refreshTokenStore.matches("user@example.com", "refresh-token")).isTrue();
        assertThat(refreshTokenStore.matches("user@example.com", "other-refresh-token")).isFalse();

        refreshTokenStore.delete("user@example.com");
        verify(stringRedisTemplate).delete("refresh:user@example.com");
    }

    @Test
    @DisplayName("saveSession/getSession/deleteSession")
    void sessionCrud() {
        refreshTokenStore.saveSession("user@example.com", "session-123", 30000L);
        verify(valueOperations).set("session:user@example.com", "session-123", 30000L, TimeUnit.MILLISECONDS);

        when(valueOperations.get("session:user@example.com")).thenReturn("session-123");
        assertThat(refreshTokenStore.getSession("user@example.com")).isEqualTo("session-123");

        refreshTokenStore.deleteSession("user@example.com");
        verify(stringRedisTemplate).delete("session:user@example.com");
    }

    @Test
    @DisplayName("deleteAll refresh token + session")
    void deleteAll() {
        refreshTokenStore.deleteAll("user@example.com");

        verify(stringRedisTemplate).delete("refresh:user@example.com");
        verify(stringRedisTemplate).delete("session:user@example.com");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
