package com.gembud.config;

import com.gembud.entity.Game;
import com.gembud.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Game data initializer.
 *
 * Adds sample games on startup if database is empty.
 *
 * @author Gembud Team
 * @since 2026-02-19
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameDataInitializer implements CommandLineRunner {

    private final GameRepository gameRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Check if games already exist
        if (gameRepository.count() > 0) {
            log.info("Games already exist in database. Skipping initialization.");
            return;
        }

        log.info("Initializing sample game data...");

        // League of Legends
        gameRepository.save(Game.builder()
            .name("리그 오브 레전드")
            .genre("MOBA")
            .description("5대5 팀 전략 게임으로, 챔피언을 선택해 적 넥서스를 파괴하세요.")
            .imageUrl("https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&q=80")
            .build());

        // Valorant
        gameRepository.save(Game.builder()
            .name("발로란트")
            .genre("FPS")
            .description("5대5 전술 슈팅 게임. 에이전트의 고유 능력을 활용하세요.")
            .imageUrl("https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800&q=80")
            .build());

        // Overwatch 2
        gameRepository.save(Game.builder()
            .name("오버워치 2")
            .genre("FPS")
            .description("영웅을 선택하고 팀원과 협력해 목표를 달성하세요.")
            .imageUrl("https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800&q=80")
            .build());

        // Apex Legends
        gameRepository.save(Game.builder()
            .name("에이펙스 레전드")
            .genre("배틀로얄")
            .description("3인 스쿼드 배틀로얄. 레전드의 능력을 조합하세요.")
            .imageUrl("https://images.unsplash.com/photo-1560419015-7c427e8ae5ba?w=800&q=80")
            .build());

        // Lost Ark
        gameRepository.save(Game.builder()
            .name("로스트아크")
            .genre("MMORPG")
            .description("쿼터뷰 액션 MMORPG. 레이드와 던전을 함께 도전하세요.")
            .imageUrl("https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&q=80")
            .build());

        // FIFA Online 4
        gameRepository.save(Game.builder()
            .name("FIFA 온라인 4")
            .genre("스포츠")
            .description("실시간 축구 게임. 드림팀을 구성하고 경기하세요.")
            .imageUrl("https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=800&q=80")
            .build());

        // Minecraft
        gameRepository.save(Game.builder()
            .name("마인크래프트")
            .genre("샌드박스")
            .description("블록으로 세상을 만들고 친구들과 함께 탐험하세요.")
            .imageUrl("https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&q=80")
            .build());

        // Among Us
        gameRepository.save(Game.builder()
            .name("어몽어스")
            .genre("소셜추리")
            .description("우주선에서 임포스터를 찾아내거나 임무를 완수하세요.")
            .imageUrl("https://images.unsplash.com/photo-1614294148960-9aa740632a87?w=800&q=80")
            .build());

        log.info("✅ Successfully initialized 8 sample games.");
    }
}
