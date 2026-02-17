package com.gembud.repository;

import com.gembud.entity.RoomParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for RoomParticipant entity.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {

    /**
     * Find all participants in a room.
     *
     * @param roomId room ID
     * @return list of participants
     */
    List<RoomParticipant> findByRoomId(Long roomId);

    /**
     * Find participant by room and user.
     *
     * @param roomId room ID
     * @param userId user ID
     * @return optional participant
     */
    @Query("SELECT rp FROM RoomParticipant rp WHERE rp.room.id = :roomId AND rp.user.id = :userId")
    Optional<RoomParticipant> findByRoomIdAndUserId(
        @Param("roomId") Long roomId,
        @Param("userId") Long userId
    );

    /**
     * Find host of a room.
     *
     * @param roomId room ID
     * @return optional host participant
     */
    @Query("SELECT rp FROM RoomParticipant rp WHERE rp.room.id = :roomId AND rp.isHost = true")
    Optional<RoomParticipant> findHostByRoomId(@Param("roomId") Long roomId);

    /**
     * Find next host candidate (earliest non-host participant).
     *
     * @param roomId room ID
     * @return optional next host
     */
    @Query("SELECT rp FROM RoomParticipant rp WHERE rp.room.id = :roomId AND rp.isHost = false ORDER BY rp.joinOrder ASC")
    List<RoomParticipant> findNextHostCandidates(@Param("roomId") Long roomId);

    /**
     * Count participants in a room.
     *
     * @param roomId room ID
     * @return participant count
     */
    long countByRoomId(Long roomId);

    /**
     * Delete participant by room and user.
     *
     * @param roomId room ID
     * @param userId user ID
     */
    void deleteByRoomIdAndUserId(Long roomId, Long userId);
}
