import { useEffect, useMemo, useRef, useState } from 'react';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';
import reportService, { type AdminReportItem, type AdminReportStatus } from '@/services/reportService';
import adminService, {
  type SecurityEventItem,
  type SecuritySummary,
  type UserSecurityStatus,
} from '@/services/adminService';

type AdminTab = 'reports' | 'users' | 'security';

const SECURITY_EVENT_TYPES = [
  'LOGIN_FAIL',
  'LOGIN_LOCKED',
  'REFRESH_REUSE_DETECTED',
  'RATE_LIMIT_HIT',
  'LOGIN_FAIL_BURST',
  'REPORT_WARNED',
] as const;

const SECURITY_RISK_LEVELS = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;

export default function AdminPage() {
  const { user } = useAuthStore();
  const toast = useToast();

  const [activeTab, setActiveTab] = useState<AdminTab>('reports');
  const [reportItems, setReportItems] = useState<AdminReportItem[]>([]);
  const [reportStatusFilter, setReportStatusFilter] = useState<AdminReportStatus>('PENDING');
  const [reportSearch, setReportSearch] = useState('');
  const [reportPageMeta, setReportPageMeta] = useState({ page: 0, size: 20, totalElements: 0, totalPages: 0 });
  const [isLoadingReports, setIsLoadingReports] = useState(false);
  const [processingReportId, setProcessingReportId] = useState<number | null>(null);

  const [lookupUserId, setLookupUserId] = useState('');
  const [securityStatus, setSecurityStatus] = useState<UserSecurityStatus | null>(null);
  const [isLoadingSecurityStatus, setIsLoadingSecurityStatus] = useState(false);
  const [isUnlocking, setIsUnlocking] = useState(false);

  const [securityFilters, setSecurityFilters] = useState({
    eventType: '',
    riskScore: '',
    from: '',
    to: '',
  });
  const [securityItems, setSecurityItems] = useState<SecurityEventItem[]>([]);
  const [securityPageMeta, setSecurityPageMeta] = useState({ page: 0, size: 20, totalElements: 0, totalPages: 0 });
  const [securitySummary, setSecuritySummary] = useState<SecuritySummary | null>(null);
  const [isLoadingSecurityList, setIsLoadingSecurityList] = useState(false);
  const [isLoadingSummary, setIsLoadingSummary] = useState(false);
  const didSkipInitialReportFilterLoadRef = useRef(false);

  const adminEmail = import.meta.env.VITE_ADMIN_EMAIL as string | undefined;
  const isAdmin = useMemo(() => {
    if (!user) return false;
    const role = (user as { role?: string }).role;
    return role === 'ADMIN' || (!!adminEmail && user.email === adminEmail);
  }, [user, adminEmail]);

  const loadReports = async (
    status: AdminReportStatus = reportStatusFilter,
    search: string = reportSearch,
    page: number = 0,
  ) => {
    setIsLoadingReports(true);
    try {
      const data = await reportService.getAdminReports({
        status,
        search: search.trim() || undefined,
        page,
        size: reportPageMeta.size,
      });
      setReportItems(data.content);
      setReportPageMeta({
        page: data.page,
        size: data.size,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
      });
    } catch {
      toast.error('신고 목록 조회에 실패했습니다.');
    } finally {
      setIsLoadingReports(false);
    }
  };

  const toIsoOrUndefined = (value: string): string | undefined => {
    if (!value.trim()) return undefined;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
  };

  const getSummaryWindowMinutes = (from?: string, to?: string): number => {
    if (!from || !to) return 60;
    const diffMs = new Date(to).getTime() - new Date(from).getTime();
    if (Number.isNaN(diffMs) || diffMs <= 0) return 60;
    const minutes = Math.ceil(diffMs / (1000 * 60));
    return Math.max(1, Math.min(1440, minutes));
  };

  const loadSecurityData = async (page: number = 0, sizeOverride?: number) => {
    const fromIso = toIsoOrUndefined(securityFilters.from);
    const toIso = toIsoOrUndefined(securityFilters.to);

    if (fromIso && toIso && new Date(fromIso).getTime() > new Date(toIso).getTime()) {
      toast.error('시작일은 종료일보다 이전이어야 합니다.');
      return;
    }

    setIsLoadingSecurityList(true);
    setIsLoadingSummary(true);
    try {
      const [listData, summaryData] = await Promise.all([
        adminService.getSecurityEvents({
          eventType: securityFilters.eventType || undefined,
          riskScore: securityFilters.riskScore || undefined,
          from: fromIso,
          to: toIso,
          page,
          size: sizeOverride ?? securityPageMeta.size,
        }),
        adminService.getSecuritySummary(getSummaryWindowMinutes(fromIso, toIso)),
      ]);

      setSecurityItems(listData.content);
      setSecurityPageMeta({
        page: listData.page,
        size: listData.size,
        totalElements: listData.totalElements,
        totalPages: listData.totalPages,
      });
      setSecuritySummary(summaryData);
    } catch {
      toast.error('보안 이벤트 조회에 실패했습니다.');
    } finally {
      setIsLoadingSecurityList(false);
      setIsLoadingSummary(false);
    }
  };

  useEffect(() => {
    if (!isAdmin) return;
    loadReports('PENDING');
    loadSecurityData(0);
  }, [isAdmin]);

  useEffect(() => {
    if (!isAdmin) return;
    if (!didSkipInitialReportFilterLoadRef.current) {
      didSkipInitialReportFilterLoadRef.current = true;
      return;
    }
    loadReports(reportStatusFilter, reportSearch, 0);
  }, [isAdmin, reportStatusFilter, reportSearch]);

  const handleWarn = async (reportId: number) => {
    const warningMessage = window.prompt('경고 메시지를 입력하세요.', '운영 정책 위반 경고');
    if (!warningMessage || !warningMessage.trim()) return;

    setProcessingReportId(reportId);
    try {
      await reportService.warnReport(reportId, warningMessage.trim());
      toast.success('경고 처리 완료');
      await loadReports(reportStatusFilter);
    } catch {
      toast.error('경고 처리에 실패했습니다.');
    } finally {
      setProcessingReportId(null);
    }
  };

  const handleResolve = async (reportId: number) => {
    const adminComment = window.prompt('처리 코멘트를 입력하세요.', '처리 완료');
    if (!adminComment || !adminComment.trim()) return;

    setProcessingReportId(reportId);
    try {
      await reportService.resolveReport(reportId, adminComment.trim());
      toast.success('신고를 처리 완료했습니다.');
      await loadReports(reportStatusFilter);
    } catch {
      toast.error('신고 처리 완료에 실패했습니다.');
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
      const data = await adminService.getUserSecurityStatus(userId);
      setSecurityStatus(data);
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
      await adminService.unlockUserLogin(securityStatus.userId);
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
        <p className="text-sm text-gray-400 mt-1">신고 처리, 유저 보안 관리, 보안 이벤트 모니터링</p>
      </div>

      <div className="flex flex-wrap gap-2">
        <TabButton label="신고 관리" active={activeTab === 'reports'} onClick={() => setActiveTab('reports')} />
        <TabButton label="유저 관리" active={activeTab === 'users'} onClick={() => setActiveTab('users')} />
        <TabButton label="보안 이벤트" active={activeTab === 'security'} onClick={() => setActiveTab('security')} />
      </div>

      {activeTab === 'reports' && (
        <section className="rounded-xl border border-gray-700 bg-[#18181b] p-5">
          <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
            <h2 className="text-lg font-semibold text-white">신고 목록</h2>
            <div className="flex flex-wrap items-center gap-2">
              {(['PENDING', 'REVIEWED', 'RESOLVED'] as AdminReportStatus[]).map((status) => (
                <button
                  key={status}
                  type="button"
                  onClick={() => setReportStatusFilter(status)}
                  className={`px-3 py-1.5 rounded-md border text-xs font-semibold transition ${
                    reportStatusFilter === status
                      ? 'border-purple-500/70 bg-purple-500/20 text-purple-200'
                      : 'border-gray-600 text-gray-300 hover:border-gray-500'
                  }`}
                >
                  {status}
                </button>
              ))}
              <button
                type="button"
                onClick={() => loadReports(reportStatusFilter, reportSearch, reportPageMeta.page)}
                disabled={isLoadingReports}
                className="px-3 py-1.5 rounded-md border border-gray-600 text-gray-200 hover:border-gray-400 disabled:opacity-60"
              >
                새로고침
              </button>
            </div>
          </div>
          <div className="mb-2 flex gap-2">
            <input
              type="text"
              value={reportSearch}
              onChange={(e) => setReportSearch(e.target.value)}
              placeholder="신고자/피신고자 닉네임 검색"
              className="w-full px-3 py-2 rounded-md bg-[#111114] border border-gray-700 text-white text-sm focus:outline-none focus:border-purple-500"
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  void loadReports(reportStatusFilter, reportSearch, 0);
                }
              }}
            />
            <button
              type="button"
              onClick={() => loadReports(reportStatusFilter, reportSearch, 0)}
              disabled={isLoadingReports}
              className="px-3 py-2 rounded-md border border-gray-600 text-gray-200 hover:border-gray-400 disabled:opacity-60 whitespace-nowrap"
            >
              검색
            </button>
          </div>
          <p className="mb-4 text-xs text-gray-500">
            총 {reportPageMeta.totalElements.toLocaleString('ko-KR')}건
          </p>

          {isLoadingReports ? (
            <p className="text-gray-400 text-sm">불러오는 중...</p>
          ) : reportItems.length === 0 ? (
            <p className="text-gray-400 text-sm">조건에 맞는 신고가 없습니다.</p>
          ) : (
            <div className="space-y-3">
              {reportItems.map((report) => (
                <div key={report.id} className="rounded-lg border border-gray-700 bg-[#111114] p-4 space-y-2">
                  <div className="flex items-center justify-between gap-3">
                    <div className="text-sm text-gray-300">
                      신고자: <span className="text-white">{report.reporter.nickname}</span> / 피신고자:{' '}
                      <span className="text-white">{report.reported.nickname}</span>
                    </div>
                    <ReportStatusBadge status={report.status} />
                  </div>
                  <div className="text-sm text-gray-300">사유: {report.reason}</div>
                  <div className="text-xs text-gray-500">접수일: {new Date(report.createdAt).toLocaleString('ko-KR')}</div>

                  {report.status !== 'RESOLVED' && (
                    <div className="mt-1 flex gap-2">
                      <button
                        type="button"
                        onClick={() => handleWarn(report.id)}
                        disabled={processingReportId === report.id}
                        className="px-3 py-1.5 rounded-md bg-yellow-500/20 border border-yellow-500/50 text-yellow-200 hover:bg-yellow-500/30 disabled:opacity-60"
                      >
                        {processingReportId === report.id ? '처리 중...' : '경고 처리'}
                      </button>
                      <button
                        type="button"
                        onClick={() => handleResolve(report.id)}
                        disabled={processingReportId === report.id}
                        className="px-3 py-1.5 rounded-md bg-emerald-500/20 border border-emerald-500/50 text-emerald-200 hover:bg-emerald-500/30 disabled:opacity-60"
                      >
                        {processingReportId === report.id ? '처리 중...' : '처리 완료'}
                      </button>
                    </div>
                  )}
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
        <section className="rounded-xl border border-gray-700 bg-[#18181b] p-5 space-y-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-semibold text-white">보안 이벤트</h2>
            <button
              type="button"
              onClick={() => loadSecurityData(securityPageMeta.page)}
              disabled={isLoadingSecurityList || isLoadingSummary}
              className="px-3 py-1.5 rounded-md border border-gray-600 text-gray-200 hover:border-gray-400 disabled:opacity-60"
            >
              새로고침
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-2">
            <select
              value={securityFilters.eventType}
              onChange={(e) => setSecurityFilters((prev) => ({ ...prev, eventType: e.target.value }))}
              className="px-3 py-2 rounded-md bg-[#111114] border border-gray-700 text-white text-sm"
            >
              <option value="">전체 타입</option>
              {SECURITY_EVENT_TYPES.map((eventType) => (
                <option key={eventType} value={eventType}>{eventType}</option>
              ))}
            </select>

            <select
              value={securityFilters.riskScore}
              onChange={(e) => setSecurityFilters((prev) => ({ ...prev, riskScore: e.target.value }))}
              className="px-3 py-2 rounded-md bg-[#111114] border border-gray-700 text-white text-sm"
            >
              <option value="">전체 위험도</option>
              {SECURITY_RISK_LEVELS.map((risk) => (
                <option key={risk} value={risk}>{risk}</option>
              ))}
            </select>

            <input
              type="datetime-local"
              value={securityFilters.from}
              onChange={(e) => setSecurityFilters((prev) => ({ ...prev, from: e.target.value }))}
              className="px-3 py-2 rounded-md bg-[#111114] border border-gray-700 text-white text-sm"
            />

            <input
              type="datetime-local"
              value={securityFilters.to}
              onChange={(e) => setSecurityFilters((prev) => ({ ...prev, to: e.target.value }))}
              className="px-3 py-2 rounded-md bg-[#111114] border border-gray-700 text-white text-sm"
            />

            <button
              type="button"
              onClick={() => loadSecurityData(0)}
              disabled={isLoadingSecurityList || isLoadingSummary}
              className="px-3 py-2 rounded-md bg-purple-500 hover:bg-purple-600 text-white font-medium disabled:opacity-60"
            >
              필터 적용
            </button>
          </div>

          {!securitySummary ? (
            <p className="text-sm text-gray-400">{isLoadingSummary ? '요약 불러오는 중...' : '요약 데이터가 없습니다.'}</p>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <SummaryCard label="로그인 실패" value={securitySummary.loginFailCount} />
              <SummaryCard label="계정 잠금" value={securitySummary.loginLockedCount} />
              <SummaryCard label="리프레시 재사용" value={securitySummary.refreshReuseCount} />
              <SummaryCard label="레이트리밋 차단" value={securitySummary.rateLimitHitCount} />
            </div>
          )}

          <div className="flex items-center justify-between text-xs text-gray-400">
            <span>총 {securityPageMeta.totalElements.toLocaleString('ko-KR')}건</span>
            <div className="flex items-center gap-2">
              <span>페이지 크기</span>
              <select
                value={securityPageMeta.size}
                onChange={(e) => {
                  const nextSize = Number(e.target.value);
                  setSecurityPageMeta((prev) => ({ ...prev, size: nextSize, page: 0 }));
                  void loadSecurityData(0, nextSize);
                }}
                className="px-2 py-1 rounded bg-[#111114] border border-gray-700 text-white"
              >
                {[20, 50, 100].map((size) => (
                  <option key={size} value={size}>{size}</option>
                ))}
              </select>
            </div>
          </div>

          {isLoadingSecurityList ? (
            <p className="text-sm text-gray-400">보안 이벤트 불러오는 중...</p>
          ) : securityItems.length === 0 ? (
            <p className="text-sm text-gray-400">조건에 맞는 보안 이벤트가 없습니다.</p>
          ) : (
            <div className="overflow-x-auto rounded-lg border border-gray-700">
              <table className="min-w-full text-sm">
                <thead className="bg-[#111114] text-gray-300">
                  <tr>
                    <th className="px-3 py-2 text-left">시각</th>
                    <th className="px-3 py-2 text-left">이벤트</th>
                    <th className="px-3 py-2 text-left">위험도</th>
                    <th className="px-3 py-2 text-left">결과</th>
                    <th className="px-3 py-2 text-left">유저ID</th>
                    <th className="px-3 py-2 text-left">IP</th>
                    <th className="px-3 py-2 text-left">엔드포인트</th>
                  </tr>
                </thead>
                <tbody>
                  {securityItems.map((item) => (
                    <tr key={item.id} className="border-t border-gray-800 text-gray-200">
                      <td className="px-3 py-2 whitespace-nowrap">{new Date(item.createdAt).toLocaleString('ko-KR')}</td>
                      <td className="px-3 py-2 whitespace-nowrap">{item.eventType}</td>
                      <td className="px-3 py-2"><RiskBadge risk={item.riskScore} /></td>
                      <td className="px-3 py-2 whitespace-nowrap">{item.result ?? '-'}</td>
                      <td className="px-3 py-2 whitespace-nowrap">{item.userId ?? '-'}</td>
                      <td className="px-3 py-2 whitespace-nowrap">{item.ip ?? '-'}</td>
                      <td className="px-3 py-2">{item.endpoint ?? '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={() => loadSecurityData(Math.max(securityPageMeta.page - 1, 0))}
              disabled={securityPageMeta.page === 0 || isLoadingSecurityList}
              className="px-3 py-1.5 rounded-md border border-gray-600 text-gray-200 disabled:opacity-60"
            >
              이전
            </button>
            <span className="text-sm text-gray-300">
              {securityPageMeta.page + 1} / {Math.max(securityPageMeta.totalPages, 1)}
            </span>
            <button
              type="button"
              onClick={() => loadSecurityData(Math.min(securityPageMeta.page + 1, Math.max(securityPageMeta.totalPages - 1, 0)))}
              disabled={securityPageMeta.page >= Math.max(securityPageMeta.totalPages - 1, 0) || isLoadingSecurityList}
              className="px-3 py-1.5 rounded-md border border-gray-600 text-gray-200 disabled:opacity-60"
            >
              다음
            </button>
          </div>
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

function ReportStatusBadge({ status }: { status: string }) {
  if (status === 'PENDING') {
    return <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-500/20 text-amber-300 border border-amber-500/40">PENDING</span>;
  }
  if (status === 'REVIEWED') {
    return <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-blue-500/20 text-blue-300 border border-blue-500/40">REVIEWED</span>;
  }
  if (status === 'RESOLVED') {
    return <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">RESOLVED</span>;
  }
  return <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-gray-500/20 text-gray-300 border border-gray-500/40">{status}</span>;
}

function RiskBadge({ risk }: { risk: string | null }) {
  if (!risk) {
    return <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-gray-500/20 text-gray-300 border border-gray-500/40">-</span>;
  }

  if (risk === 'CRITICAL') {
    return <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-red-500/20 text-red-300 border border-red-500/40">CRITICAL</span>;
  }
  if (risk === 'HIGH') {
    return <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-orange-500/20 text-orange-300 border border-orange-500/40">HIGH</span>;
  }
  if (risk === 'MEDIUM') {
    return <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-yellow-500/20 text-yellow-300 border border-yellow-500/40">MEDIUM</span>;
  }
  return <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">LOW</span>;
}
