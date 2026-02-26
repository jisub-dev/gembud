package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.request.EvaluationRequest;
import com.gembud.dto.response.EvaluationResponse;
import com.gembud.service.EvaluationService;
import com.gembud.service.TemperatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for evaluation endpoints.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Tag(name = "Evaluation", description = "평가 및 온도 관리 API")
@RestController
@RequestMapping("/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final TemperatureService temperatureService;

    /**
     * Create an evaluation for a room participant.
     *
     * @param roomId room ID
     * @param request evaluation request
     * @param userDetails authenticated user
     * @return created evaluation
     */
    @Operation(summary = "Create evaluation", description = "방 참가자 평가 생성")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "평가 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "낮은 온도로 평가 불가"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "방을 찾을 수 없음")
    })
    @PostMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<EvaluationResponse>> createEvaluation(
        @PathVariable Long roomId,
        @Valid @RequestBody EvaluationRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        EvaluationResponse response = evaluationService.createEvaluation(
            roomId,
            request,
            userDetails.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * Get all evaluations for a room.
     *
     * @param roomId room ID
     * @return list of evaluations
     */
    @Operation(summary = "Get room evaluations", description = "방의 모든 평가 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "평가 목록 조회 성공")
    })
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<List<EvaluationResponse>>> getEvaluationsByRoom(
        @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getEvaluationsByRoom(roomId)));
    }

    /**
     * Get participants that can be evaluated in a room.
     *
     * @param roomId room ID
     * @param userDetails authenticated user
     * @return list of user IDs
     */
    @Operation(summary = "Get evaluatable participants", description = "평가 가능한 참가자 목록 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참가자 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/rooms/{roomId}/evaluatable")
    public ResponseEntity<ApiResponse<List<Long>>> getEvaluatableParticipants(
        @PathVariable Long roomId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<Long> participants = evaluationService.getEvaluatableParticipants(
            roomId,
            userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success(participants));
    }

    /**
     * Get all evaluations received by a user.
     *
     * @param userId user ID
     * @return list of evaluations
     */
    @Operation(summary = "Get user evaluations", description = "사용자가 받은 모든 평가 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "평가 목록 조회 성공")
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<EvaluationResponse>>> getEvaluationsReceived(
        @PathVariable Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getEvaluationsReceived(userId)));
    }

    /**
     * Get temperature statistics for a user.
     *
     * @param userId user ID
     * @return temperature stats
     */
    @Operation(summary = "Get temperature stats", description = "사용자의 온도 통계 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "온도 통계 조회 성공")
    })
    @GetMapping("/users/{userId}/temperature")
    public ResponseEntity<ApiResponse<TemperatureService.TemperatureStats>> getTemperatureStats(
        @PathVariable Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(temperatureService.getTemperatureStats(userId)));
    }
}
