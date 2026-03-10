import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AdminPage from '@/pages/AdminPage';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';
import reportService from '@/services/reportService';
import adminService from '@/services/adminService';

const { toastSuccess, toastError } = vi.hoisted(() => ({
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
}));

vi.mock('@/store/authStore', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('@/hooks/useToast', () => ({
  useToast: vi.fn(),
}));

vi.mock('@/services/reportService', () => ({
  default: {
    getAdminReports: vi.fn(),
    warnReport: vi.fn(),
    resolveReport: vi.fn(),
  },
}));

vi.mock('@/services/adminService', () => ({
  default: {
    getSecuritySummary: vi.fn(),
    getSecurityEvents: vi.fn(),
    getUserSecurityStatus: vi.fn(),
    unlockUserLogin: vi.fn(),
  },
}));

describe('AdminPage reports flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuthStore).mockReturnValue({
      user: { id: 1, email: 'admin@test.com', role: 'ADMIN' },
    } as any);
    vi.mocked(useToast).mockReturnValue({
      success: toastSuccess,
      error: toastError,
      info: vi.fn(),
    } as any);
    vi.mocked(adminService.getSecuritySummary).mockResolvedValue({
      loginFailCount: 0,
      loginLockedCount: 0,
      refreshReuseCount: 0,
      rateLimitHitCount: 0,
    } as any);
    vi.mocked(adminService.getSecurityEvents).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    } as any);
  });

  it('filters by status and nickname search', async () => {
    vi.mocked(reportService.getAdminReports).mockImplementation(async (params: any) => {
      if (params?.status === 'PENDING') {
        return {
          content: [
            {
              id: 11,
              reporter: { id: 1, nickname: 'alpha' },
              reported: { id: 2, nickname: 'beta' },
              reason: 'ABUSIVE',
              createdAt: '2026-03-10T10:00:00',
              status: 'PENDING',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        } as any;
      }
      if (params?.status === 'RESOLVED' && params?.search === 'zzz') {
        return {
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        } as any;
      }
      return {
        content: [
          {
            id: 12,
            reporter: { id: 3, nickname: 'gamma' },
            reported: { id: 4, nickname: 'delta' },
            reason: 'SPAM',
            createdAt: '2026-03-10T10:00:00',
            status: 'RESOLVED',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      } as any;
    });

    const user = userEvent.setup();
    render(<AdminPage />);

    expect(await screen.findByText('alpha')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'RESOLVED' }));
    expect(await screen.findByText('gamma')).toBeInTheDocument();

    const searchInput = screen.getByPlaceholderText('신고자/피신고자 닉네임 검색');
    await user.type(searchInput, 'zzz');
    await user.click(screen.getByRole('button', { name: '검색' }));
    expect(screen.getByText('조건에 맞는 신고가 없습니다.')).toBeInTheDocument();
  });

  it('warn action refreshes list', async () => {
    vi.spyOn(window, 'prompt').mockReturnValue('경고 메시지');
    vi.mocked(reportService.getAdminReports).mockResolvedValue({
      content: [
        {
          id: 21,
          reporter: { id: 1, nickname: 'reporter' },
          reported: { id: 2, nickname: 'reported' },
          reason: 'ABUSIVE',
          createdAt: '2026-03-10T10:00:00',
          status: 'PENDING',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    } as any);
    vi.mocked(reportService.warnReport).mockResolvedValue(undefined);

    const user = userEvent.setup();
    render(<AdminPage />);

    await user.click(await screen.findByRole('button', { name: '경고 처리' }));

    await waitFor(() => {
      expect(reportService.warnReport).toHaveBeenCalledWith(21, '경고 메시지');
      expect(reportService.getAdminReports).toHaveBeenCalledWith({
        status: 'PENDING',
        search: undefined,
        page: 0,
        size: 20,
      });
      expect(toastSuccess).toHaveBeenCalledWith('경고 처리 완료');
    });
  });

  it('security tab applies filters and calls admin service with paging', async () => {
    vi.mocked(adminService.getSecuritySummary).mockResolvedValue({
      loginFailCount: 3,
      loginLockedCount: 1,
      refreshReuseCount: 2,
      rateLimitHitCount: 4,
    } as any);
    vi.mocked(adminService.getSecurityEvents).mockResolvedValue({
      content: [
        {
          id: 88,
          eventType: 'LOGIN_FAIL',
          userId: 100,
          ip: '127.0.0.1',
          endpoint: '/api/v1/auth/login',
          result: 'FAIL',
          riskScore: 'HIGH',
          createdAt: '2026-03-10T10:00:00',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    } as any);

    const user = userEvent.setup();
    render(<AdminPage />);

    await user.click(screen.getByRole('button', { name: '보안 이벤트' }));
    const [typeSelect, riskSelect] = screen.getAllByRole('combobox');
    await user.selectOptions(typeSelect, 'LOGIN_FAIL');
    await user.selectOptions(riskSelect, 'HIGH');
    await user.click(screen.getByRole('button', { name: '필터 적용' }));

    await waitFor(() => {
      const calls = vi.mocked(adminService.getSecurityEvents).mock.calls;
      const lastCall = calls[calls.length - 1];
      expect(calls.length).toBeGreaterThan(0);
      expect(lastCall?.[0]).toMatchObject({
        eventType: 'LOGIN_FAIL',
        riskScore: 'HIGH',
        page: 0,
        size: 20,
      });
    });

    expect(await screen.findByText('127.0.0.1')).toBeInTheDocument();
    expect(screen.getByText('/api/v1/auth/login')).toBeInTheDocument();
  });
});
