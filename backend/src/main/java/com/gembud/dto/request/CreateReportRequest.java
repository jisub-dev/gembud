package com.gembud.dto.request;

import com.gembud.entity.Report.ReportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a report.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {

    /**
     * Reported user ID.
     */
    @NotNull(message = "Reported user ID is required")
    private Long reportedId;

    /**
     * Room ID (nullable).
     */
    private Long roomId;

    /**
     * Report category (Phase 11).
     */
    @NotNull(message = "Category is required")
    private ReportCategory category;

    /**
     * Report reason.
     */
    @NotBlank(message = "Reason is required")
    private String reason;

    /**
     * Detailed description.
     */
    private String description;
}
