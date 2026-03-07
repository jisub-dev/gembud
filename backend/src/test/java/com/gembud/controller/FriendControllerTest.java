package com.gembud.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gembud.dto.request.FriendRequest;
import com.gembud.dto.response.FriendResponse;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.security.JwtTokenProvider;
import com.gembud.service.FriendService;
import com.gembud.service.RefreshTokenStore;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FriendController.class)
@AutoConfigureMockMvc(addFilters = false)
class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FriendService friendService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RefreshTokenStore refreshTokenStore;

    @Test
    @DisplayName("POST /friends/requests - friendId로 요청 전송")
    @WithMockUser(username = "test@example.com")
    void sendFriendRequest_WithFriendId_ShouldReturnCreated() throws Exception {
        FriendRequest request = FriendRequest.builder().friendId(2L).build();
        FriendResponse response = FriendResponse.builder()
            .id(1L)
            .userId(1L)
            .friendId(2L)
            .userNickname("me")
            .friendNickname("other")
            .status("PENDING")
            .createdAt(LocalDateTime.now())
            .build();

        when(friendService.sendFriendRequest(eq("test@example.com"), any(FriendRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post("/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.friendId").value(2))
            .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("PUT /friends/requests/{id}/accept - 요청 수락")
    @WithMockUser(username = "test@example.com")
    void acceptFriendRequest_ShouldReturnOk() throws Exception {
        FriendResponse response = FriendResponse.builder()
            .id(1L)
            .userId(2L)
            .friendId(1L)
            .userNickname("other")
            .friendNickname("me")
            .status("ACCEPTED")
            .updatedAt(LocalDateTime.now())
            .build();

        when(friendService.acceptFriendRequest("test@example.com", 1L)).thenReturn(response);

        mockMvc.perform(put("/friends/requests/1/accept"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("DELETE /friends/requests/{id} - 보낸 요청 취소")
    @WithMockUser(username = "test@example.com")
    void cancelSentRequest_ShouldReturnOk() throws Exception {
        mockMvc.perform(delete("/friends/requests/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("GET /friends/requests/pending - 받은 요청 목록")
    @WithMockUser(username = "test@example.com")
    void getPendingRequests_ShouldReturnList() throws Exception {
        FriendResponse pending = FriendResponse.builder()
            .id(3L)
            .userId(2L)
            .friendId(1L)
            .userNickname("sender")
            .friendNickname("me")
            .status("PENDING")
            .build();

        when(friendService.getPendingRequests("test@example.com")).thenReturn(List.of(pending));

        mockMvc.perform(get("/friends/requests/pending"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /friends/requests - 중복 요청 시 409")
    @WithMockUser(username = "test@example.com")
    void sendFriendRequest_Duplicate_ShouldReturnConflict() throws Exception {
        FriendRequest request = FriendRequest.builder().friendId(2L).build();
        when(friendService.sendFriendRequest(eq("test@example.com"), any(FriendRequest.class)))
            .thenThrow(new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS));

        mockMvc.perform(post("/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("FRIEND002"));
    }
}
