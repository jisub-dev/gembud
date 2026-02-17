package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.dto.request.ChatMessageRequest;
import com.gembud.dto.response.ChatMessageResponse;
import com.gembud.entity.ChatMessage;
import com.gembud.entity.ChatRoom;
import com.gembud.entity.ChatRoomMember;
import com.gembud.entity.Room;
import com.gembud.entity.User;
import com.gembud.repository.ChatMessageRepository;
import com.gembud.repository.ChatRoomMemberRepository;
import com.gembud.repository.ChatRoomRepository;
import com.gembud.repository.RoomRepository;
import com.gembud.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Tests for ChatService.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
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

    @InjectMocks
    private ChatService chatService;

    private User testUser;
    private ChatRoom roomChatRoom;
    private ChatRoom groupChatRoom;
    private ChatRoom directChatRoom;
    private ChatMessageRequest messageRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .nickname("TestUser")
            .temperature(new BigDecimal("36.5"))
            .build();

        roomChatRoom = ChatRoom.builder()
            .id(1L)
            .type(ChatRoom.ChatRoomType.ROOM_CHAT)
            .relatedRoomId(100L)
            .build();

        groupChatRoom = ChatRoom.builder()
            .id(2L)
            .type(ChatRoom.ChatRoomType.GROUP_CHAT)
            .name("Test Group")
            .build();

        directChatRoom = ChatRoom.builder()
            .id(3L)
            .type(ChatRoom.ChatRoomType.DIRECT_CHAT)
            .build();

        messageRequest = ChatMessageRequest.builder()
            .chatRoomId(1L)
            .message("Hello, World!")
            .build();
    }

    @Test
    @DisplayName("sendMessage - ROOM_CHAT should not save message")
    void sendMessage_RoomChat_ShouldNotSave() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(roomChatRoom));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(1L, 1L)).thenReturn(true);

        // When
        ChatMessageResponse response = chatService.sendMessage(1L, messageRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getChatRoomId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("TestUser");
        assertThat(response.getMessage()).isEqualTo("Hello, World!");
        assertThat(response.getId()).isNull(); // Not saved

        // Verify message was not saved
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendMessage - GROUP_CHAT should save message")
    void sendMessage_GroupChat_ShouldSave() {
        // Given
        messageRequest = ChatMessageRequest.builder()
            .chatRoomId(2L)
            .message("Group message")
            .build();

        ChatMessage savedMessage = ChatMessage.builder()
            .id(10L)
            .chatRoom(groupChatRoom)
            .user(testUser)
            .message("Group message")
            .createdAt(LocalDateTime.now())
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(2L)).thenReturn(Optional.of(groupChatRoom));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(2L, 1L)).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
        when(chatMessageRepository.countByChatRoomId(2L)).thenReturn(50L);

        // When
        ChatMessageResponse response = chatService.sendMessage(1L, messageRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getChatRoomId()).isEqualTo(2L);
        assertThat(response.getMessage()).isEqualTo("Group message");

        // Verify message was saved
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(chatMessageRepository, never()).deleteOldMessages(anyLong(), any(Integer.class));
    }

    @Test
    @DisplayName("sendMessage - GROUP_CHAT should delete old messages when exceeding limit")
    void sendMessage_GroupChat_ShouldDeleteOldMessages() {
        // Given
        messageRequest = ChatMessageRequest.builder()
            .chatRoomId(2L)
            .message("New message")
            .build();

        ChatMessage savedMessage = ChatMessage.builder()
            .id(101L)
            .chatRoom(groupChatRoom)
            .user(testUser)
            .message("New message")
            .createdAt(LocalDateTime.now())
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(2L)).thenReturn(Optional.of(groupChatRoom));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(2L, 1L)).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
        when(chatMessageRepository.countByChatRoomId(2L)).thenReturn(101L); // Exceeds limit

        // When
        chatService.sendMessage(1L, messageRequest);

        // Then
        verify(chatMessageRepository, times(1)).deleteOldMessages(2L, 100);
    }

    @Test
    @DisplayName("sendMessage - DIRECT_CHAT should save all messages")
    void sendMessage_DirectChat_ShouldSaveAll() {
        // Given
        messageRequest = ChatMessageRequest.builder()
            .chatRoomId(3L)
            .message("Direct message")
            .build();

        ChatMessage savedMessage = ChatMessage.builder()
            .id(20L)
            .chatRoom(directChatRoom)
            .user(testUser)
            .message("Direct message")
            .createdAt(LocalDateTime.now())
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(3L)).thenReturn(Optional.of(directChatRoom));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(3L, 1L)).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        // When
        ChatMessageResponse response = chatService.sendMessage(1L, messageRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getMessage()).isEqualTo("Direct message");

        // Verify message was saved and no cleanup
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(chatMessageRepository, never()).deleteOldMessages(anyLong(), any(Integer.class));
    }

    @Test
    @DisplayName("sendMessage - should throw exception when user not found")
    void sendMessage_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.sendMessage(1L, messageRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User not found: 1");
    }

    @Test
    @DisplayName("sendMessage - should throw exception when chat room not found")
    void sendMessage_ChatRoomNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.sendMessage(1L, messageRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Chat room not found: 1");
    }

    @Test
    @DisplayName("sendMessage - should throw exception when user is not a member")
    void sendMessage_UserNotMember_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(roomChatRoom));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(1L, 1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> chatService.sendMessage(1L, messageRequest))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("User is not a member of this chat room");
    }

    @Test
    @DisplayName("getRecentMessages - ROOM_CHAT should return empty list")
    void getRecentMessages_RoomChat_ShouldReturnEmpty() {
        // Given
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(roomChatRoom));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(1L, 1L)).thenReturn(true);

        // When
        List<ChatMessageResponse> messages = chatService.getRecentMessages(1L, 1L, 50);

        // Then
        assertThat(messages).isEmpty();
        verify(chatMessageRepository, never()).findRecentMessages(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("getRecentMessages - GROUP_CHAT should return recent messages")
    void getRecentMessages_GroupChat_ShouldReturnMessages() {
        // Given
        ChatMessage msg1 = ChatMessage.builder()
            .id(1L)
            .chatRoom(groupChatRoom)
            .user(testUser)
            .message("Message 1")
            .createdAt(LocalDateTime.now().minusMinutes(10))
            .build();

        ChatMessage msg2 = ChatMessage.builder()
            .id(2L)
            .chatRoom(groupChatRoom)
            .user(testUser)
            .message("Message 2")
            .createdAt(LocalDateTime.now().minusMinutes(5))
            .build();

        when(chatRoomRepository.findById(2L)).thenReturn(Optional.of(groupChatRoom));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(2L, 1L)).thenReturn(true);
        when(chatMessageRepository.findRecentMessages(eq(2L), any(Pageable.class)))
            .thenReturn(Arrays.asList(msg2, msg1)); // Most recent first

        // When
        List<ChatMessageResponse> messages = chatService.getRecentMessages(2L, 1L, 50);

        // Then
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getMessage()).isEqualTo("Message 2");
        assertThat(messages.get(1).getMessage()).isEqualTo("Message 1");

        verify(chatMessageRepository, times(1))
            .findRecentMessages(2L, PageRequest.of(0, 50));
    }

    @Test
    @DisplayName("getRecentMessages - should throw exception when chat room not found")
    void getRecentMessages_ChatRoomNotFound_ShouldThrowException() {
        // Given
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.getRecentMessages(1L, 1L, 50))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Chat room not found: 1");
    }

    @Test
    @DisplayName("getRecentMessages - should throw exception when user is not a member")
    void getRecentMessages_UserNotMember_ShouldThrowException() {
        // Given
        when(chatRoomRepository.findById(2L)).thenReturn(Optional.of(groupChatRoom));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(2L, 1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> chatService.getRecentMessages(2L, 1L, 50))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("User is not a member of this chat room");
    }

    @Test
    @DisplayName("createChatRoomForGameRoom - should create ROOM_CHAT")
    void createChatRoomForGameRoom_ShouldCreateRoomChat() {
        // Given
        Room gameRoom = Room.builder()
            .id(100L)
            .title("Test Game Room")
            .build();

        ChatRoom createdChatRoom = ChatRoom.builder()
            .id(50L)
            .type(ChatRoom.ChatRoomType.ROOM_CHAT)
            .relatedRoomId(100L)
            .build();

        when(roomRepository.findById(100L)).thenReturn(Optional.of(gameRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(createdChatRoom);

        // When
        Long chatRoomId = chatService.createChatRoomForGameRoom(100L);

        // Then
        assertThat(chatRoomId).isEqualTo(50L);
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("createChatRoomForGameRoom - should throw exception when room not found")
    void createChatRoomForGameRoom_RoomNotFound_ShouldThrowException() {
        // Given
        when(roomRepository.findById(100L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.createChatRoomForGameRoom(100L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Room not found: 100");
    }

    @Test
    @DisplayName("addMemberToChatRoom - should add new member")
    void addMemberToChatRoom_ShouldAddMember() {
        // Given
        when(chatRoomRepository.findById(2L)).thenReturn(Optional.of(groupChatRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(2L, 1L)).thenReturn(false);

        // When
        chatService.addMemberToChatRoom(2L, 1L);

        // Then
        verify(chatRoomMemberRepository, times(1)).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("addMemberToChatRoom - should not add duplicate member")
    void addMemberToChatRoom_DuplicateMember_ShouldNotAdd() {
        // Given
        when(chatRoomRepository.findById(2L)).thenReturn(Optional.of(groupChatRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(2L, 1L)).thenReturn(true);

        // When
        chatService.addMemberToChatRoom(2L, 1L);

        // Then
        verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("addMemberToChatRoom - should throw exception when chat room not found")
    void addMemberToChatRoom_ChatRoomNotFound_ShouldThrowException() {
        // Given
        when(chatRoomRepository.findById(2L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.addMemberToChatRoom(2L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Chat room not found: 2");
    }

    @Test
    @DisplayName("removeMemberFromChatRoom - should remove member")
    void removeMemberFromChatRoom_ShouldRemoveMember() {
        // Given
        ChatRoomMember member = ChatRoomMember.builder()
            .id(1L)
            .chatRoom(groupChatRoom)
            .user(testUser)
            .build();

        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(2L, 1L))
            .thenReturn(Optional.of(member));

        // When
        chatService.removeMemberFromChatRoom(2L, 1L);

        // Then
        verify(chatRoomMemberRepository, times(1)).delete(member);
    }

    @Test
    @DisplayName("removeMemberFromChatRoom - should not throw when member not found")
    void removeMemberFromChatRoom_MemberNotFound_ShouldNotThrow() {
        // Given
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(2L, 1L))
            .thenReturn(Optional.empty());

        // When & Then - should not throw
        chatService.removeMemberFromChatRoom(2L, 1L);

        verify(chatRoomMemberRepository, never()).delete(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("getChatRoomByGameRoomId - should return chat room ID")
    void getChatRoomByGameRoomId_ShouldReturnChatRoomId() {
        // Given
        when(chatRoomRepository.findByRelatedRoomId(100L))
            .thenReturn(Optional.of(roomChatRoom));

        // When
        Long chatRoomId = chatService.getChatRoomByGameRoomId(100L);

        // Then
        assertThat(chatRoomId).isEqualTo(1L);
    }

    @Test
    @DisplayName("getChatRoomByGameRoomId - should throw exception when chat room not found")
    void getChatRoomByGameRoomId_NotFound_ShouldThrowException() {
        // Given
        when(chatRoomRepository.findByRelatedRoomId(100L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.getChatRoomByGameRoomId(100L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Chat room not found for game room: 100");
    }
}
