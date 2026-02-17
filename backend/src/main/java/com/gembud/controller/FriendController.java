package com.gembud.controller;

import com.gembud.dto.request.FriendRequest;
import com.gembud.dto.response.FriendResponse;
import com.gembud.service.FriendService;
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
@RestController
@RequestMapping("/api/friends")
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
    @PostMapping("/requests")
    public ResponseEntity<FriendResponse> sendFriendRequest(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody FriendRequest request
    ) {
        FriendResponse response = friendService.sendFriendRequest(
            userDetails.getUsername(),
            request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Accept friend request.
     *
     * @param userDetails authenticated user
     * @param requestId friend request ID
     * @return updated friend relationship
     */
    @PutMapping("/requests/{requestId}/accept")
    public ResponseEntity<FriendResponse> acceptFriendRequest(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long requestId
    ) {
        FriendResponse response = friendService.acceptFriendRequest(
            userDetails.getUsername(),
            requestId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Reject friend request.
     *
     * @param userDetails authenticated user
     * @param requestId friend request ID
     * @return updated friend relationship
     */
    @PutMapping("/requests/{requestId}/reject")
    public ResponseEntity<FriendResponse> rejectFriendRequest(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long requestId
    ) {
        FriendResponse response = friendService.rejectFriendRequest(
            userDetails.getUsername(),
            requestId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Unfriend (remove friend).
     *
     * @param userDetails authenticated user
     * @param friendId friend user ID
     * @return no content
     */
    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> unfriend(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long friendId
    ) {
        friendService.unfriend(userDetails.getUsername(), friendId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get my friends list.
     *
     * @param userDetails authenticated user
     * @return list of friends
     */
    @GetMapping
    public ResponseEntity<List<FriendResponse>> getMyFriends(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<FriendResponse> friends = friendService.getMyFriends(userDetails.getUsername());
        return ResponseEntity.ok(friends);
    }

    /**
     * Get pending friend requests (received).
     *
     * @param userDetails authenticated user
     * @return list of pending requests
     */
    @GetMapping("/requests/pending")
    public ResponseEntity<List<FriendResponse>> getPendingRequests(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<FriendResponse> requests = friendService.getPendingRequests(
            userDetails.getUsername()
        );
        return ResponseEntity.ok(requests);
    }

    /**
     * Get sent friend requests.
     *
     * @param userDetails authenticated user
     * @return list of sent requests
     */
    @GetMapping("/requests/sent")
    public ResponseEntity<List<FriendResponse>> getSentRequests(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<FriendResponse> requests = friendService.getSentRequests(
            userDetails.getUsername()
        );
        return ResponseEntity.ok(requests);
    }
}
