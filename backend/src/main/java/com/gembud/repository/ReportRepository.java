package com.gembud.repository;

import com.gembud.entity.Report;
import com.gembud.entity.Report.ReportStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Report entity.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Find reports by reporter.
     *
     * @param reporterId reporter ID
     * @return list of reports
     */
    @Query("SELECT r FROM Report r WHERE r.reporter.id = :reporterId ORDER BY r.createdAt DESC")
    List<Report> findByReporterId(@Param("reporterId") Long reporterId);

    /**
     * Find reports by reported user.
     *
     * @param reportedId reported user ID
     * @return list of reports
     */
    @Query("SELECT r FROM Report r WHERE r.reported.id = :reportedId ORDER BY r.createdAt DESC")
    List<Report> findByReportedId(@Param("reportedId") Long reportedId);

    /**
     * Find reports by status.
     *
     * @param status report status
     * @return list of reports
     */
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    /**
     * Find reports by room.
     *
     * @param roomId room ID
     * @return list of reports
     */
    @Query("SELECT r FROM Report r WHERE r.room.id = :roomId ORDER BY r.createdAt DESC")
    List<Report> findByRoomId(@Param("roomId") Long roomId);

    /**
     * Count pending reports for a user.
     *
     * @param reportedId reported user ID
     * @return count
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.reported.id = :reportedId AND r.status = 'PENDING'")
    long countPendingByReportedId(@Param("reportedId") Long reportedId);

    /**
     * Check if user has already reported another user in a room.
     *
     * @param reporterId reporter ID
     * @param reportedId reported user ID
     * @param roomId room ID
     * @return true if report exists
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Report r " +
           "WHERE r.reporter.id = :reporterId AND r.reported.id = :reportedId " +
           "AND r.room.id = :roomId")
    boolean existsByReporterIdAndReportedIdAndRoomId(
        @Param("reporterId") Long reporterId,
        @Param("reportedId") Long reportedId,
        @Param("roomId") Long roomId
    );
}
