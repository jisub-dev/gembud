package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gembud.dto.request.CreateRoomRequest;
import com.gembud.dto.response.RoomResponse;
import com.gembud.entity.ChatRoom;
import com.gembud.entity.Game;
import com.gembud.entity.User;
import com.gembud.repository.ChatRoomMemberRepository;
import com.gembud.repository.ChatRoomRepository;
import com.gembud.repository.GameRepository;
import com.gembud.repository.RoomParticipantRepository;
import com.gembud.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RoomServiceCreateRoomIntegrationTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Test
    @DisplayName("createRoom - missing ROOM_CHAT mapping should be recovered without rollback")
    void createRoom_MissingRoomChatMapping_ShouldCreateRoomAndChat() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User user = userRepository.save(User.builder()
            .email("room-create-" + suffix + "@example.com")
            .password("password123")
            .nickname("room-" + suffix)
            .temperature(new BigDecimal("36.5"))
            .build());

        Game game = gameRepository.save(Game.builder()
            .name("room-create-game-" + suffix)
            .genre("TEST")
            .description("integration test game")
            .build());

        CreateRoomRequest request = CreateRoomRequest.builder()
            .gameId(game.getId())
            .title("integration create room " + suffix)
            .description("create room rollback regression")
            .maxParticipants(5)
            .isPrivate(false)
            .build();

        RoomResponse response = roomService.createRoom(request, user.getEmail());

        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo(request.getTitle());
        assertThat(response.getCurrentParticipants()).isEqualTo(1);
        assertThat(roomParticipantRepository.findByRoomId(response.getId())).hasSize(1);

        ChatRoom chatRoom = chatRoomRepository.findByTypeAndRelatedRoomId(
                ChatRoom.ChatRoomType.ROOM_CHAT,
                response.getId()
            )
            .orElseThrow();
        assertThat(chatRoom.getPublicId()).isNotBlank();
        assertThat(chatRoomMemberRepository.countByChatRoomId(chatRoom.getId())).isEqualTo(1);
    }
}
