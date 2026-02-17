package com.gembud.service;

import com.gembud.dto.request.CreateRoomRequest;
import com.gembud.dto.request.JoinRoomRequest;
import com.gembud.dto.response.RoomResponse;
import com.gembud.entity.Game;
import com.gembud.entity.Room;
import com.gembud.entity.RoomFilter;
import com.gembud.entity.RoomParticipant;
import com.gembud.entity.User;
import com.gembud.repository.GameRepository;
import com.gembud.repository.RoomFilterRepository;
import com.gembud.repository.RoomParticipantRepository;
import com.gembud.repository.RoomRepository;
import com.gembud.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Game game = gameRepository.findById(request.getGameId())
            .orElseThrow(() -> new IllegalArgumentException("Game not found"));

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
            .password(encodedPassword)
            .createdBy(user)
            .build();

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

        return buildRoomResponse(room);
    }

    /**
     * Get rooms by game.
     *
     * @param gameId game ID
     * @return list of room responses
     */
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByGame(Long gameId) {
        return roomRepository.findByGameIdAndStatus(gameId, Room.RoomStatus.OPEN).stream()
            .map(this::buildRoomResponse)
            .collect(Collectors.toList());
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
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));
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
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Check if room is full
        if (room.getStatus() == Room.RoomStatus.FULL) {
            throw new IllegalStateException("Room is full");
        }

        // Check if room is closed
        if (room.getStatus() == Room.RoomStatus.CLOSED) {
            throw new IllegalStateException("Room is closed");
        }

        // Check if already in room
        if (participantRepository.findByRoomIdAndUserId(roomId, user.getId()).isPresent()) {
            throw new IllegalStateException("Already in this room");
        }

        // Verify password if private
        if (Boolean.TRUE.equals(room.getIsPrivate()) && room.getPassword() != null) {
            if (request.getPassword() == null ||
                !passwordEncoder.matches(request.getPassword(), room.getPassword())) {
                throw new IllegalArgumentException("Invalid password");
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
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        RoomParticipant participant = participantRepository.findByRoomIdAndUserId(roomId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Not in this room"));

        boolean wasHost = participant.getIsHost();

        // Remove participant
        participantRepository.delete(participant);
        room.decrementParticipants();

        // If no participants left, close room
        if (room.getCurrentParticipants() == 0) {
            room.close();
            roomRepository.save(room);
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
    }

    /**
     * Close a room (host only).
     *
     * @param roomId room ID
     * @param userEmail current user email
     */
    @Transactional
    public void closeRoom(Long roomId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        RoomParticipant participant = participantRepository.findByRoomIdAndUserId(roomId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Not in this room"));

        if (!participant.getIsHost()) {
            throw new IllegalArgumentException("Only host can close the room");
        }

        room.close();
        roomRepository.save(room);
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
            .build();
    }
}
