package com.gembud.dto.response;

import com.gembud.entity.Advertisement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for advertisement (Phase 11).
 *
 * @author Gembud Team
 * @since 2026-02-18
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdResponse {

    /**
     * Ad ID.
     */
    private Long id;

    /**
     * Ad title.
     */
    private String title;

    /**
     * Ad description.
     */
    private String description;

    /**
     * Ad image URL.
     */
    private String imageUrl;

    /**
     * Ad target URL.
     */
    private String targetUrl;

    /**
     * Create from Advertisement entity.
     *
     * @param ad advertisement
     * @return ad response
     */
    public static AdResponse from(Advertisement ad) {
        return AdResponse.builder()
            .id(ad.getId())
            .title(ad.getTitle())
            .description(ad.getDescription())
            .imageUrl(ad.getImageUrl())
            .targetUrl(ad.getTargetUrl())
            .build();
    }
}
