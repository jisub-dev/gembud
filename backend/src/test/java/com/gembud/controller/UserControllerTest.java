package com.gembud.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gembud.entity.User;
import com.gembud.security.JwtTokenProvider;
import com.gembud.service.RefreshTokenStore;
import com.gembud.service.UserService;
import com.gembud.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RefreshTokenStore refreshTokenStore;

    @Test
    @DisplayName("GET /users/search - 이메일 일부로도 친구 검색이 된다")
    @WithMockUser(username = "me@test.com")
    void searchUsers_ByEmailFragment_ReturnsMatches() throws Exception {
        User currentUser = User.builder()
            .email("me@test.com")
            .nickname("me")
            .password("pw")
            .build();
        ReflectionTestUtils.setField(currentUser, "id", 1L);

        User foundUser = User.builder()
            .email("test2@test.com")
            .nickname("buddy")
            .password("pw")
            .build();
        ReflectionTestUtils.setField(foundUser, "id", 2L);

        when(userRepository.findByEmail("me@test.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.searchFriendCandidates(eq(1L), eq("test"), org.mockito.ArgumentMatchers.any(Pageable.class)))
            .thenReturn(List.of(foundUser));

        mockMvc.perform(get("/users/search").param("q", "test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].email").value("test2@test.com"))
            .andExpect(jsonPath("$.data[0].nickname").value("buddy"));
    }

    @Test
    @DisplayName("GET /users/search - 2자 미만이면 빈 배열 반환")
    @WithMockUser(username = "me@test.com")
    void searchUsers_TooShort_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/users/search").param("q", "t"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }
}
