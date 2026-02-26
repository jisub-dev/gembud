package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.request.FriendRequest;
import com.gembud.dto.response.FriendResponse;
import com.gembud.service.FriendService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for friend operations.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Tag(name = "Friend", description = "친구 관리 API")
@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /**
     * Send friend request.
     *
     * @param userDetails authenticated user
     * @param request friend request
     * @return created friend relationship
     */
    @Operation(summary = "Send friend request", description = "친구 요청 전송")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "친구 요청 전송 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 친구 또는 요청이 존재함")
    })
    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<FriendResponse>> sendFriendRequest(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody FriendRequest request
    ) {
        FriendResponse response = friendService.sendFriendRequest(
            userDetails.getUsername(),
            request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * Accept friend request.
     *
     * @param userDetails authenticated user
     * @param requestId friend request ID
     * @return updated friend relationship
     */
    @Operation(summary = "Accept friend request", description = "친구 요청 수락")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "친구 요청 수락 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음")
    })
    @PutMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<FriendResponse>> acceptFriendRequest(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long requestId
    ) {
        FriendResponse response = friendService.acceptFriendRequest(
            userDetails.getUsername(),
            requestId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Reject friend request.
     *
     * @param userDetails authenticated user
     * @param requestId friend request ID
     * @return updated friend relationship
     */
    @Operation(summary = "Reject friend request", description = "친구 요청 거절")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "친구 요청 거절 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음")
    })
    @PutMapping("/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<FriendResponse>> rejectFriendRequest(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long requestId
    ) {
        FriendResponse response = friendService.rejectFriendRequest(
            userDetails.getUsername(),
            requestId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Unfriend (remove friend).
     *
     * @param userDetails authenticated user
     * @param friendId friend user ID
     * @return no content
     */
    @Operation(summary = "Unfriend", description = "친구 삭제")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "친구 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "친구 관계를 찾을 수 없음")
    })
    @DeleteMapping("/{friendId}")
    public ResponseEntity<ApiResponse<Void>> unfriend(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long friendId
    ) {
        friendService.unfriend(userDetails.getUsername(), friendId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /**
     * Get my friends list.
     *
     * @param userDetails authenticated user
     * @return list of friends
     */
    @Operation(summary = "Get friends", description = "내 친구 목록 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "친구 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getMyFriends(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<FriendResponse> friends = friendService.getMyFriends(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(friends));
    }

    /**
     * Get pending friend requests (received).
     *
     * @param userDetails authenticated user
     * @return list of pending requests
     */
    @Operation(summary = "Get pending requests", description = "받은 친구 요청 목록 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/requests/pending")
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getPendingRequests(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<FriendResponse> requests = friendService.getPendingRequests(
            userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    /**
     * Get sent friend requests.
     *
     * @param userDetails authenticated user
     * @return list of sent requests
     */
    @Operation(summary = "Get sent requests", description = "보낸 친구 요청 목록 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/requests/sent")
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getSentRequests(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<FriendResponse> requests = friendService.getSentRequests(
            userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success(requests));
    }
}
