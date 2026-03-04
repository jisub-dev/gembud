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

    /**
     * Find all rooms a user is participating in (non-closed).
     *
     * @param userId user ID
     * @return list of participants with room info
     */
    @Query("SELECT rp FROM RoomParticipant rp JOIN FETCH rp.room r WHERE rp.user.id = :userId AND r.status <> 'CLOSED' ORDER BY rp.joinedAt DESC")
    List<RoomParticipant> findActiveRoomsByUserId(@Param("userId") Long userId);

    /**
     * Find all participants for multiple rooms (batch load).
     *
     * @param roomIds list of room IDs
     * @return list of participants
     */
    List<RoomParticipant> findByRoomIdIn(List<Long> roomIds);

    /**
     * Check if a user is already participating in any non-closed room.
     *
     * @param userId user ID
     * @return true if user is already in an active room
     */
    @Query("SELECT COUNT(rp) > 0 FROM RoomParticipant rp WHERE rp.user.id = :userId AND rp.room.status <> 'CLOSED'")
    boolean existsActiveParticipationByUserId(@Param("userId") Long userId);
}
