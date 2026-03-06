package com.gembud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for admin warning action on report.
 */
@Getter
@Setter
public class AdminWarnReportRequest {

    @NotBlank(message = "warningMessage is required")
    @Size(max = 500, message = "warningMessage must be 500 chars or less")
    private String warningMessage;
}
