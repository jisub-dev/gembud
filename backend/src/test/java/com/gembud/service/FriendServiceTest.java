package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.dto.request.FriendRequest;
import com.gembud.dto.response.FriendResponse;
import com.gembud.entity.Friend;
import com.gembud.entity.Friend.FriendStatus;
import com.gembud.entity.User;
import com.gembud.repository.FriendRepository;
import com.gembud.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for FriendService.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FriendService friendService;

    private User user1;
    private User user2;
    private User user3;
    private Friend pendingFriend;
    private Friend acceptedFriend;
    private FriendRequest friendRequest;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
            .id(1L)
            .email("user1@example.com")
            .nickname("User1")
            .temperature(new BigDecimal("36.5"))
            .build();

        user2 = User.builder()
            .id(2L)
            .email("user2@example.com")
            .nickname("User2")
            .temperature(new BigDecimal("36.5"))
            .build();

        user3 = User.builder()
            .id(3L)
            .email("user3@example.com")
            .nickname("User3")
            .temperature(new BigDecimal("36.5"))
            .build();

        pendingFriend = Friend.builder()
            .id(1L)
            .user(user1)
            .friend(user2)
            .status(FriendStatus.PENDING)
            .build();

        acceptedFriend = Friend.builder()
            .id(2L)
            .user(user1)
            .friend(user3)
            .status(FriendStatus.ACCEPTED)
            .build();

        friendRequest = FriendRequest.builder()
            .friendId(2L)
            .build();
    }

    @Test
    @DisplayName("sendFriendRequest - should send friend request successfully")
    void sendFriendRequest_ValidRequest_ShouldSendRequest() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(friendRepository.requestExists(1L, 2L)).thenReturn(false);
        when(friendRepository.save(any(Friend.class))).thenReturn(pendingFriend);

        // When
        FriendResponse response = friendService.sendFriendRequest("user1@example.com", friendRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getFriendId()).isEqualTo(2L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(friendRepository, times(1)).save(any(Friend.class));
    }

    @Test
    @DisplayName("sendFriendRequest - should throw exception when user not found")
    void sendFriendRequest_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            friendService.sendFriendRequest("unknown@example.com", friendRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User not found");
    }

    @Test
    @DisplayName("sendFriendRequest - should throw exception when friend not found")
    void sendFriendRequest_FriendNotFound_ShouldThrowException() {
        // Given
        FriendRequest invalidRequest = FriendRequest.builder()
            .friendId(999L)
            .build();

        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            friendService.sendFriendRequest("user1@example.com", invalidRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Friend user not found");
    }

    @Test
    @DisplayName("sendFriendRequest - should throw exception when sending request to self")
    void sendFriendRequest_RequestToSelf_ShouldThrowException() {
        // Given
        FriendRequest selfRequest = FriendRequest.builder()
            .friendId(1L)
            .build();

        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        // When & Then
        assertThatThrownBy(() ->
            friendService.sendFriendRequest("user1@example.com", selfRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot send friend request to yourself");
    }

    @Test
    @DisplayName("sendFriendRequest - should throw exception when request already exists")
    void sendFriendRequest_RequestExists_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(friendRepository.requestExists(1L, 2L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() ->
            friendService.sendFriendRequest("user1@example.com", friendRequest))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Friend request already exists");
    }

    @Test
    @DisplayName("acceptFriendRequest - should accept request successfully")
    void acceptFriendRequest_ValidRequest_ShouldAcceptRequest() {
        // Given
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user2));
        when(friendRepository.findById(1L)).thenReturn(Optional.of(pendingFriend));
        when(friendRepository.save(any(Friend.class))).thenReturn(pendingFriend);

        // When
        FriendResponse response = friendService.acceptFriendRequest("user2@example.com", 1L);

        // Then
        assertThat(response).isNotNull();
        verify(friendRepository, times(1)).save(pendingFriend);
        assertThat(pendingFriend.getStatus()).isEqualTo(FriendStatus.ACCEPTED);
    }

    @Test
    @DisplayName("acceptFriendRequest - should throw exception when user not found")
    void acceptFriendRequest_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            friendService.acceptFriendRequest("unknown@example.com", 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User not found");
    }

    @Test
    @DisplayName("acceptFriendRequest - should throw exception when request not found")
    void acceptFriendRequest_RequestNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user2));
        when(friendRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            friendService.acceptFriendRequest("user2@example.com", 999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Friend request not found");
    }

    @Test
    @DisplayName("acceptFriendRequest - should throw exception when not the receiver")
    void acceptFriendRequest_NotReceiver_ShouldThrowException() {
        // Given (user1 trying to accept, but user1 is the sender, not receiver)
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(friendRepository.findById(1L)).thenReturn(Optional.of(pendingFriend));

        // When & Then
        assertThatThrownBy(() ->
            friendService.acceptFriendRequest("user1@example.com", 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Only the receiver can accept this request");
    }

    @Test
    @DisplayName("acceptFriendRequest - should throw exception when already accepted")
    void acceptFriendRequest_AlreadyAccepted_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("user3@example.com")).thenReturn(Optional.of(user3));
        when(friendRepository.findById(2L)).thenReturn(Optional.of(acceptedFriend));

        // When & Then
        assertThatThrownBy(() ->
            friendService.acceptFriendRequest("user3@example.com", 2L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Friend request already accepted");
    }

    @Test
    @DisplayName("rejectFriendRequest - should reject request successfully")
    void rejectFriendRequest_ValidRequest_ShouldRejectRequest() {
        // Given
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user2));
        when(friendRepository.findById(1L)).thenReturn(Optional.of(pendingFriend));
        when(friendRepository.save(any(Friend.class))).thenReturn(pendingFriend);

        // When
        FriendResponse response = friendService.rejectFriendRequest("user2@example.com", 1L);

        // Then
        assertThat(response).isNotNull();
        verify(friendRepository, times(1)).save(pendingFriend);
        assertThat(pendingFriend.getStatus()).isEqualTo(FriendStatus.REJECTED);
    }

    @Test
    @DisplayName("rejectFriendRequest - should throw exception when not the receiver")
    void rejectFriendRequest_NotReceiver_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(friendRepository.findById(1L)).thenReturn(Optional.of(pendingFriend));

        // When & Then
        assertThatThrownBy(() ->
            friendService.rejectFriendRequest("user1@example.com", 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Only the receiver can reject this request");
    }

    @Test
    @DisplayName("unfriend - should remove friend successfully")
    void unfriend_ValidRequest_ShouldRemoveFriend() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(friendRepository.areFriends(1L, 3L)).thenReturn(true);

        // When
        friendService.unfriend("user1@example.com", 3L);

        // Then
        verify(friendRepository, times(1)).deleteByUserIdAndFriendId(1L, 3L);
    }

    @Test
    @DisplayName("unfriend - should throw exception when user not found")
    void unfriend_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            friendService.unfriend("unknown@example.com", 3L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User not found");
    }

    @Test
    @DisplayName("unfriend - should throw exception when not friends")
    void unfriend_NotFriends_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(friendRepository.areFriends(1L, 2L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() ->
            friendService.unfriend("user1@example.com", 2L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Not friends with this user");
    }

    @Test
    @DisplayName("getMyFriends - should return friends list")
    void getMyFriends_ShouldReturnFriendsList() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(friendRepository.findAcceptedFriends(1L))
            .thenReturn(Collections.singletonList(acceptedFriend));

        // When
        List<FriendResponse> friends = friendService.getMyFriends("user1@example.com");

        // Then
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getFriendId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getMyFriends - should return empty list when no friends")
    void getMyFriends_NoFriends_ShouldReturnEmptyList() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(friendRepository.findAcceptedFriends(1L)).thenReturn(Collections.emptyList());

        // When
        List<FriendResponse> friends = friendService.getMyFriends("user1@example.com");

        // Then
        assertThat(friends).isEmpty();
    }

    @Test
    @DisplayName("getPendingRequests - should return pending requests")
    void getPendingRequests_ShouldReturnPendingRequests() {
        // Given
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user2));
        when(friendRepository.findByFriendIdAndStatus(2L, FriendStatus.PENDING))
            .thenReturn(Collections.singletonList(pendingFriend));

        // When
        List<FriendResponse> requests = friendService.getPendingRequests("user2@example.com");

        // Then
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getUserId()).isEqualTo(1L);
        assertThat(requests.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("getSentRequests - should return sent requests")
    void getSentRequests_ShouldReturnSentRequests() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(friendRepository.findByUserIdAndStatus(1L, FriendStatus.PENDING))
            .thenReturn(Collections.singletonList(pendingFriend));

        // When
        List<FriendResponse> requests = friendService.getSentRequests("user1@example.com");

        // Then
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getFriendId()).isEqualTo(2L);
        assertThat(requests.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("areFriends - should return true when they are friends")
    void areFriends_AreFriends_ShouldReturnTrue() {
        // Given
        when(friendRepository.areFriends(1L, 3L)).thenReturn(true);

        // When
        boolean result = friendService.areFriends(1L, 3L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("areFriends - should return false when not friends")
    void areFriends_NotFriends_ShouldReturnFalse() {
        // Given
        when(friendRepository.areFriends(1L, 2L)).thenReturn(false);

        // When
        boolean result = friendService.areFriends(1L, 2L);

        // Then
        assertThat(result).isFalse();
    }
}
