package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.entity.ChatRoom;
import com.gembud.entity.ChatRoomMember;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.ChatMessageRepository;
import com.gembud.repository.ChatRoomMemberRepository;
import com.gembud.repository.ChatRoomRepository;
import com.gembud.repository.RoomRepository;
import com.gembud.repository.UserRepository;
import com.gembud.util.HtmlSanitizer;
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
}
