import { Crown, Zap, Star, Shield, X } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { useSubscriptionStatus, useActivatePremium, useCancelPremium } from '@/hooks/queries/useSubscription';
import { useToast } from '@/hooks/useToast';
import Button from '@/components/common/Button';
import PremiumBadge from '@/components/common/PremiumBadge';

const BENEFITS = [
  {
    icon: <X size={16} className="text-neon-pink" />,
    title: '광고 완전 제거',
    description: '모든 광고 없이 깔끔한 화면으로 이용',
    free: false,
    premium: true,
  },
  {
    icon: <Zap size={16} className="text-neon-cyan" />,
    title: 'AI 추천 방 확대',
    description: '추천 방 3개 → 최대 20개',
    free: '3개',
    premium: '최대 20개',
  },
  {
    icon: <Star size={16} className="text-yellow-400" />,
    title: '방 목록 상단 노출',
    description: '내가 만든 방이 목록 상단에 노출',
    free: false,
    premium: true,
  },
  {
    icon: <Shield size={16} className="text-neon-purple" />,
    title: '프리미엄 배지',
    description: '프로필에 PRO 배지 표시',
    free: false,
    premium: true,
  },
];

export default function PremiumPage() {
  const { user } = useAuthStore();
  const { data: status, isLoading } = useSubscriptionStatus();
  const { mutate: activate, isPending: isActivating } = useActivatePremium();
  const { mutate: cancel, isPending: isCancelling } = useCancelPremium();
  const { success, error } = useToast();

  const isPremium = status?.isPremium ?? user?.isPremium ?? false;

  function handleActivate() {
    activate(1, {
      onSuccess: () => success('프리미엄이 활성화되었습니다!'),
      onError: () => error('활성화에 실패했습니다.'),
    });
  }

  function handleCancel() {
    cancel(undefined, {
      onSuccess: () => success('구독이 취소되었습니다.'),
      onError: () => error('취소에 실패했습니다.'),
    });
  }

  return (
    <div className="max-w-3xl mx-auto space-y-12 py-8">
      {/* Header */}
      <section className="text-center space-y-4">
        <div className="flex items-center justify-center gap-3">
          <Crown size={36} className="text-yellow-400" />
          <h1 className="font-display text-h1 bg-gradient-to-r from-yellow-400 to-amber-300 bg-clip-text text-transparent">
            GEMBUD PRO
          </h1>
        </div>
        <p className="text-text-secondary text-lg">
          더 나은 게이밍 경험을 위한 프리미엄 멤버십
        </p>
        <div className="flex items-baseline justify-center gap-1">
          <span className="text-4xl font-bold text-white">₩2,900</span>
          <span className="text-text-muted">/월</span>
        </div>
      </section>

      {/* Current Status */}
      {!isLoading && isPremium && (
        <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-card p-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <PremiumBadge />
            <div>
              <p className="text-white font-medium">프리미엄 구독 중</p>
              {status?.premiumExpiresAt && (
                <p className="text-text-muted text-sm">
                  만료: {new Date(status.premiumExpiresAt).toLocaleDateString('ko-KR')}
                </p>
              )}
            </div>
          </div>
          <Button
            variant="secondary"
            size="sm"
            onClick={handleCancel}
            disabled={isCancelling}
          >
            구독 취소
          </Button>
        </div>
      )}

      {/* Benefits Table */}
      <section className="space-y-4">
        <h2 className="font-display text-h2 text-white text-center">혜택 비교</h2>
        <div className="overflow-hidden rounded-card border border-neon-purple/20">
          <table className="w-full">
            <thead>
              <tr className="bg-dark-secondary border-b border-neon-purple/20">
                <th className="text-left px-6 py-4 text-text-secondary font-gaming text-sm">기능</th>
                <th className="text-center px-6 py-4 text-text-secondary font-gaming text-sm">무료</th>
                <th className="text-center px-6 py-4 text-yellow-400 font-gaming text-sm">
                  <span className="flex items-center justify-center gap-1">
                    <Crown size={14} />
                    PRO
                  </span>
                </th>
              </tr>
            </thead>
            <tbody>
              {BENEFITS.map((benefit, i) => (
                <tr
                  key={i}
                  className="border-b border-neon-purple/10 last:border-0 hover:bg-dark-secondary/50 transition-colors"
                >
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      {benefit.icon}
                      <div>
                        <p className="text-text-primary font-medium">{benefit.title}</p>
                        <p className="text-text-muted text-xs">{benefit.description}</p>
                      </div>
                    </div>
                  </td>
                  <td className="text-center px-6 py-4">
                    {benefit.free === false ? (
                      <span className="text-text-muted text-lg">✕</span>
                    ) : (
                      <span className="text-text-secondary text-sm font-gaming">{benefit.free}</span>
                    )}
                  </td>
                  <td className="text-center px-6 py-4">
                    {benefit.premium === true ? (
                      <span className="text-neon-cyan text-lg">✓</span>
                    ) : (
                      <span className="text-neon-cyan text-sm font-gaming">{benefit.premium}</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* CTA */}
      {!isPremium && (
        <div className="flex justify-center">
          <Button
            variant="primary"
            size="lg"
            onClick={handleActivate}
            disabled={isActivating || !user}
            className="min-w-48"
          >
            <Crown size={18} className="mr-2" />
            {isActivating ? '처리 중...' : '1개월 구독 시작 (₩2,900)'}
          </Button>
        </div>
      )}

      <p className="text-center text-text-muted text-xs">
        * 현재 관리자/테스트 모드로 결제 없이 활성화 가능합니다. 추후 토스페이/카카오페이 연동 예정.
      </p>
    </div>
  );
}
