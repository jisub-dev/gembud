package com.gembud.service;

import com.gembud.dto.request.CreateRoomRequest;
import com.gembud.dto.request.JoinRoomRequest;
import com.gembud.dto.response.ChatMessageResponse;
import com.gembud.dto.response.RoomResponse;
import com.gembud.entity.Game;
import com.gembud.entity.Room;
import com.gembud.entity.RoomFilter;
import com.gembud.entity.RoomParticipant;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.GameRepository;
import com.gembud.repository.RoomFilterRepository;
import com.gembud.repository.RoomParticipantRepository;
import com.gembud.repository.RoomRepository;
import com.gembud.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for room operations.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final RoomFilterRepository filterRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemperatureService temperatureService;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create a new room.
     *
     * @param request create room request
     * @param userEmail current user email
     * @return created room response
     */
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Phase 11: Check if user is suspended
        if (user.isSuspended()) {
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }

        // Check temperature restriction (< 30°C cannot create rooms)
        if (!temperatureService.canCreateRoom(user.getId())) {
            throw new BusinessException(ErrorCode.LOW_TEMPERATURE);
        }

        // Check if user is already in an active room
        if (participantRepository.existsActiveParticipationByUserId(user.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_IN_OTHER_ROOM);
        }

        Game game = gameRepository.findById(request.getGameId())
            .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // Encode password if private
        String encodedPassword = null;
        if (Boolean.TRUE.equals(request.getIsPrivate()) && request.getPassword() != null) {
            encodedPassword = passwordEncoder.encode(request.getPassword());
        }

        Room room = Room.builder()
            .game(game)
            .title(request.getTitle())
            .description(request.getDescription())
            .maxParticipants(request.getMaxParticipants() != null ? request.getMaxParticipants() : 5)
            .currentParticipants(1)
            .isPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false)
            .passwordHash(encodedPassword)
            .createdBy(user)
            .build();

        // Generate invite code for private rooms (valid 24 hours)
        if (Boolean.TRUE.equals(request.getIsPrivate())) {
            room.generateInviteCode(24);
        }

        roomRepository.save(room);

        // Add creator as host
        RoomParticipant host = RoomParticipant.builder()
            .room(room)
            .user(user)
            .isHost(true)
            .joinOrder(1)
            .build();
        participantRepository.save(host);

        // Add filters
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            List<RoomFilter> filters = request.getFilters().entrySet().stream()
                .map(entry -> RoomFilter.builder()
                    .room(room)
                    .optionKey(entry.getKey())
                    .optionValue(entry.getValue())
                    .build())
                .collect(Collectors.toList());
            filterRepository.saveAll(filters);
        }

        // Create chat room for this game room
        Long chatRoomId = chatService.createChatRoomForGameRoom(room.getId());
        // Add creator as chat room member (internal, no auth check)
        chatService.addMemberToChatRoomInternal(chatRoomId, user.getId());

        return buildRoomResponse(room);
    }

    /**
     * Get rooms the current user is participating in.
     *
     * @param userEmail current user email
     * @return list of room responses
     */
    @Transactional(readOnly = true)
    public List<RoomResponse> getMyRooms(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return participantRepository.findActiveRoomsByUserId(user.getId()).stream()
            .map(rp -> buildRoomResponse(rp.getRoom()))
            .collect(Collectors.toList());
    }

    /**
     * Get rooms by game.
     *
     * @param gameId game ID
     * @return list of room responses
     */
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByGame(Long gameId) {
        return roomRepository.findByGameIdAndStatusAndDeletedAtIsNull(gameId, Room.RoomStatus.OPEN).stream()
            .map(this::buildRoomResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get room by public ID.
     *
     * @param publicId public UUID room identifier
     * @return room response
     */
    @Transactional(readOnly = true)
    public RoomResponse getRoomByPublicId(String publicId) {
        Room room = roomRepository.findByPublicId(publicId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }
        return buildRoomResponse(room);
    }

    /**
     * Get room by ID.
     *
     * @param roomId room ID
     * @return room response
     */
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }
        return buildRoomResponse(room);
    }

    /**
     * Join a room.
     *
     * @param roomId room ID
     * @param request join room request
     * @param userEmail current user email
     * @return updated room response
     */
    @Transactional
    public RoomResponse joinRoom(Long roomId, JoinRoomRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Phase 11: Check if user is suspended
        if (user.isSuspended()) {
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // Guard by both status and count to prevent over-capacity joins on stale status.
        if (room.getStatus() == Room.RoomStatus.FULL
            || room.getCurrentParticipants() >= room.getMaxParticipants()) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }

        // Check if room is closed
        if (room.getStatus() == Room.RoomStatus.CLOSED) {
            throw new BusinessException(ErrorCode.ROOM_CLOSED);
        }

        // Check if already participating in any active room
        if (participantRepository.existsActiveParticipationByUserId(user.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_IN_OTHER_ROOM);
        }

        // Check if already in this specific room
        if (participantRepository.findByRoomIdAndUserId(roomId, user.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_IN_ROOM);
        }

        // Verify password or invite code if private
        if (Boolean.TRUE.equals(room.getIsPrivate())) {
            boolean hasValidInviteCode = request.getInviteCode() != null
                && room.isInviteCodeValid()
                && request.getInviteCode().equals(room.getInviteCode());
            boolean hasValidPassword = request.getPassword() != null
                && room.getPasswordHash() != null
                && passwordEncoder.matches(request.getPassword(), room.getPasswordHash());

            if (!hasValidInviteCode && !hasValidPassword) {
                if (request.getInviteCode() != null) {
                    throw new BusinessException(ErrorCode.INVALID_INVITE_CODE);
                }
                throw new BusinessException(ErrorCode.INVALID_ROOM_PASSWORD);
            }
        }

        // Get next join order
        int nextJoinOrder = (int) participantRepository.countByRoomId(roomId) + 1;

        // Add participant
        RoomParticipant participant = RoomParticipant.builder()
            .room(room)
            .user(user)
            .isHost(false)
            .joinOrder(nextJoinOrder)
            .build();
        participantRepository.save(participant);

        // Update room participant count
        room.incrementParticipants();
        roomRepository.save(room);

        // Add user to chat room (internal, no auth check)
        Long chatRoomId = chatService.getChatRoomByGameRoomId(roomId);
        chatService.addMemberToChatRoomInternal(chatRoomId, user.getId());

        broadcastRoomUpdate(chatRoomId);

        return buildRoomResponse(room);
    }

    /**
     * Leave a room.
     *
     * @param roomId room ID
     * @param userEmail current user email
     */
    @Transactional
    public void leaveRoom(Long roomId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        RoomParticipant participant = participantRepository.findByRoomIdAndUserId(roomId, user.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_IN_ROOM));

        boolean wasHost = participant.getIsHost();

        // Remove participant from game room
        participantRepository.delete(participant);
        room.decrementParticipants();

        // Remove user from chat room
        Long chatRoomId = chatService.getChatRoomByGameRoomId(roomId);
        chatService.removeMemberFromChatRoom(chatRoomId, user.getId());

        // If no participants left, soft-delete room
        if (room.getCurrentParticipants() == 0) {
            room.softDelete();
            roomRepository.save(room);
            broadcastRoomUpdate(chatRoomId);
            return;
        }

        // If host left, transfer to next participant
        if (wasHost) {
            List<RoomParticipant> candidates = participantRepository.findNextHostCandidates(roomId);
            if (!candidates.isEmpty()) {
                RoomParticipant newHost = candidates.get(0);
                participantRepository.delete(newHost);
                RoomParticipant updatedHost = RoomParticipant.builder()
                    .id(newHost.getId())
                    .room(newHost.getRoom())
                    .user(newHost.getUser())
                    .isHost(true)
                    .joinOrder(newHost.getJoinOrder())
                    .joinedAt(newHost.getJoinedAt())
                    .build();
                participantRepository.save(updatedHost);
            }
        }

        roomRepository.save(room);
        broadcastRoomUpdate(chatRoomId);
    }

    /**
     * Kick a participant from a room (host only).
     *
     * @param roomId room ID
     * @param targetUserId user ID to kick
     * @param hostEmail current user email (must be host)
     */
    @Transactional
    public void kickParticipant(Long roomId, Long targetUserId, String hostEmail) {
        User host = userRepository.findByEmail(hostEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        RoomParticipant hostParticipant = participantRepository.findByRoomIdAndUserId(roomId, host.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_IN_ROOM));

        if (!hostParticipant.getIsHost()) {
            throw new BusinessException(ErrorCode.NOT_HOST);
        }

        if (host.getId().equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_HOST);
        }

        RoomParticipant target = participantRepository.findByRoomIdAndUserId(roomId, targetUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_IN_ROOM));

        participantRepository.delete(target);
        room.decrementParticipants();
        roomRepository.save(room);

        Long chatRoomId = chatService.getChatRoomByGameRoomId(roomId);
        chatService.removeMemberFromChatRoom(chatRoomId, targetUserId);
        broadcastRoomUpdate(chatRoomId);
    }

    /**
     * Transfer host to another participant (current host only).
     *
     * @param roomId room ID
     * @param targetUserId user ID to become new host
     * @param hostEmail current host email
     */
    @Transactional
    public void transferHost(Long roomId, Long targetUserId, String hostEmail) {
        User host = userRepository.findByEmail(hostEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        roomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        RoomParticipant hostParticipant = participantRepository.findByRoomIdAndUserId(roomId, host.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_IN_ROOM));

        if (!hostParticipant.getIsHost()) {
            throw new BusinessException(ErrorCode.NOT_HOST);
        }

        if (host.getId().equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_TRANSFER_TO_SELF);
        }

        RoomParticipant target = participantRepository.findByRoomIdAndUserId(roomId, targetUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_IN_ROOM));

        // Demote current host
        participantRepository.delete(hostParticipant);
        RoomParticipant demotedHost = RoomParticipant.builder()
            .id(hostParticipant.getId())
            .room(hostParticipant.getRoom())
            .user(hostParticipant.getUser())
            .isHost(false)
            .joinOrder(hostParticipant.getJoinOrder())
            .joinedAt(hostParticipant.getJoinedAt())
            .build();
        participantRepository.save(demotedHost);

        // Promote target to host
        participantRepository.delete(target);
        RoomParticipant newHost = RoomParticipant.builder()
            .id(target.getId())
            .room(target.getRoom())
            .user(target.getUser())
            .isHost(true)
            .joinOrder(target.getJoinOrder())
            .joinedAt(target.getJoinedAt())
            .build();
        participantRepository.save(newHost);

        Long chatRoomId = chatService.getChatRoomByGameRoomId(roomId);
        broadcastRoomUpdate(chatRoomId);
    }

    /**
     * Start a room (host only). Changes status to IN_PROGRESS.
     *
     * @param roomId room ID
     * @param hostEmail current user email (must be host)
     */
    @Transactional
    public void startRoom(Long roomId, String hostEmail) {
        User host = userRepository.findByEmail(hostEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        RoomParticipant hostParticipant = participantRepository.findByRoomIdAndUserId(roomId, host.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_IN_ROOM));

        if (!hostParticipant.getIsHost()) {
            throw new BusinessException(ErrorCode.NOT_HOST);
        }

        if (room.getStatus() == Room.RoomStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_IN_PROGRESS);
        }

        room.start();
        roomRepository.save(room);
    }

    /**
     * Join a room by public ID.
     *
     * @param publicId public room UUID
     * @param request join room request
     * @param userEmail current user email
     * @return updated room response with chat room ID
     */
    @Transactional
    public JoinRoomResult joinRoomByPublicId(String publicId, JoinRoomRequest request, String userEmail) {
        Room room = roomRepository.findByPublicId(publicId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }
        RoomResponse roomResponse = joinRoom(room.getId(), request, userEmail);
        Long chatRoomId = chatService.getChatRoomByGameRoomId(room.getId());
        return new JoinRoomResult(roomResponse, chatRoomId);
    }

    /**
     * Regenerate invite code for a private room (host only).
     *
     * @param publicId room public ID
     * @param hostEmail current user email (must be host)
     * @return updated room response with new invite code
     */
    @Transactional
    public RoomResponse regenerateInviteCode(String publicId, String hostEmail) {
        User host = userRepository.findByEmail(hostEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Room room = roomRepository.findByPublicId(publicId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        RoomParticipant hostParticipant = participantRepository.findByRoomIdAndUserId(room.getId(), host.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_IN_ROOM));

        if (!hostParticipant.getIsHost()) {
            throw new BusinessException(ErrorCode.NOT_HOST);
        }

        room.regenerateInviteCode(24);
        roomRepository.save(room);
        return buildRoomResponse(room);
    }

    /**
     * Result object for joinRoomByPublicId.
     */
    public record JoinRoomResult(RoomResponse room, Long chatRoomId) {}

    private void broadcastRoomUpdate(Long chatRoomId) {
        messagingTemplate.convertAndSend(
            "/topic/chat/" + chatRoomId,
            ChatMessageResponse.builder()
                .chatRoomId(chatRoomId)
                .type("ROOM_UPDATE")
                .build()
        );
    }

    private RoomResponse buildRoomResponse(Room room) {
        List<RoomParticipant> participants = participantRepository.findByRoomId(room.getId());
        List<RoomFilter> filters = filterRepository.findByRoomId(room.getId());

        List<RoomResponse.ParticipantInfo> participantInfos = participants.stream()
            .map(p -> RoomResponse.ParticipantInfo.builder()
                .userId(p.getUser().getId())
                .nickname(p.getUser().getNickname())
                .isHost(p.getIsHost())
                .build())
            .collect(Collectors.toList());

        Map<String, String> filterMap = filters.stream()
            .collect(Collectors.toMap(RoomFilter::getOptionKey, RoomFilter::getOptionValue));

        RoomResponse response = RoomResponse.from(room);
        return RoomResponse.builder()
            .id(response.getId())
            .publicId(response.getPublicId())
            .gameId(response.getGameId())
            .gameName(response.getGameName())
            .title(response.getTitle())
            .description(response.getDescription())
            .maxParticipants(response.getMaxParticipants())
            .currentParticipants(response.getCurrentParticipants())
            .isPrivate(response.getIsPrivate())
            .status(response.getStatus())
            .createdBy(response.getCreatedBy())
            .createdAt(response.getCreatedAt())
            .participants(participantInfos)
            .filters(filterMap)
            .inviteCode(room.isInviteCodeValid() ? room.getInviteCode() : null)
            .build();
    }
}
