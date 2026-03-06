package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
            .email("user@example.com")
            .nickname("oldNick")
            .build();
        ReflectionTestUtils.setField(user, "id", 10L);
    }

    @Test
    @DisplayName("updateNickname_Success: 30일 경과 시 닉네임 변경 성공")
    void updateNickname_Success() {
        ReflectionTestUtils.setField(user, "lastNicknameChangedAt", LocalDateTime.now().minusDays(31));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("newNick")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateNickname(10L, "newNick");

        assertThat(updated.getNickname()).isEqualTo("newNick");
        assertThat(updated.getLastNicknameChangedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateNickname_TooSoon: 30일 미경과 시 409 예외")
    void updateNickname_TooSoon() {
        ReflectionTestUtils.setField(user, "lastNicknameChangedAt", LocalDateTime.now().minusDays(10));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateNickname(10L, "newNick"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NICKNAME_CHANGE_TOO_SOON);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateNickname_Duplicate: 중복 닉네임이면 409 예외")
    void updateNickname_Duplicate() {
        ReflectionTestUtils.setField(user, "lastNicknameChangedAt", LocalDateTime.now().minusDays(35));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("takenNick")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateNickname(10L, "takenNick"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateNickname_SameValue: 동일 닉네임 요청은 저장 없이 통과")
    void updateNickname_SameValue() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        User unchanged = userService.updateNickname(10L, "oldNick");

        assertThat(unchanged).isSameAs(user);
        verify(userRepository, never()).save(any(User.class));
    }
}
