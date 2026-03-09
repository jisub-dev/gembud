package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FriendService friendService;

    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        me = User.builder()
            .email("me@example.com")
            .nickname("Me")
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(me, "id", 1L);

        other = User.builder()
            .email("other@example.com")
            .nickname("Other")
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(other, "id", 2L);
    }

    @Test
    @DisplayName("sendFriendRequest - friendId로 요청 전송 성공")
    void sendFriendRequest_WithFriendId_ShouldSucceed() {
        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(friendRepository.requestExists(1L, 2L)).thenReturn(false);
        when(friendRepository.save(any(Friend.class))).thenAnswer(inv -> inv.getArgument(0));

        FriendRequest request = FriendRequest.builder().friendId(2L).build();

        FriendResponse response = friendService.sendFriendRequest("me@example.com", request);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getFriendId()).isEqualTo(2L);
        verify(friendRepository, times(1)).save(any(Friend.class));
    }

    @Test
    @DisplayName("sendFriendRequest - email로 요청 전송 성공(하위호환)")
    void sendFriendRequest_WithEmail_ShouldSucceed() {
        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));
        when(friendRepository.requestExists(1L, 2L)).thenReturn(false);
        when(friendRepository.save(any(Friend.class))).thenAnswer(inv -> inv.getArgument(0));

        FriendRequest request = FriendRequest.builder().email("other@example.com").build();

        FriendResponse response = friendService.sendFriendRequest("me@example.com", request);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getFriendId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("sendFriendRequest - friendId/email 모두 없으면 예외")
    void sendFriendRequest_MissingTarget_ShouldThrow() {
        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));

        FriendRequest request = FriendRequest.builder().build();

        assertThatThrownBy(() -> friendService.sendFriendRequest("me@example.com", request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.MISSING_REQUIRED_FIELD);
    }

    @Test
    @DisplayName("sendFriendRequest - 동시성 충돌(DataIntegrityViolation)도 중복요청으로 매핑")
    void sendFriendRequest_DataIntegrityViolation_ShouldMapToDuplicate() {
        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(friendRepository.requestExists(1L, 2L)).thenReturn(false);
        when(friendRepository.save(any(Friend.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        FriendRequest request = FriendRequest.builder().friendId(2L).build();

        assertThatThrownBy(() -> friendService.sendFriendRequest("me@example.com", request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("acceptFriendRequest - PENDING만 수락 가능")
    void acceptFriendRequest_NotPending_ShouldThrow() {
        Friend rejected = Friend.builder()
            .id(10L)
            .user(other)
            .friend(me)
            .status(FriendStatus.REJECTED)
            .build();

        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));
        when(friendRepository.findById(10L)).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> friendService.acceptFriendRequest("me@example.com", 10L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_PENDING);
    }

    @Test
    @DisplayName("rejectFriendRequest - PENDING만 거절 가능")
    void rejectFriendRequest_NotPending_ShouldThrow() {
        Friend accepted = Friend.builder()
            .id(11L)
            .user(other)
            .friend(me)
            .status(FriendStatus.ACCEPTED)
            .build();

        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));
        when(friendRepository.findById(11L)).thenReturn(Optional.of(accepted));

        assertThatThrownBy(() -> friendService.rejectFriendRequest("me@example.com", 11L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_PENDING);
    }

    @Test
    @DisplayName("cancelSentRequest - 보낸 PENDING 요청 취소 성공")
    void cancelSentRequest_ShouldDeletePendingRequest() {
        Friend pendingSent = Friend.builder()
            .id(30L)
            .user(me)
            .friend(other)
            .status(FriendStatus.PENDING)
            .build();

        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));
        when(friendRepository.findById(30L)).thenReturn(Optional.of(pendingSent));

        friendService.cancelSentRequest("me@example.com", 30L);

        verify(friendRepository, times(1)).delete(pendingSent);
    }

    @Test
    @DisplayName("cancelSentRequest - PENDING이 아니면 예외")
    void cancelSentRequest_NotPending_ShouldThrow() {
        Friend rejectedSent = Friend.builder()
            .id(31L)
            .user(me)
            .friend(other)
            .status(FriendStatus.REJECTED)
            .build();

        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));
        when(friendRepository.findById(31L)).thenReturn(Optional.of(rejectedSent));

        assertThatThrownBy(() -> friendService.cancelSentRequest("me@example.com", 31L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_PENDING);

        verify(friendRepository, never()).delete(any(Friend.class));
    }

    @Test
    @DisplayName("unfriend - 친구가 아니면 삭제하지 않고 예외")
    void unfriend_NotFriends_ShouldThrow() {
        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));
        when(friendRepository.areFriends(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> friendService.unfriend("me@example.com", 2L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_FRIENDS);

        verify(friendRepository, never()).deleteByUserIdAndFriendId(any(), any());
    }

    @Test
    @DisplayName("getPendingRequests - 받은 요청 전체를 상태 정렬 순서로 반환")
    void getPendingRequests_ShouldReturnReceivedRequests() {
        Friend pending = Friend.builder()
            .id(12L)
            .user(other)
            .friend(me)
            .status(FriendStatus.PENDING)
            .build();
        Friend rejected = Friend.builder()
            .id(13L)
            .user(other)
            .friend(me)
            .status(FriendStatus.REJECTED)
            .build();

        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));
        when(friendRepository.findAllReceivedRequests(1L)).thenReturn(List.of(pending, rejected));

        List<FriendResponse> responses = friendService.getPendingRequests("me@example.com");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getUserId()).isEqualTo(2L);
        assertThat(responses.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(responses.get(1).getStatus()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("getSentRequests - 보낸 요청 전체를 상태 정렬 순서로 반환")
    void getSentRequests_ShouldReturnSentRequests() {
        Friend pending = Friend.builder()
            .id(20L)
            .user(me)
            .friend(other)
            .status(FriendStatus.PENDING)
            .build();
        Friend accepted = Friend.builder()
            .id(21L)
            .user(me)
            .friend(other)
            .status(FriendStatus.ACCEPTED)
            .build();

        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));
        when(friendRepository.findAllSentRequests(1L)).thenReturn(List.of(pending, accepted));

        List<FriendResponse> responses = friendService.getSentRequests("me@example.com");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(responses.get(1).getStatus()).isEqualTo("ACCEPTED");
    }
}
