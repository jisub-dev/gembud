package com.gembud.service;

import com.gembud.dto.request.FriendRequest;
import com.gembud.dto.response.FriendResponse;
import com.gembud.entity.Friend;
import com.gembud.entity.Friend.FriendStatus;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.FriendRepository;
import com.gembud.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for friend operations.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    /**
     * Send friend request.
     *
     * @param userEmail current user email
     * @param request friend request
     * @return created friend relationship
     */
    @Transactional
    public FriendResponse sendFriendRequest(String userEmail, FriendRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User friend = resolveTargetUser(request);

        // Cannot send request to self
        if (user.getId().equals(friend.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_ADD_SELF_AS_FRIEND);
        }

        // Check if request already exists (bidirectional)
        if (friendRepository.requestExists(user.getId(), friend.getId())) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }

        Friend friendRelation = Friend.builder()
            .user(user)
            .friend(friend)
            .status(FriendStatus.PENDING)
            .build();

        try {
            friendRepository.save(friendRelation);
        } catch (DataIntegrityViolationException e) {
            // Race-safe duplicate handling (bidirectional unique index)
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }

        return FriendResponse.from(friendRelation);
    }

    /**
     * Accept friend request.
     *
     * @param userEmail current user email
     * @param requestId friend request ID
     * @return updated friend relationship
     */
    @Transactional
    public FriendResponse acceptFriendRequest(String userEmail, Long requestId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Friend friendRequest = friendRepository.findById(requestId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        // Only the receiver can accept the request
        if (!friendRequest.getFriend().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NOT_REQUEST_RECEIVER);
        }

        if (friendRequest.getStatus() == FriendStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_ACCEPTED);
        }

        if (friendRequest.getStatus() != FriendStatus.PENDING) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_PENDING);
        }

        friendRequest.accept();
        friendRepository.save(friendRequest);

        return FriendResponse.from(friendRequest);
    }

    /**
     * Reject friend request.
     *
     * @param userEmail current user email
     * @param requestId friend request ID
     * @return updated friend relationship
     */
    @Transactional
    public FriendResponse rejectFriendRequest(String userEmail, Long requestId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Friend friendRequest = friendRepository.findById(requestId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        // Only the receiver can reject the request
        if (!friendRequest.getFriend().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NOT_REQUEST_RECEIVER);
        }

        if (friendRequest.getStatus() != FriendStatus.PENDING) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_PENDING);
        }

        friendRequest.reject();
        friendRepository.save(friendRequest);

        return FriendResponse.from(friendRequest);
    }

    /**
     * Cancel sent friend request.
     *
     * @param userEmail current user email
     * @param requestId friend request ID
     */
    @Transactional
    public void cancelSentRequest(String userEmail, Long requestId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Friend friendRequest = friendRepository.findById(requestId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        // Only the sender can cancel the request.
        if (!friendRequest.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NOT_REQUEST_RECEIVER);
        }

        if (friendRequest.getStatus() != FriendStatus.PENDING) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_PENDING);
        }

        friendRepository.delete(friendRequest);
    }

    /**
     * Unfriend (remove friend).
     *
     * @param userEmail current user email
     * @param friendId friend user ID
     */
    @Transactional
    public void unfriend(String userEmail, Long friendId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!friendRepository.areFriends(user.getId(), friendId)) {
            throw new BusinessException(ErrorCode.NOT_FRIENDS);
        }

        friendRepository.deleteByUserIdAndFriendId(user.getId(), friendId);
    }

    /**
     * Get my friends list.
     *
     * @param userEmail current user email
     * @return list of friends
     */
    public List<FriendResponse> getMyFriends(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return friendRepository.findAcceptedFriends(user.getId()).stream()
            .map(FriendResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Get pending friend requests (received).
     *
     * @param userEmail current user email
     * @return list of pending requests
     */
    public List<FriendResponse> getPendingRequests(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return friendRepository.findAllReceivedRequests(user.getId()).stream()
            .map(FriendResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Get sent friend requests.
     *
     * @param userEmail current user email
     * @return list of sent requests
     */
    public List<FriendResponse> getSentRequests(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return friendRepository.findAllSentRequests(user.getId()).stream()
            .map(FriendResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Check if two users are friends.
     *
     * @param userId user ID
     * @param friendId friend ID
     * @return true if they are friends
     */
    public boolean areFriends(Long userId, Long friendId) {
        return friendRepository.areFriends(userId, friendId);
    }

    private User resolveTargetUser(FriendRequest request) {
        if (request.getFriendId() != null) {
            return userRepository.findById(request.getFriendId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            return userRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        }

        throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD);
    }
}
