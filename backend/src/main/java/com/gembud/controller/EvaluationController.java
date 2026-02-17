package com.gembud.controller;

import com.gembud.dto.request.EvaluationRequest;
import com.gembud.dto.response.EvaluationResponse;
import com.gembud.service.EvaluationService;
import com.gembud.service.TemperatureService;
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
    @PostMapping("/rooms/{roomId}")
    public ResponseEntity<EvaluationResponse> createEvaluation(
        @PathVariable Long roomId,
        @Valid @RequestBody EvaluationRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        EvaluationResponse response = evaluationService.createEvaluation(
            roomId,
            request,
            userDetails.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all evaluations for a room.
     *
     * @param roomId room ID
     * @return list of evaluations
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<List<EvaluationResponse>> getEvaluationsByRoom(
        @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(evaluationService.getEvaluationsByRoom(roomId));
    }

    /**
     * Get participants that can be evaluated in a room.
     *
     * @param roomId room ID
     * @param userDetails authenticated user
     * @return list of user IDs
     */
    @GetMapping("/rooms/{roomId}/evaluatable")
    public ResponseEntity<List<Long>> getEvaluatableParticipants(
        @PathVariable Long roomId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<Long> participants = evaluationService.getEvaluatableParticipants(
            roomId,
            userDetails.getUsername()
        );
        return ResponseEntity.ok(participants);
    }

    /**
     * Get all evaluations received by a user.
     *
     * @param userId user ID
     * @return list of evaluations
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<EvaluationResponse>> getEvaluationsReceived(
        @PathVariable Long userId
    ) {
        return ResponseEntity.ok(evaluationService.getEvaluationsReceived(userId));
    }

    /**
     * Get temperature statistics for a user.
     *
     * @param userId user ID
     * @return temperature stats
     */
    @GetMapping("/users/{userId}/temperature")
    public ResponseEntity<TemperatureService.TemperatureStats> getTemperatureStats(
        @PathVariable Long userId
    ) {
        return ResponseEntity.ok(temperatureService.getTemperatureStats(userId));
    }
}
