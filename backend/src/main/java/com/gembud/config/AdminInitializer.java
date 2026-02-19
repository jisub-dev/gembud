package com.gembud.config;

import com.gembud.entity.User;
import com.gembud.entity.User.UserRole;
import com.gembud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin user initializer.
 *
 * Phase 12: 환경변수로 지정된 이메일을 가진 사용자를 ADMIN으로 승격
 *
 * @author Gembud Team
 * @since 2026-02-19 (Phase 12)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${app.admin.email:}")
    private String adminEmail;

    /**
     * 애플리케이션 시작 시 ADMIN 계정 초기화.
     *
     * 환경변수 ADMIN_EMAIL로 지정된 이메일을 가진 사용자를 ADMIN으로 승격.
     * 해당 유저가 없으면 경고 로그만 출력 (자동 생성하지 않음).
     *
     * @param args command line arguments
     */
    @Override
    @Transactional
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.info("No admin email configured (app.admin.email). Skipping admin initialization.");
            return;
        }

        log.info("Initializing admin user for email: {}", adminEmail);

        User user = userRepository.findByEmail(adminEmail).orElse(null);

        if (user == null) {
            log.warn("Admin user with email '{}' not found. Please create the user first.", adminEmail);
            return;
        }

        if (user.getRole() == UserRole.ADMIN) {
            log.info("User '{}' is already ADMIN. No action needed.", adminEmail);
            return;
        }

        // Promote to ADMIN
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);

        log.info("✅ User '{}' promoted to ADMIN successfully.", adminEmail);
    }
}
