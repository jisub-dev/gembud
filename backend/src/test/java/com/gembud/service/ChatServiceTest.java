package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.dto.request.ChatMessageRequest;
import com.gembud.dto.response.ChatMessageResponse;
import com.gembud.entity.ChatMessage;
import com.gembud.entity.ChatRoom;
import com.gembud.entity.ChatRoomMember;
import com.gembud.entity.Room;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.ChatMessageRepository;
import com.gembud.repository.ChatRoomMemberRepository;
import com.gembud.repository.ChatRoomRepository;
import com.gembud.repository.RoomRepository;
import com.gembud.repository.UserRepository;
import com.gembud.util.HtmlSanitizer;
import java.util.List;
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
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HtmlSanitizer htmlSanitizer;

    @InjectMocks
    private ChatService chatService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
            .email("user@example.com")
            .nickname("User")
            .build();
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("removeMemberFromChatRoom - ROOM_CHAT에서 마지막 멤버 이탈 시 채팅방 삭제")
    void removeMemberFromChatRoom_RoomChatEmpty_ShouldDeleteChatRoom() {
        ChatRoom chatRoom = ChatRoom.builder().type(ChatRoom.ChatRoomType.ROOM_CHAT).build();
        ReflectionTestUtils.setField(chatRoom, "id", 10L);
        ChatRoomMember member = ChatRoomMember.builder().chatRoom(chatRoom).user(user).build();

        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));
        when(chatRoomMemberRepository.countByChatRoomId(10L)).thenReturn(0L);

        chatService.removeMemberFromChatRoom(10L, 1L);

        verify(chatRoomMemberRepository).delete(member);
        verify(chatRoomRepository).delete(chatRoom);
    }

    @Test
    @DisplayName("removeMemberFromChatRoom - GROUP_CHAT에서 마지막 멤버 이탈 시 채팅방 삭제")
    void removeMemberFromChatRoom_GroupChatEmpty_ShouldDeleteChatRoom() {
        ChatRoom chatRoom = ChatRoom.builder().type(ChatRoom.ChatRoomType.GROUP_CHAT).build();
        ReflectionTestUtils.setField(chatRoom, "id", 20L);
        ChatRoomMember member = ChatRoomMember.builder().chatRoom(chatRoom).user(user).build();

        when(chatRoomRepository.findById(20L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(20L, 1L)).thenReturn(Optional.of(member));
        when(chatRoomMemberRepository.countByChatRoomId(20L)).thenReturn(0L);

        chatService.removeMemberFromChatRoom(20L, 1L);

        verify(chatRoomMemberRepository).delete(member);
        verify(chatRoomRepository).delete(chatRoom);
    }

    @Test
    @DisplayName("removeMemberFromChatRoom - DIRECT_CHAT은 마지막 멤버여도 채팅방 유지")
    void removeMemberFromChatRoom_DirectChatEmpty_ShouldNotDeleteChatRoom() {
        ChatRoom chatRoom = ChatRoom.builder().type(ChatRoom.ChatRoomType.DIRECT_CHAT).build();
        ReflectionTestUtils.setField(chatRoom, "id", 30L);
        ChatRoomMember member = ChatRoomMember.builder().chatRoom(chatRoom).user(user).build();

        when(chatRoomRepository.findById(30L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(30L, 1L)).thenReturn(Optional.of(member));
        when(chatRoomMemberRepository.countByChatRoomId(30L)).thenReturn(0L);

        chatService.removeMemberFromChatRoom(30L, 1L);

        verify(chatRoomMemberRepository).delete(member);
        verify(chatRoomRepository, never()).delete(chatRoom);
    }

    @Test
    @DisplayName("removeMemberFromChatRoom - 멤버가 없으면 no-op")
    void removeMemberFromChatRoom_NoMember_ShouldNoOp() {
        ChatRoom chatRoom = ChatRoom.builder().type(ChatRoom.ChatRoomType.ROOM_CHAT).build();
        ReflectionTestUtils.setField(chatRoom, "id", 40L);

        when(chatRoomRepository.findById(40L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(40L, 1L)).thenReturn(Optional.empty());

        chatService.removeMemberFromChatRoom(40L, 1L);

        verify(chatRoomMemberRepository, never()).delete(org.mockito.ArgumentMatchers.any(ChatRoomMember.class));
        verify(chatRoomMemberRepository, never()).countByChatRoomId(40L);
        verify(chatRoomRepository, never()).delete(chatRoom);
    }

    @Test
    @DisplayName("removeMemberFromChatRoom - 채팅방이 없으면 CHAT_ROOM_NOT_FOUND")
    void removeMemberFromChatRoom_ChatRoomNotFound_ShouldThrow() {
        when(chatRoomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.removeMemberFromChatRoom(99L, 1L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("sendMessage - 비멤버는 NOT_CHAT_MEMBER")
    void sendMessage_NotMember_ShouldThrow() {
        ChatRoom chatRoom = ChatRoom.builder().type(ChatRoom.ChatRoomType.ROOM_CHAT).build();
        ReflectionTestUtils.setField(chatRoom, "id", 55L);
        ReflectionTestUtils.setField(chatRoom, "publicId", "chat-public-55");

        ChatMessageRequest request = ChatMessageRequest.builder()
            .chatRoomId("chat-public-55")
            .message("hello")
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(chatRoomRepository.findByPublicId("chat-public-55")).thenReturn(Optional.of(chatRoom));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(55L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> chatService.sendMessage(1L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_CHAT_MEMBER);
    }

    @Test
    @DisplayName("getRecentMessagesByPublicId - 비멤버는 NOT_CHAT_MEMBER")
    void getRecentMessagesByPublicId_NotMember_ShouldThrow() {
        Room room = Room.builder().title("test").build();
        ReflectionTestUtils.setField(room, "id", 101L);
        ReflectionTestUtils.setField(room, "publicId", "room-public-101");

        ChatRoom chatRoom = ChatRoom.builder().type(ChatRoom.ChatRoomType.ROOM_CHAT).relatedRoom(room).build();
        ReflectionTestUtils.setField(chatRoom, "id", 201L);
        ReflectionTestUtils.setField(chatRoom, "publicId", "chat-public-201");

        when(roomRepository.findByPublicId("room-public-101")).thenReturn(Optional.of(room));
        when(chatRoomRepository.findByRelatedRoomId(101L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.findByPublicId("chat-public-201")).thenReturn(Optional.of(chatRoom));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(201L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> chatService.getRecentMessagesByPublicId("room-public-101", 1L, 20))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_CHAT_MEMBER);
    }

    @Test
    @DisplayName("getRecentMessagesByPublicId - room publicId로 정상 조회")
    void getRecentMessagesByPublicId_RoomPublicId_ShouldReturnMessages() {
        Room room = Room.builder().title("test").build();
        ReflectionTestUtils.setField(room, "id", 301L);
        ReflectionTestUtils.setField(room, "publicId", "room-public-301");

        ChatRoom chatRoom = ChatRoom.builder().type(ChatRoom.ChatRoomType.ROOM_CHAT).relatedRoom(room).build();
        ReflectionTestUtils.setField(chatRoom, "id", 401L);
        ReflectionTestUtils.setField(chatRoom, "publicId", "chat-public-401");

        ChatMessage message = ChatMessage.builder()
            .chatRoom(chatRoom)
            .user(user)
            .message("history-message")
            .build();
        ReflectionTestUtils.setField(message, "id", 777L);

        when(roomRepository.findByPublicId("room-public-301")).thenReturn(Optional.of(room));
        when(chatRoomRepository.findByRelatedRoomId(301L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.findByPublicId("chat-public-401")).thenReturn(Optional.of(chatRoom));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(401L, 1L)).thenReturn(true);
        when(chatMessageRepository.findRecentMessages(org.mockito.ArgumentMatchers.eq(401L),
            org.mockito.ArgumentMatchers.any())).thenReturn(List.of(message));

        List<ChatMessageResponse> responses =
            chatService.getRecentMessagesByPublicId("room-public-301", 1L, 20);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getMessage()).isEqualTo("history-message");
        verify(chatMessageRepository).findRecentMessages(org.mockito.ArgumentMatchers.eq(401L),
            org.mockito.ArgumentMatchers.any());
    }
}
