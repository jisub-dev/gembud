package com.gembud.repository;

import com.gembud.entity.RoomFilter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for RoomFilter entity.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Repository
public interface RoomFilterRepository extends JpaRepository<RoomFilter, Long> {

    /**
     * Find all filters for a room.
     *
     * @param roomId room ID
     * @return list of filters
     */
    List<RoomFilter> findByRoomId(Long roomId);

    /**
     * Delete all filters for a room.
     *
     * @param roomId room ID
     */
    void deleteByRoomId(Long roomId);
}
