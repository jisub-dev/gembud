package com.gembud.service;

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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
     * @return message response
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, ChatMessageRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(
            request.getChatRoomId(), userId)) {
            throw new BusinessException(ErrorCode.NOT_CHAT_MEMBER);
        }

        String sanitizedMessage = htmlSanitizer.sanitizeAndLimit(request.getMessage(), 1000);

        switch (chatRoom.getType()) {
            case ROOM_CHAT:
                // Phase 11: Save last 50 ROOM_CHAT messages for evidence (신고 증거)
                ChatMessage roomMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .message(sanitizedMessage)
                    .build();
                roomMessage = chatMessageRepository.save(roomMessage);

                long roomMessageCount = chatMessageRepository.countByChatRoomId(chatRoom.getId());
                if (roomMessageCount > ROOM_CHAT_MESSAGE_LIMIT) {
                    chatMessageRepository.deleteOldMessages(
                        chatRoom.getId(), ROOM_CHAT_MESSAGE_LIMIT
                    );
                }
                return ChatMessageResponse.from(roomMessage);

            case GROUP_CHAT:
                ChatMessage groupMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .message(sanitizedMessage)
                    .build();
                groupMessage = chatMessageRepository.save(groupMessage);

                long messageCount = chatMessageRepository.countByChatRoomId(chatRoom.getId());
                if (messageCount > GROUP_CHAT_MESSAGE_LIMIT) {
                    chatMessageRepository.deleteOldMessages(
                        chatRoom.getId(), GROUP_CHAT_MESSAGE_LIMIT
                    );
                }
                return ChatMessageResponse.from(groupMessage);

            case DIRECT_CHAT:
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

        chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)) {
            throw new BusinessException(ErrorCode.NOT_CHAT_MEMBER);
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages = chatMessageRepository.findRecentMessages(chatRoomId, pageable);

        return messages.stream()
            .map(ChatMessageResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Get recent messages by chat room public ID (or legacy numeric ID string).
     *
     * @param chatPublicId chat room public ID
     * @param userId user ID
     * @param limit maximum number of messages
     * @return list of messages
     */
    public List<ChatMessageResponse> getRecentMessagesByPublicId(
        String chatPublicId,
        Long userId,
        int limit
    ) {
        ChatRoom chatRoom = resolveChatRoomByPublicId(chatPublicId);
        return getRecentMessages(chatRoom.getId(), userId, limit);
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
        int deletedCount = chatMessageRepository.deleteOldRoomChatMessages(sevenDaysAgo);
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired ROOM_CHAT messages", deletedCount);
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
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        ChatRoom chatRoom = ChatRoom.builder()
            .type(ChatRoom.ChatRoomType.ROOM_CHAT)
            .relatedRoom(room)
            .build();
        chatRoom = chatRoomRepository.save(chatRoom);

        return chatRoom.getId();
    }

    /**
     * Add a member to a chat room (external API, with authorization check).
     *
     * @param chatRoomId  chat room ID
     * @param userId      user ID to add
     * @param requesterId ID of the user making the request
     */
    @Transactional
    public void addMemberToChatRoom(Long chatRoomId, Long userId, Long requesterId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        switch (chatRoom.getType()) {
            case ROOM_CHAT:
            case DIRECT_CHAT:
                throw new BusinessException(ErrorCode.CHAT_ADD_MEMBER_FORBIDDEN);
            case GROUP_CHAT:
                if (chatRoom.getCreatedBy() == null ||
                    !chatRoom.getCreatedBy().getId().equals(requesterId)) {
                    throw new BusinessException(ErrorCode.CHAT_ADD_MEMBER_FORBIDDEN);
                }
                break;
            default:
                throw new BusinessException(ErrorCode.UNKNOWN_CHAT_ROOM_TYPE);
        }

        addMemberToChatRoomInternal(chatRoomId, userId);
    }

    /**
     * Add a member to a chat room (internal use, no authorization check).
     *
     * @param chatRoomId chat room ID
     * @param userId     user ID
     */
    @Transactional
    void addMemberToChatRoomInternal(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)) {
            return;
        }

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
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
            .ifPresent(member -> {
                chatRoomMemberRepository.delete(member);

                long remainingMembers = chatRoomMemberRepository.countByChatRoomId(chatRoomId);
                if (remainingMembers == 0 && shouldDeleteChatRoomWhenEmpty(chatRoom)) {
                    chatRoomRepository.delete(chatRoom);
                }
            });
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
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    /**
     * Get chat room public ID by game room ID.
     *
     * For ROOM_CHAT this currently uses related Room publicId.
     *
     * @param roomId game room ID
     * @return chat room public ID
     */
    public String getChatRoomPublicIdByGameRoomId(Long roomId) {
        return chatRoomRepository.findByRelatedRoomId(roomId)
            .map(chatRoom -> chatRoom.getRelatedRoom().getPublicId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    /**
     * Check if a user is a member of a chat room.
     *
     * @param chatRoomId chat room ID
     * @param userId user ID
     * @return true if user is a member
     */
    public boolean isChatRoomMember(Long chatRoomId, Long userId) {
        return chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId);
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
        User user1 = userRepository.findById(userId1)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User user2 = userRepository.findById(userId2)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = ChatRoom.builder()
            .type(ChatRoom.ChatRoomType.DIRECT_CHAT)
            .build();
        chatRoom = chatRoomRepository.save(chatRoom);

        chatRoomMemberRepository.save(ChatRoomMember.builder().chatRoom(chatRoom).user(user1).build());
        chatRoomMemberRepository.save(ChatRoomMember.builder().chatRoom(chatRoom).user(user2).build());

        return chatRoom.getId();
    }

    /**
     * Get all chat rooms the user is a member of.
     *
     * @param userId user ID
     * @return list of chat rooms
     */
    public List<ChatRoom> getMyChatRooms(Long userId) {
        return chatRoomMemberRepository.findChatRoomsByUserId(userId).stream()
            .map(ChatRoomMember::getChatRoom)
            .collect(Collectors.toList());
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
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = ChatRoom.builder()
            .type(ChatRoom.ChatRoomType.GROUP_CHAT)
            .name(name)
            .createdBy(creator)
            .build();
        chatRoom = chatRoomRepository.save(chatRoom);

        chatRoomMemberRepository.save(ChatRoomMember.builder().chatRoom(chatRoom).user(creator).build());

        return chatRoom.getId();
    }

    private boolean shouldDeleteChatRoomWhenEmpty(ChatRoom chatRoom) {
        return chatRoom.getType() == ChatRoom.ChatRoomType.ROOM_CHAT
            || chatRoom.getType() == ChatRoom.ChatRoomType.GROUP_CHAT;
    }

    private ChatRoom resolveChatRoomByPublicId(String chatPublicId) {
        return roomRepository.findByPublicId(chatPublicId)
            .flatMap(room -> chatRoomRepository.findByRelatedRoomId(room.getId()))
            .or(() -> {
                try {
                    return chatRoomRepository.findById(Long.parseLong(chatPublicId));
                } catch (NumberFormatException ignored) {
                    return java.util.Optional.empty();
                }
            })
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }
}
