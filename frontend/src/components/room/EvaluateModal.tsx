import { useMemo, useState } from 'react';
import { useEvaluateUser } from '@/hooks/queries/useEvaluation';
import type { ParticipantInfo } from '@/types/room';
import { useToast } from '@/hooks/useToast';

type TemperatureChoice = -1 | 0 | 1;

interface EvaluateModalProps {
  roomId: number;
  participants: ParticipantInfo[];
  onClose: () => void;
}

const SCORE_LABEL: Record<TemperatureChoice, string> = {
  1: '따뜻해요',
  0: '보통이에요',
  '-1': '차가워요',
};

const SCORE_STYLE: Record<TemperatureChoice, string> = {
  1: 'border-emerald-400/70 bg-emerald-500/20 text-emerald-200',
  0: 'border-gray-500/70 bg-gray-600/20 text-gray-200',
  '-1': 'border-rose-400/70 bg-rose-500/20 text-rose-200',
};

function toEvaluationScore(choice: TemperatureChoice): number {
  if (choice === 1) return 5;
  if (choice === 0) return 3;
  return 1;
}

export function EvaluateModal({ roomId, participants, onClose }: EvaluateModalProps) {
  const [scoresByUser, setScoresByUser] = useState<Record<number, TemperatureChoice | undefined>>({});
  const evaluateMutation = useEvaluateUser();
  const toast = useToast();

  const isAllSelected = useMemo(
    () => participants.every((participant) => scoresByUser[participant.userId] !== undefined),
    [participants, scoresByUser],
  );

  const handleSelect = (userId: number, score: TemperatureChoice) => {
    setScoresByUser((prev) => ({ ...prev, [userId]: score }));
  };

  const handleSubmit = async () => {
    if (!isAllSelected) {
      toast.error('모든 참가자를 평가해주세요.');
      return;
    }

    try {
      for (const participant of participants) {
        const selected = scoresByUser[participant.userId] as TemperatureChoice;
        const value = toEvaluationScore(selected);
        await evaluateMutation.mutateAsync({
          roomId,
          evaluatedId: participant.userId,
          mannerScore: value,
          skillScore: value,
          communicationScore: value,
        });
      }

      toast.success('평가를 제출했습니다.');
      onClose();
    } catch {
      toast.error('평가 제출에 실패했습니다. 잠시 후 다시 시도해주세요.');
    }
  };

  return (
    <div className="fixed inset-0 z-[70] bg-black/70 flex items-center justify-center p-4">
      <div className="w-full max-w-2xl bg-[#17171b] border border-gray-700 rounded-2xl p-5 sm:p-6">
        <div className="flex items-start justify-between gap-3 mb-4">
          <div>
            <h3 className="text-lg sm:text-xl font-bold text-white">게임 종료 평가</h3>
            <p className="text-sm text-gray-400 mt-1">참가자별로 게임 매너를 평가해주세요.</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="px-3 py-1.5 text-sm rounded-md border border-gray-600 text-gray-300 hover:text-white hover:border-gray-400 transition"
          >
            닫기
          </button>
        </div>

        <div className="space-y-3 max-h-[55vh] overflow-y-auto pr-1">
          {participants.map((participant) => {
            const selected = scoresByUser[participant.userId];
            return (
              <div key={participant.userId} className="rounded-xl border border-gray-700 bg-[#111114] p-3 sm:p-4">
                <div className="flex items-center justify-between gap-3 mb-3">
                  <p className="text-white font-semibold truncate">{participant.nickname}</p>
                  {selected !== undefined && (
                    <span className="text-xs text-gray-300">선택: {SCORE_LABEL[selected]}</span>
                  )}
                </div>
                <div className="grid grid-cols-3 gap-2">
                  {([1, 0, -1] as TemperatureChoice[]).map((score) => {
                    const active = selected === score;
                    return (
                      <button
                        key={score}
                        type="button"
                        onClick={() => handleSelect(participant.userId, score)}
                        className={`px-2 py-2 rounded-lg border text-sm font-medium transition ${
                          active
                            ? SCORE_STYLE[score]
                            : 'border-gray-600 bg-transparent text-gray-300 hover:border-gray-500 hover:text-white'
                        }`}
                      >
                        {SCORE_LABEL[score]}
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>

        <div className="mt-5 flex items-center justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            disabled={evaluateMutation.isPending}
            className="px-4 py-2 rounded-md border border-gray-600 text-gray-300 hover:text-white hover:border-gray-400 disabled:opacity-60 transition"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={!isAllSelected || evaluateMutation.isPending}
            className="px-4 py-2 rounded-md bg-purple-500 text-white font-semibold hover:bg-purple-600 disabled:bg-gray-700 disabled:cursor-not-allowed transition"
          >
            {evaluateMutation.isPending ? '제출 중...' : '평가 제출'}
          </button>
        </div>
      </div>
    </div>
  );
}
