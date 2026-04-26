package com.gembud.repository;

import com.gembud.entity.Room;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Room entity.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    /**
     * Find rooms by game ID.
     *
     * @param gameId game ID
     * @return list of rooms
     */
    List<Room> findByGameId(Long gameId);

    /**
     * Find rooms by game ID and status.
     *
     * @param gameId game ID
     * @param status room status
     * @return list of rooms
     */
    List<Room> findByGameIdAndStatus(Long gameId, Room.RoomStatus status);

    /**
     * Find non-deleted rooms by game ID and status.
     *
     * @param gameId game ID
     * @param status room status
     * @return list of rooms
     */
    List<Room> findByGameIdAndStatusAndDeletedAtIsNull(Long gameId, Room.RoomStatus status);

    /**
     * Find non-deleted rooms by game ID and statuses.
     *
     * @param gameId game ID
     * @param statuses allowed room statuses
     * @return list of rooms
     */
    List<Room> findByGameIdAndStatusInAndDeletedAtIsNull(Long gameId, Collection<Room.RoomStatus> statuses);

    /**
     * Find rooms by status.
     *
     * @param status room status
     * @return list of rooms
     */
    List<Room> findByStatus(Room.RoomStatus status);

    /**
     * Find rooms created by user.
     *
     * @param userId user ID
     * @return list of rooms
     */
    @Query("SELECT r FROM Room r WHERE r.createdBy.id = :userId")
    List<Room> findByCreatedBy(@Param("userId") Long userId);

    /**
     * Find room by public ID (UUID string).
     *
     * @param publicId public UUID identifier
     * @return room optional
     */
    Optional<Room> findByPublicId(String publicId);

    /**
     * Find room by ID with pessimistic write lock for capacity-sensitive joins.
     *
     * @param roomId room ID
     * @return locked room optional
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :roomId")
    Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);
}
