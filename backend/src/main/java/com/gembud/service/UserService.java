package com.gembud.service;

import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User profile domain service.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * Update user's nickname with 30-day cooldown policy.
     *
     * @param userId user ID
     * @param requestedNickname requested nickname
     * @return updated user
     */
    @Transactional
    public User updateNickname(Long userId, String requestedNickname) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String normalizedNickname = requestedNickname == null ? "" : requestedNickname.trim();

        if (user.getNickname().equals(normalizedNickname)) {
            return user;
        }

        if (!user.canChangeNickname()) {
            throw new BusinessException(ErrorCode.NICKNAME_CHANGE_TOO_SOON);
        }

        if (userRepository.existsByNickname(normalizedNickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.updateNickname(normalizedNickname);
        return userRepository.save(user);
    }
}
