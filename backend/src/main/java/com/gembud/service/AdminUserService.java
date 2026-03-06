package com.gembud.service;

import com.gembud.dto.response.AdminUserSecurityStatusResponse;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-only user security operations.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;

    @Transactional
    public void unlockLoginLock(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.unlock();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AdminUserSecurityStatusResponse getSecurityStatus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        long failedCount = user.getEmail() != null
            ? rateLimitService.getLoginFailedCount(user.getEmail())
            : 0L;

        return AdminUserSecurityStatusResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .loginLocked(user.isLoginLocked())
            .loginLockedUntil(user.getLoginLockedUntil())
            .failedLoginCountInWindow(failedCount)
            .windowMinutes(rateLimitService.getLoginWindowMinutes())
            .build();
    }
}
