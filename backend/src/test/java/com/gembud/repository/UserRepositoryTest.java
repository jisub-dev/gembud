package com.gembud.repository;

import com.gembud.config.TestcontainersConfig;
import com.gembud.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository 테스트
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    void findByEmail_Success() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스터")
                .build();
        entityManager.persistAndFlush(user);

        // when
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("존재하지 않는 이메일 조회 시 빈 Optional 반환")
    void findByEmail_NotFound() {
        // when
        Optional<User> found = userRepository.findByEmail("notfound@example.com");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("OAuth 제공자와 ID로 사용자 조회 성공")
    void findByOauthProviderAndOauthId_Success() {
        // given
        User user = User.builder()
                .oauthProvider(User.OAuthProvider.GOOGLE)
                .oauthId("google-123456")
                .nickname("구글사용자")
                .build();
        entityManager.persistAndFlush(user);

        // when
        Optional<User> found = userRepository.findByOauthProviderAndOauthId(
                User.OAuthProvider.GOOGLE,
                "google-123456"
        );

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getOauthProvider()).isEqualTo(User.OAuthProvider.GOOGLE);
        assertThat(found.get().getOauthId()).isEqualTo("google-123456");
    }

    @Test
    @DisplayName("이메일 중복 체크 - 존재함")
    void existsByEmail_True() {
        // given
        User user = User.builder()
                .email("duplicate@example.com")
                .password("password123")
                .nickname("중복테스트")
                .build();
        entityManager.persistAndFlush(user);

        // when
        boolean exists = userRepository.existsByEmail("duplicate@example.com");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("이메일 중복 체크 - 존재하지 않음")
    void existsByEmail_False() {
        // when
        boolean exists = userRepository.existsByEmail("notexist@example.com");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("닉네임 중복 체크 - 존재함")
    void existsByNickname_True() {
        // given
        User user = User.builder()
                .email("user@example.com")
                .password("password123")
                .nickname("중복닉네임")
                .build();
        entityManager.persistAndFlush(user);

        // when
        boolean exists = userRepository.existsByNickname("중복닉네임");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자 저장 시 기본 온도 36.5 설정")
    void save_DefaultTemperature() {
        // given
        User user = User.builder()
                .email("temp@example.com")
                .password("password123")
                .nickname("온도테스트")
                .build();

        // when
        User saved = userRepository.save(user);
        entityManager.flush();

        // then
        assertThat(saved.getTemperature()).isEqualTo(new BigDecimal("36.5"));
    }

    @Test
    @DisplayName("사용자 온도 업데이트 테스트")
    void updateTemperature_Success() {
        // given
        User user = User.builder()
                .email("temp@example.com")
                .password("password123")
                .nickname("온도변경")
                .build();
        User saved = userRepository.save(user);
        entityManager.flush();

        // when
        saved.updateTemperature(new BigDecimal("5.0"));
        userRepository.save(saved);
        entityManager.flush();
        entityManager.clear();

        User updated = userRepository.findById(saved.getId()).get();

        // then
        assertThat(updated.getTemperature()).isEqualTo(new BigDecimal("41.50"));
    }

    @Test
    @DisplayName("사용자 온도 최소값 제한 (0도)")
    void updateTemperature_MinLimit() {
        // given
        User user = User.builder()
                .email("min@example.com")
                .password("password123")
                .nickname("최소온도")
                .build();
        User saved = userRepository.save(user);

        // when
        saved.updateTemperature(new BigDecimal("-50.0"));
        userRepository.save(saved);
        entityManager.flush();

        // then
        assertThat(saved.getTemperature()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("사용자 온도 최대값 제한 (100도)")
    void updateTemperature_MaxLimit() {
        // given
        User user = User.builder()
                .email("max@example.com")
                .password("password123")
                .nickname("최대온도")
                .build();
        User saved = userRepository.save(user);

        // when
        saved.updateTemperature(new BigDecimal("100.0"));
        userRepository.save(saved);
        entityManager.flush();

        // then
        assertThat(saved.getTemperature()).isEqualTo(new BigDecimal("100"));
    }

    @Test
    @DisplayName("OAuth 사용자 여부 확인")
    void isOAuthUser_True() {
        // given
        User user = User.builder()
                .oauthProvider(User.OAuthProvider.DISCORD)
                .oauthId("discord-789")
                .nickname("디스코드사용자")
                .build();

        // when & then
        assertThat(user.isOAuthUser()).isTrue();
        assertThat(user.isEmailUser()).isFalse();
    }

    @Test
    @DisplayName("이메일 사용자 여부 확인")
    void isEmailUser_True() {
        // given
        User user = User.builder()
                .email("email@example.com")
                .password("password123")
                .nickname("이메일사용자")
                .build();

        // when & then
        assertThat(user.isEmailUser()).isTrue();
        assertThat(user.isOAuthUser()).isFalse();
    }
}
