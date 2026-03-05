package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.entity.Report;
import com.gembud.entity.Report.ReportCategory;
import com.gembud.entity.Report.ReportStatus;
import com.gembud.entity.Room;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.repository.ReportRepository;
import com.gembud.repository.RoomRepository;
import com.gembud.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for ReportService.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReportService reportService;

    private User reporter;
    private User reported;
    private Room testRoom;
    private Report testReport;

    @BeforeEach
    void setUp() {
        reporter = User.builder()
            .email("reporter@example.com")
            .nickname("Reporter")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(reporter, "id", 1L);

        reported = User.builder()
            .email("reported@example.com")
            .nickname("Reported")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(reported, "id", 2L);

        testRoom = Room.builder()
            .id(1L)
            .title("Test Room")
            .build();

        testReport = Report.builder()
            .id(1L)
            .reporter(reporter)
            .reported(reported)
            .room(testRoom)
            .category(ReportCategory.VERBAL_ABUSE)
            .reason("욕설 사용")
            .description("심한 욕설을 사용했습니다.")
            .status(ReportStatus.PENDING)
            .build();
    }

    @Test
    @DisplayName("createReport - should create report successfully")
    void createReport_Success() {
        // Given
        when(userRepository.findByEmail("reporter@example.com"))
            .thenReturn(Optional.of(reporter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reported));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(reportRepository.existsByReporterIdAndReportedIdAndRoomId(1L, 2L, 1L))
            .thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenReturn(testReport);
        when(reportRepository.countPendingByReportedId(2L)).thenReturn(0L);

        // When
        Report result = reportService.createReport(
            "reporter@example.com",
            2L,
            1L,
            ReportCategory.VERBAL_ABUSE,
            "욕설 사용",
            "심한 욕설을 사용했습니다."
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getReporter().getId()).isEqualTo(1L);
        assertThat(result.getReported().getId()).isEqualTo(2L);
        assertThat(result.getRoom().getId()).isEqualTo(1L);
        assertThat(result.getReason()).isEqualTo("욕설 사용");
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    @DisplayName("createReport - should throw exception when reporter not found")
    void createReport_ReporterNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("unknown@example.com"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            reportService.createReport("unknown@example.com", 2L, 1L, ReportCategory.VERBAL_ABUSE, "욕설", "상세 내용"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("createReport - should throw exception when reported user not found")
    void createReport_ReportedNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("reporter@example.com"))
            .thenReturn(Optional.of(reporter));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            reportService.createReport("reporter@example.com", 999L, 1L, ReportCategory.VERBAL_ABUSE, "욕설", "상세 내용"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("createReport - should throw exception when reporting yourself")
    void createReport_ReportingSelf_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("reporter@example.com"))
            .thenReturn(Optional.of(reporter));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));

        // When & Then
        assertThatThrownBy(() ->
            reportService.createReport("reporter@example.com", 1L, null, ReportCategory.VERBAL_ABUSE, "욕설", "상세 내용"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("createReport - should throw exception when already reported in room")
    void createReport_AlreadyReported_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("reporter@example.com"))
            .thenReturn(Optional.of(reporter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reported));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(reportRepository.existsByReporterIdAndReportedIdAndRoomId(1L, 2L, 1L))
            .thenReturn(true);

        // When & Then
        assertThatThrownBy(() ->
            reportService.createReport("reporter@example.com", 2L, 1L, ReportCategory.VERBAL_ABUSE, "욕설", "상세 내용"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("createReport - should create report without room")
    void createReport_WithoutRoom_Success() {
        // Given
        when(userRepository.findByEmail("reporter@example.com"))
            .thenReturn(Optional.of(reporter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reported));
        when(reportRepository.countPendingByReportedId(2L)).thenReturn(0L);

        Report reportWithoutRoom = Report.builder()
            .id(2L)
            .reporter(reporter)
            .reported(reported)
            .room(null)
            .category(ReportCategory.FALSE_INFO)
            .reason("프로필 욕설")
            .description("프로필에 욕설이 포함되어 있습니다.")
            .status(ReportStatus.PENDING)
            .build();

        when(reportRepository.save(any(Report.class))).thenReturn(reportWithoutRoom);

        // When
        Report result = reportService.createReport(
            "reporter@example.com",
            2L,
            null,
            ReportCategory.FALSE_INFO,
            "프로필 욕설",
            "프로필에 욕설이 포함되어 있습니다."
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRoom()).isNull();
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    @DisplayName("getMyReports - should return reporter's reports")
    void getMyReports_Success() {
        // Given
        when(userRepository.findByEmail("reporter@example.com"))
            .thenReturn(Optional.of(reporter));
        when(reportRepository.findByReporterId(1L))
            .thenReturn(Arrays.asList(testReport));

        // When
        List<Report> results = reportService.getMyReports("reporter@example.com");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getReporter().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getReportsByStatus - should return reports by status")
    void getReportsByStatus_Success() {
        // Given
        when(reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING))
            .thenReturn(Arrays.asList(testReport));

        // When
        List<Report> results = reportService.getReportsByStatus(ReportStatus.PENDING);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("getReportsAgainstUser - should return reports against a user")
    void getReportsAgainstUser_Success() {
        // Given
        when(userRepository.existsById(2L)).thenReturn(true);
        when(reportRepository.findByReportedId(2L))
            .thenReturn(Arrays.asList(testReport));

        // When
        List<Report> results = reportService.getReportsAgainstUser(2L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getReported().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getReportsAgainstUser - should throw exception when user not found")
    void getReportsAgainstUser_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> reportService.getReportsAgainstUser(999L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("markAsReviewed - should mark report as reviewed")
    void markAsReviewed_Success() {
        // Given
        when(reportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(reportRepository.save(any(Report.class))).thenReturn(testReport);

        // When
        Report result = reportService.markAsReviewed(1L);

        // Then
        assertThat(result.getStatus()).isEqualTo(ReportStatus.REVIEWED);
        verify(reportRepository).save(testReport);
    }

    @Test
    @DisplayName("markAsReviewed - should throw exception when report not found")
    void markAsReviewed_ReportNotFound_ShouldThrowException() {
        // Given
        when(reportRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reportService.markAsReviewed(999L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("markAsReviewed - should throw exception when not PENDING")
    void markAsReviewed_NotPending_ShouldThrowException() {
        // Given
        Report reviewedReport = Report.builder()
            .id(1L)
            .reporter(reporter)
            .reported(reported)
            .category(ReportCategory.VERBAL_ABUSE)
            .status(ReportStatus.REVIEWED)
            .build();

        when(reportRepository.findById(1L)).thenReturn(Optional.of(reviewedReport));

        // When & Then
        assertThatThrownBy(() -> reportService.markAsReviewed(1L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("resolveReport - should resolve report with comment")
    void resolveReport_Success() {
        // Given
        when(reportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(reportRepository.save(any(Report.class))).thenReturn(testReport);

        // When
        Report result = reportService.resolveReport(1L, "처리 완료");

        // Then
        assertThat(result.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(reportRepository).save(testReport);
    }

    @Test
    @DisplayName("resolveReport - should throw exception when already resolved")
    void resolveReport_AlreadyResolved_ShouldThrowException() {
        // Given
        Report resolvedReport = Report.builder()
            .id(1L)
            .reporter(reporter)
            .reported(reported)
            .category(ReportCategory.VERBAL_ABUSE)
            .status(ReportStatus.RESOLVED)
            .build();

        when(reportRepository.findById(1L)).thenReturn(Optional.of(resolvedReport));

        // When & Then
        assertThatThrownBy(() -> reportService.resolveReport(1L, "처리 완료"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getPendingReportCount - should return count")
    void getPendingReportCount_Success() {
        // Given
        when(reportRepository.countPendingByReportedId(2L)).thenReturn(3L);

        // When
        long count = reportService.getPendingReportCount(2L);

        // Then
        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("deleteReport - should delete report")
    void deleteReport_Success() {
        // Given
        when(reportRepository.existsById(1L)).thenReturn(true);

        // When
        reportService.deleteReport(1L);

        // Then
        verify(reportRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteReport - should throw exception when not found")
    void deleteReport_NotFound_ShouldThrowException() {
        // Given
        when(reportRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> reportService.deleteReport(999L))
            .isInstanceOf(BusinessException.class);
    }
}
