package com.gembud.controller;

import com.gembud.dto.request.CreateRoomRequest;
import com.gembud.dto.request.JoinRoomRequest;
import com.gembud.dto.response.RoomResponse;
import com.gembud.service.RoomService;
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
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
        @Valid @RequestBody CreateRoomRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        RoomResponse response = roomService.createRoom(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get rooms by game.
     *
     * @param gameId game ID
     * @return list of rooms
     */
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getRoomsByGame(
        @RequestParam Long gameId
    ) {
        return ResponseEntity.ok(roomService.getRoomsByGame(gameId));
    }

    /**
     * Get room by ID.
     *
     * @param roomId room ID
     * @return room details
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    /**
     * Join a room.
     *
     * @param roomId room ID
     * @param request join room request
     * @param userDetails authenticated user
     * @return updated room
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<RoomResponse> joinRoom(
        @PathVariable Long roomId,
        @RequestBody JoinRoomRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        RoomResponse response = roomService.joinRoom(roomId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Leave a room.
     *
     * @param roomId room ID
     * @param userDetails authenticated user
     * @return no content
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
        @PathVariable Long roomId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        roomService.leaveRoom(roomId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Close a room (host only).
     *
     * @param roomId room ID
     * @param userDetails authenticated user
     * @return no content
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> closeRoom(
        @PathVariable Long roomId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        roomService.closeRoom(roomId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
