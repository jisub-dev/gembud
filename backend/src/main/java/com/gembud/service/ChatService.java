package com.gembud.service;

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
import com.gembud.util.HtmlSanitizer;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for chat operations.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final int GROUP_CHAT_MESSAGE_LIMIT = 100;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final HtmlSanitizer htmlSanitizer;

    /**
     * Send a chat message.
     *
     * @param userId user ID
     * @param request message request
     * @return message response (null if ROOM_CHAT and not saved)
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, ChatMessageRequest request) {
        // Verify user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Verify chat room exists
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Chat room not found: " + request.getChatRoomId()
            ));

        // Verify user is a member of the chat room
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(
            request.getChatRoomId(), userId)) {
            throw new IllegalStateException(
                "User is not a member of this chat room"
            );
        }

        // Sanitize message to prevent XSS attacks
        String sanitizedMessage = htmlSanitizer.sanitizeAndLimit(request.getMessage(), 1000);

        // Handle message based on chat room type
        switch (chatRoom.getType()) {
            case ROOM_CHAT:
                // Don't save ROOM_CHAT messages (real-time only)
                return ChatMessageResponse.builder()
                    .chatRoomId(chatRoom.getId())
                    .userId(user.getId())
                    .username(user.getNickname())
                    .message(sanitizedMessage)
                    .build();

            case GROUP_CHAT:
                // Save GROUP_CHAT message
                ChatMessage groupMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .message(sanitizedMessage)
                    .build();
                groupMessage = chatMessageRepository.save(groupMessage);

                // Delete old messages (keep only last 100)
                long messageCount = chatMessageRepository.countByChatRoomId(chatRoom.getId());
                if (messageCount > GROUP_CHAT_MESSAGE_LIMIT) {
                    chatMessageRepository.deleteOldMessages(
                        chatRoom.getId(), GROUP_CHAT_MESSAGE_LIMIT
                    );
                }

                return ChatMessageResponse.from(groupMessage);

            case DIRECT_CHAT:
                // Save DIRECT_CHAT message (keep all)
                ChatMessage directMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .message(sanitizedMessage)
                    .build();
                directMessage = chatMessageRepository.save(directMessage);

                return ChatMessageResponse.from(directMessage);

            default:
                throw new IllegalStateException("Unknown chat room type: " + chatRoom.getType());
        }
    }

    /**
     * Get recent messages from a chat room.
     *
     * @param chatRoomId chat room ID
     * @param userId user ID
     * @param limit maximum number of messages
     * @return list of messages
     */
    public List<ChatMessageResponse> getRecentMessages(
        Long chatRoomId, Long userId, int limit) {

        // Verify chat room exists
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + chatRoomId));

        // Verify user is a member of the chat room
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)) {
            throw new IllegalStateException("User is not a member of this chat room");
        }

        // ROOM_CHAT messages are not saved
        if (chatRoom.getType() == ChatRoom.ChatRoomType.ROOM_CHAT) {
            return Collections.emptyList();
        }

        // Get recent messages for GROUP_CHAT and DIRECT_CHAT
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages = chatMessageRepository.findRecentMessages(
            chatRoomId, pageable
        );

        return messages.stream()
            .map(ChatMessageResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Create a chat room for a game room.
     *
     * @param roomId game room ID
     * @return created chat room ID
     */
    @Transactional
    public Long createChatRoomForGameRoom(Long roomId) {
        // Verify room exists
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        // Create ROOM_CHAT
        ChatRoom chatRoom = ChatRoom.builder()
            .type(ChatRoom.ChatRoomType.ROOM_CHAT)
            .relatedRoomId(roomId)
            .build();
        chatRoom = chatRoomRepository.save(chatRoom);

        return chatRoom.getId();
    }

    /**
     * Add a member to a chat room.
     *
     * @param chatRoomId chat room ID
     * @param userId user ID
     */
    @Transactional
    public void addMemberToChatRoom(Long chatRoomId, Long userId) {
        // Verify chat room exists
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + chatRoomId));

        // Verify user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Check if already a member
        if (chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)) {
            return; // Already a member
        }

        // Add member
        ChatRoomMember member = ChatRoomMember.builder()
            .chatRoom(chatRoom)
            .user(user)
            .build();
        chatRoomMemberRepository.save(member);
    }

    /**
     * Remove a member from a chat room.
     *
     * @param chatRoomId chat room ID
     * @param userId user ID
     */
    @Transactional
    public void removeMemberFromChatRoom(Long chatRoomId, Long userId) {
        // Find and delete the membership
        chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
            .ifPresent(chatRoomMemberRepository::delete);
    }

    /**
     * Get chat room by game room ID.
     *
     * @param roomId game room ID
     * @return chat room ID
     */
    public Long getChatRoomByGameRoomId(Long roomId) {
        return chatRoomRepository.findByRelatedRoomId(roomId)
            .map(ChatRoom::getId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Chat room not found for game room: " + roomId
            ));
    }
}
