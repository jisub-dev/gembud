package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.dto.response.AdminUserSecurityStatusResponse;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RateLimitService rateLimitService;

    private AdminUserService adminUserService;
    private User user;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(userRepository, rateLimitService);
        user = User.builder()
            .email("admin@test.com")
            .password("encoded")
            .nickname("TestUser")
            .build();
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("unlockLoginLock - 존재하는 유저의 잠금을 해제한다")
    void unlockLoginLock_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminUserService.unlockLoginLock(1L);

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("unlockLoginLock - 존재하지 않는 유저면 예외를 던진다")
    void unlockLoginLock_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.unlockLoginLock(99L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("getSecurityStatus - 유저의 보안 상태를 반환한다")
    void getSecurityStatus_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rateLimitService.getLoginFailedCount("admin@test.com")).thenReturn(3L);
        when(rateLimitService.getLoginWindowMinutes()).thenReturn(10);

        AdminUserSecurityStatusResponse response = adminUserService.getSecurityStatus(1L);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("admin@test.com");
        assertThat(response.getFailedLoginCountInWindow()).isEqualTo(3L);
        assertThat(response.getWindowMinutes()).isEqualTo(10);
    }

    @Test
    @DisplayName("getSecurityStatus - 존재하지 않는 유저면 예외를 던진다")
    void getSecurityStatus_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getSecurityStatus(99L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }
}
