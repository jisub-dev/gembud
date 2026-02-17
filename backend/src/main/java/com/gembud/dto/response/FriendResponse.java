package com.gembud.dto.response;

import com.gembud.entity.Friend;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for friend.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponse {

    /**
     * Friend relationship ID.
     */
    private Long id;

    /**
     * User ID (requester).
     */
    private Long userId;

    /**
     * User nickname (requester).
     */
    private String userNickname;

    /**
     * Friend ID (receiver or friend).
     */
    private Long friendId;

    /**
     * Friend nickname.
     */
    private String friendNickname;

    /**
     * Friend request status.
     */
    private String status;

    /**
     * Created timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Updated timestamp.
     */
    private LocalDateTime updatedAt;

    /**
     * Convert Friend entity to response DTO.
     *
     * @param friend friend entity
     * @return response DTO
     */
    public static FriendResponse from(Friend friend) {
        return FriendResponse.builder()
            .id(friend.getId())
            .userId(friend.getUser().getId())
            .userNickname(friend.getUser().getNickname())
            .friendId(friend.getFriend().getId())
            .friendNickname(friend.getFriend().getNickname())
            .status(friend.getStatus().name())
            .createdAt(friend.getCreatedAt())
            .updatedAt(friend.getUpdatedAt())
            .build();
    }
}
