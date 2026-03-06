package com.gembud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gembud 게임 파티원 모집 플랫폼 메인 애플리케이션
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
@org.springframework.scheduling.annotation.EnableAsync
public class GembudApplication {

    public static void main(String[] args) {
        SpringApplication.run(GembudApplication.class, args);
    }

}
