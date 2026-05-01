import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import userService from '@/services/userService';
import { useGames } from '@/hooks/queries/useGames';
import { Gamepad2, ChevronRight, Check, Loader2 } from 'lucide-react';

const MAX_SELECTION = 3;
const ONBOARDING_PREF_KEY = 'onboarding:selectedGameIds';
const ONBOARDING_DONE_KEY = 'onboarding:completedUserIds';

// Game genre → color mapping for visual variety
const GENRE_COLORS: Record<string, { bg: string; border: string; text: string; glow: string }> = {
  'FPS': { bg: 'bg-red-500/10', border: 'border-red-500/40', text: 'text-red-400', glow: 'shadow-red-500/20' },
  'RPG': { bg: 'bg-yellow-500/10', border: 'border-yellow-500/40', text: 'text-yellow-400', glow: 'shadow-yellow-500/20' },
  'MOBA': { bg: 'bg-blue-500/10', border: 'border-blue-500/40', text: 'text-blue-400', glow: 'shadow-blue-500/20' },
  'Battle Royale': { bg: 'bg-orange-500/10', border: 'border-orange-500/40', text: 'text-orange-400', glow: 'shadow-orange-500/20' },
  'Sports': { bg: 'bg-green-500/10', border: 'border-green-500/40', text: 'text-green-400', glow: 'shadow-green-500/20' },
  'Strategy': { bg: 'bg-cyan-500/10', border: 'border-cyan-500/40', text: 'text-cyan-400', glow: 'shadow-cyan-500/20' },
};

const DEFAULT_COLOR = { bg: 'bg-purple-500/10', border: 'border-purple-500/40', text: 'text-purple-400', glow: 'shadow-purple-500/20' };

function getGenreColor(genre: string) {
  for (const key of Object.keys(GENRE_COLORS)) {
    if (genre?.toLowerCase().includes(key.toLowerCase())) {
      return GENRE_COLORS[key];
    }
  }
  return DEFAULT_COLOR;
}

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
        user: state.user ? { ...state.user, nickname: updated.nickname } : state.user,
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
      if (prev.includes(gameId)) return prev.filter((id) => id !== gameId);
      if (prev.length >= MAX_SELECTION) return prev;
      return [...prev, gameId];
    });
  };

  const handleComplete = () => {
    localStorage.setItem(ONBOARDING_PREF_KEY, JSON.stringify(selectedGameIds));
    markOnboardingDone();
    navigate('/', { replace: true });
  };

  return (
    <div className="min-h-screen bg-[#0e0e10] text-white flex flex-col">
      {/* Background effects */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[600px] h-[400px] bg-purple-600/10 rounded-full blur-3xl" />
        <div className="absolute bottom-0 right-0 w-[400px] h-[400px] bg-indigo-600/10 rounded-full blur-3xl" />
        <div
          className="absolute inset-0 opacity-5"
          style={{
            backgroundImage: `linear-gradient(rgba(139,92,246,0.5) 1px, transparent 1px),
              linear-gradient(90deg, rgba(139,92,246,0.5) 1px, transparent 1px)`,
            backgroundSize: '60px 60px',
          }}
        />
      </div>

      <div className="relative z-10 flex flex-col items-center justify-center flex-1 px-4 py-10">
        <div className="w-full max-w-lg">
          {/* Logo */}
          <div className="flex items-center justify-center gap-2 mb-10">
            <div className="w-8 h-8 rounded-lg bg-purple-600/30 border border-purple-500/50 flex items-center justify-center">
              <Gamepad2 size={16} className="text-purple-400" />
            </div>
            <span className="text-xl font-bold text-white font-['Orbitron',sans-serif] tracking-wider">GEMBUD</span>
          </div>

          {/* Step indicators */}
          <div className="flex items-center justify-center gap-3 mb-8">
            {[1, 2].map((s) => (
              <div key={s} className="flex items-center gap-3">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-all duration-300 ${
                    s < step
                      ? 'bg-purple-600 text-white'
                      : s === step
                      ? 'bg-purple-600/30 border-2 border-purple-500 text-purple-300'
                      : 'bg-gray-800 border border-gray-700 text-gray-600'
                  }`}
                >
                  {s < step ? <Check size={14} /> : s}
                </div>
                {s < 2 && (
                  <div className="w-16 h-0.5 rounded-full overflow-hidden bg-gray-800">
                    <div
                      className="h-full bg-purple-600 transition-all duration-500"
                      style={{ width: step > s ? '100%' : '0%' }}
                    />
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* Progress bar */}
          <div className="h-1 rounded-full bg-gray-800 mb-8 overflow-hidden">
            <div
              className="h-full rounded-full bg-gradient-to-r from-purple-600 to-pink-500 transition-all duration-500"
              style={{ width: `${progress}%` }}
            />
          </div>

          {/* Step 1 — Nickname */}
          {step === 1 && (
            <div className="rounded-2xl border border-gray-800 bg-[#18181b]/80 backdrop-blur-sm p-8">
              <div className="mb-6">
                <h1 className="text-2xl font-bold text-white mb-2">환영합니다! 👋</h1>
                <p className="text-gray-400">게임 파티원 찾기 전에 닉네임을 설정해 주세요.</p>
              </div>

              <div className="space-y-2">
                <label htmlFor="nickname" className="block text-sm font-medium text-gray-300">
                  닉네임
                </label>
                <input
                  id="nickname"
                  type="text"
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleNext(); }}
                  className="w-full rounded-xl border border-gray-700 bg-[#0e0e10] px-4 py-3 text-white placeholder-gray-600 outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500/30 transition-all"
                  placeholder="사용할 닉네임 입력"
                  maxLength={20}
                />
                <div className="flex items-center justify-between">
                  {error ? (
                    <p className="text-sm text-red-400">{error}</p>
                  ) : (
                    <p className="text-xs text-gray-600">영문, 한글, 숫자 사용 가능</p>
                  )}
                  <span className="text-xs text-gray-600">{nickname.length}/20</span>
                </div>
              </div>

              <button
                type="button"
                onClick={handleNext}
                disabled={isSubmitting || !nickname.trim()}
                className="mt-6 w-full flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-purple-600 to-purple-500 px-4 py-3 font-semibold text-white hover:from-purple-500 hover:to-purple-400 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-lg shadow-purple-500/20"
              >
                {isSubmitting ? (
                  <><Loader2 size={16} className="animate-spin" /> 저장 중...</>
                ) : (
                  <>다음 <ChevronRight size={16} /></>
                )}
              </button>
            </div>
          )}

          {/* Step 2 — Game selection */}
          {step === 2 && (
            <div className="rounded-2xl border border-gray-800 bg-[#18181b]/80 backdrop-blur-sm p-8">
              <div className="mb-6">
                <h1 className="text-2xl font-bold text-white mb-2">관심 게임 선택 🎮</h1>
                <p className="text-gray-400">최대 3개까지 선택하면 맞춤 파티원을 추천해 드려요.</p>
              </div>

              {isGamesLoading ? (
                <div className="flex items-center justify-center py-12 text-gray-500">
                  <Loader2 size={20} className="animate-spin mr-2" /> 게임 목록 불러오는 중...
                </div>
              ) : (
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 max-h-72 overflow-y-auto pr-1">
                  {games.map((game) => {
                    const selected = selectedGameIds.includes(game.id);
                    const color = getGenreColor(game.genre ?? '');
                    const disabled = !selected && selectedGameIds.length >= MAX_SELECTION;
                    return (
                      <button
                        key={game.id}
                        type="button"
                        onClick={() => toggleGame(game.id)}
                        disabled={disabled}
                        className={`relative rounded-xl border p-3 text-left transition-all duration-200 ${
                          selected
                            ? `${color.bg} ${color.border} shadow-lg ${color.glow}`
                            : disabled
                            ? 'border-gray-800 bg-[#0e0e10] opacity-40 cursor-not-allowed'
                            : 'border-gray-800 bg-[#0e0e10] hover:border-gray-600'
                        }`}
                      >
                        {selected && (
                          <div className={`absolute top-2 right-2 w-5 h-5 rounded-full ${color.bg} ${color.border} border flex items-center justify-center`}>
                            <Check size={10} className={color.text} />
                          </div>
                        )}
                        <p className="font-semibold text-sm text-white pr-5 truncate">{game.name}</p>
                        <p className={`mt-1 text-xs ${selected ? color.text : 'text-gray-500'}`}>
                          {game.genre ?? 'Game'}
                        </p>
                      </button>
                    );
                  })}
                </div>
              )}

              <div className="mt-6 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-400">
                    <span className={selectedGameIds.length > 0 ? 'text-purple-400 font-semibold' : ''}>
                      {selectedGameIds.length}
                    </span>
                    /{MAX_SELECTION} 선택
                  </span>
                  {selectedGameIds.length === MAX_SELECTION && (
                    <span className="text-xs text-purple-400 bg-purple-500/10 px-2 py-0.5 rounded-full">최대 선택!</span>
                  )}
                </div>
                <button
                  type="button"
                  onClick={handleComplete}
                  className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-purple-600 to-purple-500 px-5 py-2.5 font-semibold text-white hover:from-purple-500 hover:to-purple-400 transition-all duration-200 shadow-lg shadow-purple-500/20"
                >
                  시작하기 <ChevronRight size={16} />
                </button>
              </div>

              <button
                type="button"
                onClick={handleComplete}
                className="mt-3 w-full text-center text-xs text-gray-600 hover:text-gray-400 transition-colors"
              >
                건너뛰기
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
