package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.entity.Advertisement;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.AdViewRepository;
import com.gembud.repository.AdvertisementRepository;
import com.gembud.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdServiceTest {

    @Mock
    private AdvertisementRepository advertisementRepository;

    @Mock
    private AdViewRepository adViewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AdService adService;

    private Advertisement ad;
    private User freeUser;
    private User premiumUser;

    @BeforeEach
    void setUp() {
        ad = Advertisement.builder()
            .id(10L)
            .title("ad")
            .description("desc")
            .targetUrl("https://example.com")
            .isActive(true)
            .isGamingRelated(true)
            .expiresAt(LocalDateTime.now().plusDays(1))
            .build();

        freeUser = User.builder()
            .email("free@example.com")
            .nickname("free")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(freeUser, "id", 1L);

        premiumUser = User.builder()
            .email("premium@example.com")
            .nickname("premium")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(premiumUser, "id", 2L);
        premiumUser.activatePremium(LocalDateTime.now().plusDays(1));

        ReflectionTestUtils.setField(adService, "dailyViewLimit", 5);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("recordAdView - 5회 미만이면 정상 기록")
    void recordAdView_WithinDailyCap_Success() {
        when(advertisementRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(userRepository.findById(1L)).thenReturn(Optional.of(freeUser));
        when(valueOperations.increment(any(String.class))).thenReturn(3L);

        adService.recordAdView(10L, 1L);

        verify(adViewRepository).save(any());
        verify(redisTemplate, never()).expire(any(String.class), any(Long.class), any(TimeUnit.class));
    }

    @Test
    @DisplayName("recordAdView - 5회 초과면 AD_VIEW_LIMIT_EXCEEDED")
    void recordAdView_OverDailyCap_Throws() {
        when(advertisementRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(userRepository.findById(1L)).thenReturn(Optional.of(freeUser));
        when(valueOperations.increment(any(String.class))).thenReturn(6L);

        assertThatThrownBy(() -> adService.recordAdView(10L, 1L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.AD_VIEW_LIMIT_EXCEEDED);

        verify(adViewRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordAdView - Premium 유저는 cap 미적용")
    void recordAdView_PremiumUser_NoCapApplied() {
        when(advertisementRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(userRepository.findById(2L)).thenReturn(Optional.of(premiumUser));

        adService.recordAdView(10L, 2L);

        verify(redisTemplate, never()).opsForValue();
        verify(adViewRepository).save(any());
    }
}
