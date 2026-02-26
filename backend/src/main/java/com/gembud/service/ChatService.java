package com.gembud.service;

import com.gembud.dto.request.ChatMessageRequest;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.dto.response.ChatMessageResponse;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.entity.ChatMessage;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.entity.ChatRoom;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.entity.ChatRoomMember;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.entity.Room;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.ChatMessageRepository;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.ChatRoomMemberRepository;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.ChatRoomRepository;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.RoomRepository;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.UserRepository;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.util.HtmlSanitizer;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import java.util.Collections;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import java.util.List;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import java.util.stream.Collectors;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import org.springframework.data.domain.Pageable;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import org.springframework.stereotype.Service;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import org.springframework.transaction.annotation.Transactional;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;

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

    private static final int ROOM_CHAT_MESSAGE_LIMIT = 50;  // Phase 11: Keep last 50 for evidence
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
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // Verify user is a member of the chat room
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(
            request.getChatRoomId(), userId)) {
            throw new BusinessException(ErrorCode.NOT_CHAT_MEMBER);
        }

        // Sanitize message to prevent XSS attacks
        String sanitizedMessage = htmlSanitizer.sanitizeAndLimit(request.getMessage(), 1000);

        // Handle message based on chat room type
        switch (chatRoom.getType()) {
            case ROOM_CHAT:
                // Phase 11: Save last 50 ROOM_CHAT messages for evidence (신고 증거)
                ChatMessage roomMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .message(sanitizedMessage)
                    .build();
                roomMessage = chatMessageRepository.save(roomMessage);

                // Delete old messages (keep only last 50)
                long roomMessageCount = chatMessageRepository.countByChatRoomId(chatRoom.getId());
                if (roomMessageCount > ROOM_CHAT_MESSAGE_LIMIT) {
                    chatMessageRepository.deleteOldMessages(
                        chatRoom.getId(), ROOM_CHAT_MESSAGE_LIMIT
                    );
                }

                return ChatMessageResponse.from(roomMessage);

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
                throw new BusinessException(ErrorCode.UNKNOWN_CHAT_ROOM_TYPE);
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
            throw new BusinessException(ErrorCode.NOT_CHAT_MEMBER);
        }

        // Get recent messages (Phase 11: ROOM_CHAT now saves last 50)
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages = chatMessageRepository.findRecentMessages(
            chatRoomId, pageable
        );

        return messages.stream()
            .map(ChatMessageResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Cleanup expired chat messages (Phase 11: Evidence retention).
     * ROOM_CHAT messages older than 7 days are deleted.
     * Runs daily at 3 AM.
     */
    @Transactional
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredMessages() {
        java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7);

        // Delete ROOM_CHAT messages older than 7 days
        int deletedCount = chatMessageRepository.deleteOldRoomChatMessages(sevenDaysAgo);

        if (deletedCount > 0) {
            System.out.println("[ChatService] Cleaned up " + deletedCount + " expired ROOM_CHAT messages");
        }
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
            .relatedRoom(room)
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

    /**
     * Create a direct chat room between two users.
     *
     * @param userId1 first user ID
     * @param userId2 second user ID
     * @return created chat room ID
     */
    @Transactional
    public Long createDirectChatRoom(Long userId1, Long userId2) {
        // Verify users exist
        User user1 = userRepository.findById(userId1)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId1));
        User user2 = userRepository.findById(userId2)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId2));

        // Create DIRECT_CHAT
        ChatRoom chatRoom = ChatRoom.builder()
            .type(ChatRoom.ChatRoomType.DIRECT_CHAT)
            .build();
        chatRoom = chatRoomRepository.save(chatRoom);

        // Add both users as members
        ChatRoomMember member1 = ChatRoomMember.builder()
            .chatRoom(chatRoom)
            .user(user1)
            .build();
        chatRoomMemberRepository.save(member1);

        ChatRoomMember member2 = ChatRoomMember.builder()
            .chatRoom(chatRoom)
            .user(user2)
            .build();
        chatRoomMemberRepository.save(member2);

        return chatRoom.getId();
    }

    /**
     * Create a group chat room.
     *
     * @param name group chat name
     * @param creatorId creator user ID
     * @return created chat room ID
     */
    @Transactional
    public Long createGroupChatRoom(String name, Long creatorId) {
        // Verify creator exists
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new IllegalArgumentException("Creator not found: " + creatorId));

        // Create GROUP_CHAT
        ChatRoom chatRoom = ChatRoom.builder()
            .type(ChatRoom.ChatRoomType.GROUP_CHAT)
            .name(name)
            .build();
        chatRoom = chatRoomRepository.save(chatRoom);

        // Add creator as member
        ChatRoomMember member = ChatRoomMember.builder()
            .chatRoom(chatRoom)
            .user(creator)
            .build();
        chatRoomMemberRepository.save(member);

        return chatRoom.getId();
    }
}
