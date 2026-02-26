package com.gembud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Standard API response wrapper for all successful responses.
 *
 * Provides consistent response format across all endpoints:
 * - timestamp: when the response was generated
 * - status: HTTP status code
 * - message: human-readable message
 * - data: actual response data (generic type T)
 *
 * @param <T> type of the response data
 * @author Gembud Team
 * @since 2026-02-22 (Phase 2: DTO Standardization)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "표준 API 응답 래퍼")
public class ApiResponse<T> {

    @Schema(description = "응답 생성 시각", example = "2026-02-22T21:37:31")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "HTTP 상태 코드", example = "200")
    private Integer status;

    @Schema(description = "응답 메시지", example = "Success")
    private String message;

    @Schema(description = "응답 데이터")
    private T data;

    /**
     * Creates a successful response with HTTP 200.
     *
     * @param data response data
     * @param <T> type of data
     * @return ApiResponse with status 200
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .timestamp(LocalDateTime.now())
            .status(200)
            .message("Success")
            .data(data)
            .build();
    }

    /**
     * Creates a successful response with HTTP 200 and custom message.
     *
     * @param message custom message
     * @param data response data
     * @param <T> type of data
     * @return ApiResponse with status 200
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .timestamp(LocalDateTime.now())
            .status(200)
            .message(message)
            .data(data)
            .build();
    }

    /**
     * Creates a created response with HTTP 201.
     *
     * @param data response data
     * @param <T> type of data
     * @return ApiResponse with status 201
     */
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
            .timestamp(LocalDateTime.now())
            .status(201)
            .message("Created")
            .data(data)
            .build();
    }

    /**
     * Creates a no content response with HTTP 204.
     * Typically used for DELETE operations.
     *
     * @param <T> type of data
     * @return ApiResponse with status 204 and null data
     */
    public static <T> ApiResponse<T> noContent() {
        return ApiResponse.<T>builder()
            .timestamp(LocalDateTime.now())
            .status(204)
            .message("No Content")
            .data(null)
            .build();
    }
}
