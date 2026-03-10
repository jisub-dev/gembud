import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import userService from '@/services/userService';
import { useGames } from '@/hooks/queries/useGames';

const MAX_SELECTION = 3;
const ONBOARDING_PREF_KEY = 'onboarding:selectedGameIds';
const ONBOARDING_DONE_KEY = 'onboarding:completedUserIds';

export default function OnboardingPage() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [step, setStep] = useState<1 | 2>(1);
  const [nickname, setNickname] = useState(user?.nickname ?? '');
  const [selectedGameIds, setSelectedGameIds] = useState<number[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { data: games = [], isLoading: isGamesLoading } = useGames();

  const progress = useMemo(() => (step === 1 ? 50 : 100), [step]);

  const markOnboardingDone = () => {
    if (!user?.id) return;
    const stored = localStorage.getItem(ONBOARDING_DONE_KEY);
    const completedIds: number[] = stored ? JSON.parse(stored) : [];
    if (!completedIds.includes(user.id)) {
      completedIds.push(user.id);
      localStorage.setItem(ONBOARDING_DONE_KEY, JSON.stringify(completedIds));
    }
  };

  const handleNext = async () => {
    if (!nickname.trim()) {
      setError('닉네임을 입력해 주세요.');
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      const updated = await userService.updateProfile({ nickname: nickname.trim() });
      useAuthStore.setState((state) => ({
        ...state,
        user: state.user
          ? {
              ...state.user,
              nickname: updated.nickname,
            }
          : state.user,
      }));
      setStep(2);
    } catch {
      setError('닉네임 저장에 실패했습니다. 잠시 후 다시 시도해 주세요.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const toggleGame = (gameId: number) => {
    setSelectedGameIds((prev) => {
      if (prev.includes(gameId)) {
        return prev.filter((id) => id !== gameId);
      }

      if (prev.length >= MAX_SELECTION) {
        return prev;
      }

      return [...prev, gameId];
    });
  };

  const handleComplete = () => {
    localStorage.setItem(ONBOARDING_PREF_KEY, JSON.stringify(selectedGameIds));
    markOnboardingDone();
    navigate('/', { replace: true });
  };

  return (
    <div className="min-h-screen bg-[#0e0e10] text-white">
      <div className="mx-auto w-full max-w-3xl px-4 py-10">
        <div className="mb-8 space-y-2">
          <p className="text-sm text-gray-300">{step}/2</p>
          <div className="h-2 rounded-full bg-gray-800">
            <div
              className="h-2 rounded-full bg-blue-500 transition-all duration-300"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>

        {step === 1 && (
          <section className="rounded-xl border border-gray-800 bg-[#18181b] p-6">
            <h1 className="text-2xl font-bold">환영합니다!</h1>
            <p className="mt-2 text-sm text-gray-400">사용할 닉네임을 설정해 주세요.</p>

            <div className="mt-6 space-y-2">
              <label htmlFor="nickname" className="text-sm text-gray-300">
                닉네임
              </label>
              <input
                id="nickname"
                type="text"
                value={nickname}
                onChange={(event) => setNickname(event.target.value)}
                className="w-full rounded-lg border border-gray-700 bg-[#101114] px-3 py-2 text-white outline-none focus:border-blue-500"
                placeholder="닉네임을 입력하세요"
              />
              {error && <p className="text-sm text-red-400">{error}</p>}
            </div>

            <button
              type="button"
              onClick={handleNext}
              disabled={isSubmitting}
              className="mt-6 w-full rounded-lg bg-blue-600 px-4 py-2 font-semibold hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isSubmitting ? '저장 중...' : '다음'}
            </button>
          </section>
        )}

        {step === 2 && (
          <section className="rounded-xl border border-gray-800 bg-[#18181b] p-6">
            <h1 className="text-2xl font-bold">관심 게임을 선택해 주세요</h1>
            <p className="mt-2 text-sm text-gray-400">최대 3개까지 선택할 수 있습니다.</p>

            <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3">
              {isGamesLoading && (
                <p className="col-span-full text-sm text-gray-400">게임 목록을 불러오는 중입니다...</p>
              )}
              {!isGamesLoading && games.map((game) => {
                const selected = selectedGameIds.includes(game.id);
                return (
                  <button
                    key={game.id}
                    type="button"
                    onClick={() => toggleGame(game.id)}
                    className={`rounded-lg border p-3 text-left transition ${selected ? 'border-blue-500 bg-blue-500/10' : 'border-gray-700 bg-[#101114] hover:border-gray-500'}`}
                  >
                    <p className="font-semibold">{game.name}</p>
                    <p className="mt-1 text-xs text-gray-400">{game.genre}</p>
                  </button>
                );
              })}
            </div>

            <div className="mt-6 flex items-center justify-between">
              <p className="text-sm text-gray-400">{selectedGameIds.length}/{MAX_SELECTION} 선택</p>
              <button
                type="button"
                onClick={handleComplete}
                className="rounded-lg bg-blue-600 px-4 py-2 font-semibold hover:bg-blue-500"
              >
                완료
              </button>
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
