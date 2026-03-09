import { useEffect, useMemo, useState } from 'react';
import api from '@/services/api';
import type { ApiResponse } from '@/types/api';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';

type AdminTab = 'reports' | 'users' | 'security';

interface ReportItem {
  id: number;
  reporter: { id: number; nickname: string };
  reported: { id: number; nickname: string };
  reason: string;
  createdAt: string;
  status: string;
}

interface UserSecurityStatus {
  userId: number;
  email: string;
  loginLocked: boolean;
  loginLockedUntil: string | null;
  failedLoginCountInWindow: number;
  windowMinutes: number;
}

interface SecuritySummary {
  loginFailCount: number;
  loginLockedCount: number;
  refreshReuseCount: number;
  rateLimitHitCount: number;
}

export default function AdminPage() {
  const { user } = useAuthStore();
  const toast = useToast();

  const [activeTab, setActiveTab] = useState<AdminTab>('reports');
  const [pendingReports, setPendingReports] = useState<ReportItem[]>([]);
  const [isLoadingReports, setIsLoadingReports] = useState(false);
  const [processingReportId, setProcessingReportId] = useState<number | null>(null);

  const [lookupUserId, setLookupUserId] = useState('');
  const [securityStatus, setSecurityStatus] = useState<UserSecurityStatus | null>(null);
  const [isLoadingSecurityStatus, setIsLoadingSecurityStatus] = useState(false);
  const [isUnlocking, setIsUnlocking] = useState(false);

  const [securitySummary, setSecuritySummary] = useState<SecuritySummary | null>(null);
  const [isLoadingSummary, setIsLoadingSummary] = useState(false);

  const adminEmail = import.meta.env.VITE_ADMIN_EMAIL as string | undefined;
  const isAdmin = useMemo(() => {
    if (!user) return false;
    const role = (user as { role?: string }).role;
    return role === 'ADMIN' || (!!adminEmail && user.email === adminEmail);
  }, [user, adminEmail]);

  const loadPendingReports = async () => {
    setIsLoadingReports(true);
    try {
      const response = await api.get<ApiResponse<ReportItem[]>>('/reports/status/PENDING');
      setPendingReports(response.data.data);
    } catch {
      toast.error('대기 중 신고 목록 조회에 실패했습니다.');
    } finally {
      setIsLoadingReports(false);
    }
  };

  const loadSecuritySummary = async () => {
    setIsLoadingSummary(true);
    try {
      const response = await api.get<ApiResponse<SecuritySummary>>('/admin/security-events/summary');
      setSecuritySummary(response.data.data);
    } catch {
      toast.error('보안 이벤트 요약 조회에 실패했습니다.');
    } finally {
      setIsLoadingSummary(false);
    }
  };

  useEffect(() => {
    if (!isAdmin) return;
    loadPendingReports();
    loadSecuritySummary();
  }, [isAdmin]);

  const handleWarn = async (reportId: number) => {
    const warningMessage = window.prompt('경고 메시지를 입력하세요.', '운영 정책 위반 경고');
    if (!warningMessage || !warningMessage.trim()) return;

    setProcessingReportId(reportId);
    try {
      await api.post(`/admin/reports/${reportId}/warn`, { warningMessage: warningMessage.trim() });
      toast.success('경고 처리 완료');
      await loadPendingReports();
    } catch {
      toast.error('경고 처리에 실패했습니다.');
    } finally {
      setProcessingReportId(null);
    }
  };

  const handleLookupSecurityStatus = async () => {
    const userId = Number(lookupUserId);
    if (!userId || Number.isNaN(userId)) {
      toast.error('유효한 유저 ID를 입력해주세요.');
      return;
    }
    setIsLoadingSecurityStatus(true);
    try {
      const response = await api.get<ApiResponse<UserSecurityStatus>>(`/admin/users/${userId}/security-status`);
      setSecurityStatus(response.data.data);
    } catch {
      toast.error('유저 보안 상태 조회에 실패했습니다.');
      setSecurityStatus(null);
    } finally {
      setIsLoadingSecurityStatus(false);
    }
  };

  const handleUnlockLoginLock = async () => {
    if (!securityStatus) return;
    setIsUnlocking(true);
    try {
      await api.delete(`/admin/users/${securityStatus.userId}/login-lock`);
      toast.success('로그인 잠금을 해제했습니다.');
      await handleLookupSecurityStatus();
    } catch {
      toast.error('로그인 잠금 해제에 실패했습니다.');
    } finally {
      setIsUnlocking(false);
    }
  };

  if (!isAdmin) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-8">
        <div className="rounded-xl border border-red-500/40 bg-red-500/10 p-6 text-red-200">
          관리자 권한이 없습니다.
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">관리자 페이지</h1>
        <p className="text-sm text-gray-400 mt-1">신고 처리, 유저 보안 관리, 보안 이벤트 요약</p>
      </div>

      <div className="flex flex-wrap gap-2">
        <TabButton label="신고 관리" active={activeTab === 'reports'} onClick={() => setActiveTab('reports')} />
        <TabButton label="유저 관리" active={activeTab === 'users'} onClick={() => setActiveTab('users')} />
        <TabButton label="보안 이벤트" active={activeTab === 'security'} onClick={() => setActiveTab('security')} />
      </div>

      {activeTab === 'reports' && (
        <section className="rounded-xl border border-gray-700 bg-[#18181b] p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-white">처리 대기 신고</h2>
            <button
              type="button"
              onClick={loadPendingReports}
              disabled={isLoadingReports}
              className="px-3 py-1.5 rounded-md border border-gray-600 text-gray-200 hover:border-gray-400 disabled:opacity-60"
            >
              새로고침
            </button>
          </div>

          {isLoadingReports ? (
            <p className="text-gray-400 text-sm">불러오는 중...</p>
          ) : pendingReports.length === 0 ? (
            <p className="text-gray-400 text-sm">처리 대기 신고가 없습니다.</p>
          ) : (
            <div className="space-y-3">
              {pendingReports.map((report) => (
                <div key={report.id} className="rounded-lg border border-gray-700 bg-[#111114] p-4 space-y-2">
                  <div className="text-sm text-gray-300">
                    신고자: <span className="text-white">{report.reporter.nickname}</span> / 피신고자:{' '}
                    <span className="text-white">{report.reported.nickname}</span>
                  </div>
                  <div className="text-sm text-gray-300">사유: {report.reason}</div>
                  <div className="text-xs text-gray-500">접수일: {new Date(report.createdAt).toLocaleString('ko-KR')}</div>
                  <button
                    type="button"
                    onClick={() => handleWarn(report.id)}
                    disabled={processingReportId === report.id}
                    className="mt-1 px-3 py-1.5 rounded-md bg-yellow-500/20 border border-yellow-500/50 text-yellow-200 hover:bg-yellow-500/30 disabled:opacity-60"
                  >
                    {processingReportId === report.id ? '처리 중...' : '경고 처리'}
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {activeTab === 'users' && (
        <section className="rounded-xl border border-gray-700 bg-[#18181b] p-5 space-y-4">
          <h2 className="text-lg font-semibold text-white">유저 보안 상태 조회</h2>
          <div className="flex gap-2">
            <input
              type="number"
              min={1}
              value={lookupUserId}
              onChange={(e) => setLookupUserId(e.target.value)}
              placeholder="유저 ID 입력"
              className="w-56 px-3 py-2 rounded-md bg-[#111114] border border-gray-700 text-white focus:outline-none focus:border-purple-500"
            />
            <button
              type="button"
              onClick={handleLookupSecurityStatus}
              disabled={isLoadingSecurityStatus}
              className="px-4 py-2 rounded-md bg-purple-500 hover:bg-purple-600 text-white font-medium disabled:opacity-60"
            >
              조회
            </button>
          </div>

          {securityStatus && (
            <div className="rounded-lg border border-gray-700 bg-[#111114] p-4 space-y-2 text-sm text-gray-200">
              <div>유저 ID: {securityStatus.userId}</div>
              <div>이메일: {securityStatus.email}</div>
              <div>로그인 잠금: {securityStatus.loginLocked ? '예' : '아니오'}</div>
              <div>잠금 만료: {securityStatus.loginLockedUntil ? new Date(securityStatus.loginLockedUntil).toLocaleString('ko-KR') : '-'}</div>
              <div>
                실패 횟수: {securityStatus.failedLoginCountInWindow} / {securityStatus.windowMinutes}분
              </div>
              <button
                type="button"
                onClick={handleUnlockLoginLock}
                disabled={!securityStatus.loginLocked || isUnlocking}
                className="mt-2 px-3 py-1.5 rounded-md bg-red-500/20 border border-red-500/50 text-red-200 hover:bg-red-500/30 disabled:opacity-60"
              >
                {isUnlocking ? '해제 중...' : '로그인 잠금 해제'}
              </button>
            </div>
          )}
        </section>
      )}

      {activeTab === 'security' && (
        <section className="rounded-xl border border-gray-700 bg-[#18181b] p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-white">보안 이벤트 요약</h2>
            <button
              type="button"
              onClick={loadSecuritySummary}
              disabled={isLoadingSummary}
              className="px-3 py-1.5 rounded-md border border-gray-600 text-gray-200 hover:border-gray-400 disabled:opacity-60"
            >
              새로고침
            </button>
          </div>

          {!securitySummary ? (
            <p className="text-sm text-gray-400">{isLoadingSummary ? '불러오는 중...' : '요약 데이터가 없습니다.'}</p>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <SummaryCard label="로그인 실패" value={securitySummary.loginFailCount} />
              <SummaryCard label="계정 잠금" value={securitySummary.loginLockedCount} />
              <SummaryCard label="리프레시 재사용" value={securitySummary.refreshReuseCount} />
              <SummaryCard label="레이트리밋 차단" value={securitySummary.rateLimitHitCount} />
            </div>
          )}
        </section>
      )}
    </div>
  );
}

function TabButton({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`px-4 py-2 rounded-md border text-sm font-medium transition ${
        active
          ? 'bg-purple-500/20 border-purple-500/60 text-purple-200'
          : 'bg-transparent border-gray-700 text-gray-300 hover:border-gray-500'
      }`}
    >
      {label}
    </button>
  );
}

function SummaryCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-gray-700 bg-[#111114] p-4">
      <p className="text-xs text-gray-400">{label}</p>
      <p className="mt-1 text-2xl font-bold text-white">{value.toLocaleString('ko-KR')}</p>
    </div>
  );
}
