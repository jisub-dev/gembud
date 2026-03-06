package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.request.CreateRoomRequest;
import com.gembud.dto.request.JoinRoomRequest;
import com.gembud.dto.response.RoomResponse;
import com.gembud.service.RoomService;
import com.gembud.service.RoomService.JoinRoomResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for room endpoints.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Tag(name = "Room", description = "방 관리 API")
@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * Create a new room.
     *
     * @param request create room request
     * @param userDetails authenticated user
     * @return created room
     */
    @Operation(summary = "Create room", description = "새로운 방 생성")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "방 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "낮은 온도로 방 생성 불가")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
        @Valid @RequestBody CreateRoomRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        RoomResponse response = roomService.createRoom(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * Get rooms the current user is participating in.
     *
     * @param userDetails authenticated user
     * @return list of rooms
     */
    @Operation(summary = "Get my rooms", description = "내가 참여 중인 방 목록 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "방 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getMyRooms(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getMyRooms(userDetails.getUsername())));
    }

    /**
     * Get rooms by game.
     *
     * @param gameId game ID
     * @return list of rooms
     */
    @Operation(summary = "Get rooms by game", description = "게임별 방 목록 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "방 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByGame(
        @RequestParam Long gameId
    ) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoomsByGame(gameId)));
    }

    /**
     * Get room by numeric ID (legacy).
     *
     * @param roomId room numeric ID
     * @return room details
     */
    @Operation(summary = "Get room by ID", description = "방 상세 정보 조회 (numeric ID)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "방 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "방을 찾을 수 없음")
    })
    @GetMapping("/id/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Long roomId) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoomById(roomId)));
    }

    /**
     * Get room by public ID (UUID).
     *
     * @param publicId room public UUID
     * @return room details
     */
    @Operation(summary = "Get room by public ID", description = "방 상세 정보 조회 (UUID)")
    @GetMapping("/{publicId}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomByPublicId(@PathVariable String publicId) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoomByPublicId(publicId)));
    }

    /**
     * Join a room by numeric ID (legacy).
     *
     * @param roomId room numeric ID
     * @param request join room request
     * @param userDetails authenticated user
     * @return updated room
     */
    @Operation(summary = "Join room", description = "방 입장")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "방 입장 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 비밀번호 또는 방이 가득 참"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "방을 찾을 수 없음")
    })
    @PostMapping("/id/{roomId}/join")
    public ResponseEntity<ApiResponse<RoomResponse>> joinRoom(
        @PathVariable Long roomId,
        @RequestBody JoinRoomRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        RoomResponse response = roomService.joinRoom(roomId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Join a room by public ID (UUID). Returns roomResponse + chatRoomId.
     *
     * @param publicId room public UUID
     * @param request join room request
     * @param userDetails authenticated user
     * @return room + chatRoomId
     */
    @Operation(summary = "Join room by public ID", description = "방 입장 (UUID, chatRoomId 포함 반환)")
    @PostMapping("/{publicId}/join")
    public ResponseEntity<ApiResponse<Map<String, Object>>> joinRoomByPublicId(
        @PathVariable String publicId,
        @RequestBody JoinRoomRequest request,
        @AuthenticationPrincipal UserDetails userDetails,
        HttpServletRequest httpRequest
    ) {
        String ip = extractClientIp(httpRequest);
        JoinRoomResult result = roomService.joinRoomByPublicId(
            publicId, request, userDetails.getUsername(), ip
        );
        Map<String, Object> body = Map.of(
            "room", result.room(),
            "chatRoomId", result.chatRoomId()
        );
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    /**
     * Leave a room by numeric ID.
     *
     * @param roomId room numeric ID
     * @param userDetails authenticated user
     * @return no content
     */
    @Operation(summary = "Leave room", description = "방 나가기")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "방 나가기 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "방을 찾을 수 없음")
    })
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
        @PathVariable Long roomId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        roomService.leaveRoom(roomId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /**
     * Kick a participant from a room (host only).
     */
    @Operation(summary = "Kick participant", description = "참여자 강퇴 (방장만 가능)")
    @PostMapping("/{roomId}/kick/{userId}")
    public ResponseEntity<ApiResponse<Void>> kickParticipant(
        @PathVariable Long roomId,
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        roomService.kickParticipant(roomId, userId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /**
     * Transfer host to another participant (host only).
     */
    @Operation(summary = "Transfer host", description = "방장 이전 (방장만 가능)")
    @PostMapping("/{roomId}/transfer/{userId}")
    public ResponseEntity<ApiResponse<Void>> transferHost(
        @PathVariable Long roomId,
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        roomService.transferHost(roomId, userId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /**
     * Start a room (host only). Changes status to IN_PROGRESS.
     */
    @Operation(summary = "Start room", description = "방 시작 (방장만 가능)")
    @PostMapping("/{roomId}/start")
    public ResponseEntity<ApiResponse<Void>> startRoom(
        @PathVariable Long roomId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        roomService.startRoom(roomId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /**
     * Regenerate invite code for a private room (host only).
     *
     * @param publicId room public UUID
     * @param userDetails authenticated user (must be host)
     * @return updated room with new invite code
     */
    @Operation(summary = "Regenerate invite code", description = "비공개 방 초대코드 재발급 (방장만 가능)")
    @PostMapping("/{publicId}/invite/regenerate")
    public ResponseEntity<ApiResponse<RoomResponse>> regenerateInviteCode(
        @PathVariable String publicId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        RoomResponse response = roomService.regenerateInviteCode(publicId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
