package com.gembud.service;

import com.gembud.entity.AdView;
import com.gembud.entity.Advertisement;
import com.gembud.entity.User;
import com.gembud.repository.AdViewRepository;
import com.gembud.repository.AdvertisementRepository;
import com.gembud.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for advertisement management (Phase 11).
 *
 * @author Gembud Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdService {

    private final AdvertisementRepository advertisementRepository;
    private final AdViewRepository adViewRepository;
    private final UserRepository userRepository;

    private static final int MAX_ADS_PER_DAY = 3;

    /**
     * Get ads for user (Phase 11: 1-day 3x limit, premium excluded).
     *
     * @param userId user ID
     * @return list of ads (empty if premium or limit exceeded)
     */
    public List<Advertisement> getAdsForUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // TODO: Exclude premium users when premium feature is implemented
        // if (user.isPremium()) {
        //     return Collections.emptyList();
        // }

        // Check daily limit
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        long viewCount = adViewRepository.countByUserIdSince(userId, oneDayAgo);

        if (viewCount >= MAX_ADS_PER_DAY) {
            log.debug("User {} has reached daily ad limit ({}/{})",
                user.getNickname(), viewCount, MAX_ADS_PER_DAY);
            return Collections.emptyList();
        }

        // Return active gaming ads
        return advertisementRepository.findActiveGamingAds(LocalDateTime.now());
    }

    /**
     * Record ad view (Phase 11).
     *
     * @param adId advertisement ID
     * @param userId user ID
     */
    @Transactional
    public void recordAdView(Long adId, Long userId) {
        Advertisement ad = advertisementRepository.findById(adId)
            .orElseThrow(() -> new IllegalArgumentException("Advertisement not found"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if ad is still valid
        if (!ad.isValid()) {
            throw new IllegalStateException("Advertisement is not active or expired");
        }

        // Check daily limit
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        long viewCount = adViewRepository.countByUserIdSince(userId, oneDayAgo);

        if (viewCount >= MAX_ADS_PER_DAY) {
            throw new IllegalStateException("Daily ad view limit exceeded");
        }

        // Record view
        AdView adView = AdView.builder()
            .advertisement(ad)
            .user(user)
            .build();
        adViewRepository.save(adView);

        log.info("Ad view recorded: user={}, ad={}, dailyCount={}",
            user.getNickname(), ad.getId(), viewCount + 1);
    }

    /**
     * Get remaining ad views for user today (Phase 11).
     *
     * @param userId user ID
     * @return remaining views
     */
    public int getRemainingAdViews(Long userId) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        long viewCount = adViewRepository.countByUserIdSince(userId, oneDayAgo);
        return Math.max(0, MAX_ADS_PER_DAY - (int) viewCount);
    }
}
