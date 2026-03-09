import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Star, Pencil, UserPlus, Flag } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { useGetEvaluations, useUserTemperature } from '@/hooks/queries/useEvaluation';
import {
  useFriendRequests,
  useFriends,
  useSendFriendRequest,
  useSentFriendRequests,
} from '@/hooks/queries/useFriends';
import { useToast } from '@/hooks/useToast';
import { EditProfileModal } from '@/components/profile/EditProfileModal';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import reportService from '@/services/reportService';
import type { Evaluation } from '@/types/evaluation';

/**
 * User profile page with real API data.
 *
 * @author Gembud Team
 * @since 2026-03-03
 */
export default function ProfilePage() {
  const { userId } = useParams<{ userId?: string }>();
  const { user } = useAuthStore();
  const toast = useToast();

  const [showEditModal, setShowEditModal] = useState(false);

  const parsedUserId = userId ? Number(userId) : undefined;
  const targetUserId = Number.isFinite(parsedUserId) ? parsedUserId : user?.id;
  const isOwnProfile = Boolean(user && targetUserId === user.id);

  const { data: evaluations, isLoading: evalsLoading } = useGetEvaluations(targetUserId ?? 0);
  const { data: tempStats, isLoading: tempLoading } = useUserTemperature(targetUserId ?? 0);
  const { data: myReports = [], isLoading: reportsLoading } = useQuery({
    queryKey: ['myReports'],
    queryFn: reportService.getMyReports,
    enabled: isOwnProfile,
  });

  const { data: friends = [] } = useFriends();
  const { data: sentRequests = [] } = useSentFriendRequests();
  const { data: receivedRequests = [] } = useFriendRequests();
  const sendRequestMutation = useSendFriendRequest();

  const profileRelation = useMemo<'FRIEND' | 'PENDING' | 'NONE'>(() => {
    if (!targetUserId || !user) return 'NONE';

    const isFriend = friends.some((friend) => {
      const counterpartId = friend.userId === user.id ? friend.friendId : friend.userId;
      return counterpartId === targetUserId;
    });
    if (isFriend) return 'FRIEND';

    const pendingSent = sentRequests.some(
      (request) => request.friendId === targetUserId && request.status === 'PENDING'
    );
    const pendingReceived = receivedRequests.some(
      (request) => request.userId === targetUserId && request.status === 'PENDING'
    );

    return pendingSent || pendingReceived ? 'PENDING' : 'NONE';
  }, [friends, receivedRequests, sentRequests, targetUserId, user]);

  const displayName = useMemo(() => {
    if (isOwnProfile) {
      return user?.nickname ?? '내 프로필';
    }

    const fromFriends = friends.find((friend) => {
      const counterpartId = user && friend.userId === user.id ? friend.friendId : friend.userId;
      return counterpartId === targetUserId;
    });
    if (fromFriends) {
      return user && fromFriends.userId === user.id
        ? fromFriends.friendNickname
        : fromFriends.userNickname;
    }

    const fromSent = sentRequests.find((request) => request.friendId === targetUserId);
    if (fromSent) return fromSent.friendNickname;

    const fromReceived = receivedRequests.find((request) => request.userId === targetUserId);
    if (fromReceived) return fromReceived.userNickname;

    return targetUserId ? `사용자 ${targetUserId}` : '사용자';
  }, [friends, isOwnProfile, receivedRequests, sentRequests, targetUserId, user]);

  const handleSendFriendRequest = () => {
    if (!targetUserId || isOwnProfile || profileRelation !== 'NONE') return;

    sendRequestMutation.mutate(targetUserId, {
      onSuccess: () => {
        toast.success('친구 요청을 보냈습니다');
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || '친구 요청 실패');
      },
    });
  };

  const temperature = tempStats?.currentTemperature ?? user?.temperature ?? 36.5;
  const evaluationCount = tempStats?.evaluationCount ?? evaluations?.length ?? 0;

  const tempPercent = Math.min(100, Math.max(0, ((temperature - 20) / (60 - 20)) * 100));

  const avgMannerScore = tempStats?.averageMannerScore ?? 0;
  const avgSkillScore = tempStats?.averageSkillScore ?? 0;
  const avgCommScore = tempStats?.averageCommunicationScore ?? 0;

  const formatDate = (isoString: string) => {
    const diff = Date.now() - new Date(isoString).getTime();
    const days = Math.floor(diff / 86400000);
    if (days === 0) return '오늘';
    if (days === 1) return '1일 전';
    if (days < 7) return `${days}일 전`;
    if (days < 30) return `${Math.floor(days / 7)}주 전`;
    return `${Math.floor(days / 30)}달 전`;
  };

  const renderStars = (score: number) => {
    const filled = Math.round(score);
    return [1, 2, 3, 4, 5].map((s) => (
      <Star
        key={s}
        size={14}
        className={s <= filled ? 'text-yellow-400 fill-yellow-400' : 'text-gray-600'}
      />
    ));
  };

  const renderFriendButton = () => {
    if (isOwnProfile || !targetUserId) return null;

    if (profileRelation === 'FRIEND') {
      return (
        <button
          type="button"
          disabled
          className="px-4 py-2 rounded border border-emerald-400/40 bg-emerald-500/20 text-emerald-300 font-semibold"
        >
          친구
        </button>
      );
    }

    if (profileRelation === 'PENDING') {
      return (
        <button
          type="button"
          disabled
          className="px-4 py-2 rounded border border-amber-400/40 bg-amber-500/20 text-amber-300 font-semibold"
        >
          친구 요청됨
        </button>
      );
    }

    return (
      <button
        type="button"
        onClick={handleSendFriendRequest}
        disabled={sendRequestMutation.isPending}
        className="flex items-center gap-2 px-4 py-2 bg-purple-500 hover:bg-purple-600 rounded font-semibold transition disabled:bg-gray-600"
      >
        <UserPlus size={15} />
        {sendRequestMutation.isPending ? '요청 중...' : '친구 추가'}
      </button>
    );
  };

  const reportStatusStyle = (status: string) => {
    if (status === 'PENDING') {
      return 'bg-amber-500/20 text-amber-300 border border-amber-400/40';
    }
    if (status === 'RESOLVED') {
      return 'bg-emerald-500/20 text-emerald-300 border border-emerald-400/40';
    }
    if (status === 'REJECTED' || status === 'REVIEWED') {
      return 'bg-rose-500/20 text-rose-300 border border-rose-400/40';
    }
    return 'bg-gray-500/20 text-gray-300 border border-gray-400/40';
  };

  const reportStatusLabel = (status: string) => {
    if (status === 'PENDING') return 'PENDING';
    if (status === 'RESOLVED') return 'RESOLVED';
    if (status === 'REJECTED' || status === 'REVIEWED') return 'REJECTED';
    return status;
  };

  return (
    <>
      {showEditModal && user && (
        <EditProfileModal
          currentNickname={user.nickname}
          onClose={() => setShowEditModal(false)}
        />
      )}
      <div className="min-h-screen bg-[#0e0e10] text-white">
        <div className="container mx-auto px-4 py-8 max-w-4xl">
          {/* Profile Header */}
          <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-8 mb-6">
            <div className="flex items-start gap-6">
              {/* Avatar */}
              <div className="w-24 h-24 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center text-4xl font-bold flex-shrink-0">
                {displayName?.[0] || '?'}
              </div>

              {/* Info */}
              <div className="flex-1">
                <h1 className="text-3xl font-bold mb-2">{displayName}</h1>
                <p className="text-gray-400 mb-4">{isOwnProfile ? user?.email : ''}</p>

                {/* Stats */}
                <div className="flex gap-6">
                  <div>
                    <div className="text-2xl font-bold text-purple-400">
                      {tempLoading ? '...' : `${temperature.toFixed(1)}°C`}
                    </div>
                    <div className="text-sm text-gray-400">매너 온도</div>
                  </div>
                  <div>
                    <div className="text-2xl font-bold">
                      {tempLoading ? '...' : evaluationCount}
                    </div>
                    <div className="text-sm text-gray-400">받은 평가</div>
                  </div>
                  {tempStats && (
                    <>
                      <div>
                        <div className="text-2xl font-bold text-blue-400">
                          {avgMannerScore.toFixed(1)}
                        </div>
                        <div className="text-sm text-gray-400">매너 점수</div>
                      </div>
                      <div>
                        <div className="text-2xl font-bold text-green-400">
                          {avgSkillScore.toFixed(1)}
                        </div>
                        <div className="text-sm text-gray-400">실력 점수</div>
                      </div>
                    </>
                  )}
                </div>
              </div>

              {/* Action buttons */}
              <div className="flex gap-2">
                {renderFriendButton()}
                {!isOwnProfile && (
                  <button
                    type="button"
                    onClick={() => toast.info('신고 기능은 준비 중입니다')}
                    className="flex items-center gap-2 px-4 py-2 bg-red-500/20 hover:bg-red-500/30 border border-red-400/40 rounded font-semibold transition"
                  >
                    <Flag size={15} />
                    신고
                  </button>
                )}
                {isOwnProfile && (
                  <button
                    onClick={() => setShowEditModal(true)}
                    className="flex items-center gap-2 px-4 py-2 bg-purple-500 hover:bg-purple-600 rounded font-semibold transition"
                  >
                    <Pencil size={15} />
                    프로필 수정
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Temperature Bar */}
          <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 mb-6">
            <h2 className="text-xl font-semibold mb-4">
              매너 온도
              <span className="ml-3 text-lg font-bold text-purple-400">{temperature.toFixed(1)}°C</span>
            </h2>
            <div className="relative h-6 bg-gray-700 rounded-full overflow-hidden">
              <div
                className="absolute inset-y-0 left-0 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500 rounded-full transition-all duration-500"
                style={{ width: `${tempPercent}%` }}
              />
            </div>
            <p className="text-sm text-gray-400 mt-2">
              게임 매너가 좋으면 온도가 올라갑니다!
            </p>
          </div>

          {/* Score Breakdown */}
          {tempStats && (
            <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 mb-6">
              <h2 className="text-xl font-semibold mb-4">평가 점수 상세</h2>
              <div className="grid grid-cols-3 gap-4">
                {[
                  { label: '매너', score: avgMannerScore, color: 'text-blue-400' },
                  { label: '실력', score: avgSkillScore, color: 'text-green-400' },
                  { label: '소통', score: avgCommScore, color: 'text-yellow-400' },
                ].map(({ label, score, color }) => (
                  <div key={label} className="text-center p-4 bg-[#0e0e10] rounded-lg">
                    <div className={`text-3xl font-bold ${color} mb-1`}>{score.toFixed(1)}</div>
                    <div className="text-sm text-gray-400 mb-2">{label}</div>
                    <div className="flex justify-center">{renderStars(score)}</div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Recent Evaluations */}
          <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6">
            <h2 className="text-xl font-semibold mb-4">최근 받은 평가</h2>

            {evalsLoading ? (
              <LoadingSpinner className="py-8" />
            ) : !evaluations || evaluations.length === 0 ? (
              <div className="text-center py-8 text-gray-400">
                <p>아직 받은 평가가 없습니다</p>
              </div>
            ) : (
              <div className="space-y-4">
                {evaluations.slice(0, 10).map((ev: Evaluation) => (
                  <div key={ev.id} className="p-4 bg-[#0e0e10] rounded-lg">
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-semibold text-gray-300">익명의 평가자</span>
                      <div className="flex items-center gap-1">
                        {renderStars(ev.averageScore)}
                        <span className="text-xs text-gray-400 ml-1">{ev.averageScore.toFixed(1)}</span>
                      </div>
                    </div>
                    <div className="flex gap-4 text-sm text-gray-400">
                      <span>매너 {ev.mannerScore}/5</span>
                      <span>실력 {ev.skillScore}/5</span>
                      <span>소통 {ev.communicationScore}/5</span>
                    </div>
                    <p className="text-gray-500 text-xs mt-2">{formatDate(ev.createdAt)}</p>
                  </div>
                ))}
              </div>
            )}
          </div>

          {isOwnProfile && (
            <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 mt-6">
              <h2 className="text-xl font-semibold mb-4">내 신고 내역</h2>
              {reportsLoading ? (
                <LoadingSpinner className="py-8" />
              ) : myReports.length === 0 ? (
                <div className="text-center py-8 text-gray-400">
                  <p>등록된 신고 내역이 없습니다</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {myReports.slice(0, 10).map((report) => (
                    <div key={report.id} className="rounded bg-[#0e0e10] p-4">
                      <div className="flex items-center justify-between gap-3">
                        <div>
                          <p className="font-semibold text-gray-200">
                            {report.reported?.nickname ?? `사용자 ${report.reported?.id ?? ''}`}
                          </p>
                          <p className="text-sm text-gray-400 mt-1">{report.reason}</p>
                        </div>
                        <span className={`px-2 py-0.5 text-xs rounded-full font-semibold ${reportStatusStyle(report.status)}`}>
                          {reportStatusLabel(report.status)}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </>
  );
}
