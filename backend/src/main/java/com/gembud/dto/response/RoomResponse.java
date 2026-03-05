package com.gembud.dto.response;

import com.gembud.entity.Room;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Room.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private Long id;
    private String publicId;
    private Long gameId;
    private String gameName;
    private String title;
    private String description;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Boolean isPrivate;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<ParticipantInfo> participants;
    private Map<String, String> filters;
    private String inviteCode;

    /**
     * Convert Room entity to RoomResponse.
     *
     * @param room room entity
     * @return room response
     */
    public static RoomResponse from(Room room) {
        return RoomResponse.builder()
            .id(room.getId())
            .publicId(room.getPublicId())
            .gameId(room.getGame().getId())
            .gameName(room.getGame().getName())
            .title(room.getTitle())
            .description(room.getDescription())
            .maxParticipants(room.getMaxParticipants())
            .currentParticipants(room.getCurrentParticipants())
            .isPrivate(room.getIsPrivate())
            .status(room.getStatus().name())
            .createdBy(room.getCreatedBy().getNickname())
            .createdAt(room.getCreatedAt())
            .build();
    }

    /**
     * Participant info DTO.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo {
        private Long userId;
        private String nickname;
        private Boolean isHost;
    }
}
