package com.gembud.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.eq;

import com.gembud.entity.Report;
import com.gembud.entity.User;
import com.gembud.security.CustomAuthenticationEntryPoint;
import com.gembud.security.JwtAuthenticationFilter;
import com.gembud.security.CustomUserDetails;
import com.gembud.security.JwtTokenProvider;
import com.gembud.security.OAuth2SuccessHandler;
import com.gembud.service.RefreshTokenStore;
import com.gembud.service.ReportService;
import jakarta.servlet.FilterChain;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc
class AdminReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RefreshTokenStore refreshTokenStore;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("GET /admin/reports - 상태/검색/페이지 파라미터로 목록 조회")
    void listReports_WithFilters_ReturnsPagedData() throws Exception {
        User reporter = User.builder()
            .email("reporter@example.com")
            .nickname("AlphaReporter")
            .temperature(new BigDecimal("36.5"))
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(reporter, "id", 1L);

        User reported = User.builder()
            .email("reported@example.com")
            .nickname("BetaTarget")
            .temperature(new BigDecimal("36.5"))
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(reported, "id", 2L);

        Report report = Report.builder()
            .id(7L)
            .reporter(reporter)
            .reported(reported)
            .reason("욕설")
            .description("욕설 신고")
            .status(Report.ReportStatus.PENDING)
            .category(Report.ReportCategory.VERBAL_ABUSE)
            .priority(Report.ReportPriority.HIGH)
            .build();

        org.mockito.Mockito.when(reportService.searchAdminReports(
            eq(Report.ReportStatus.PENDING),
            eq("alpha"),
            eq("reporter"),
            eq("target"),
            any()
        )).thenReturn(new PageImpl<>(List.of(report), PageRequest.of(0, 20), 1));

        CustomUserDetails principal = new CustomUserDetails(
            99L,
            "admin@gembud.com",
            "",
            User.UserRole.ADMIN
        );
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(get("/admin/reports")
                .with(authentication(authToken))
                .param("status", "PENDING")
                .param("search", "alpha")
                .param("reporterNickname", "reporter")
                .param("reportedNickname", "target")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].id").value(7))
            .andExpect(jsonPath("$.data.content[0].reporter.nickname").value("AlphaReporter"))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20))
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /admin/reports/{id}/warn - 성공 시 warningIssued=true")
    void warnReport_Success_WarningIssuedTrue() throws Exception {
        User reporter = User.builder()
            .email("reporter@example.com")
            .nickname("Reporter")
            .temperature(new BigDecimal("36.5"))
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(reporter, "id", 1L);

        User reported = User.builder()
            .email("reported@example.com")
            .nickname("Reported")
            .temperature(new BigDecimal("36.5"))
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(reported, "id", 2L);

        Report report = Report.builder()
            .id(9L)
            .reporter(reporter)
            .reported(reported)
            .reason("욕설")
            .description("욕설 신고")
            .status(Report.ReportStatus.REVIEWED)
            .category(Report.ReportCategory.VERBAL_ABUSE)
            .priority(Report.ReportPriority.HIGH)
            .build();

        org.mockito.Mockito.when(reportService.warnReport(
            org.mockito.ArgumentMatchers.eq(9L),
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.eq("경고 메시지")
        )).thenReturn(report);

        CustomUserDetails principal = new CustomUserDetails(
            99L,
            "admin@gembud.com",
            "",
            User.UserRole.ADMIN
        );
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(post("/admin/reports/9/warn")
                .with(authentication(authToken))
                .with(csrf())
                .contentType("application/json")
                .content("{\"warningMessage\":\"경고 메시지\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(9))
            .andExpect(jsonPath("$.data.warningIssued").value(true));
    }
}
