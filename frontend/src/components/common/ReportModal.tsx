import { useState } from 'react';
import reportService from '@/services/reportService';
import { useToast } from '@/hooks/useToast';

type ReportReason = 'SPAM' | 'ABUSIVE' | 'CHEATING' | 'OTHER';

interface ReportModalProps {
  reportedUserId: number;
  reportedNickname: string;
  chatMessageId?: number;
  onClose: () => void;
}

const REASON_OPTIONS: { value: ReportReason; label: string }[] = [
  { value: 'SPAM', label: '스팸/광고' },
  { value: 'ABUSIVE', label: '욕설/비방' },
  { value: 'CHEATING', label: '비매너/게임 방해' },
  { value: 'OTHER', label: '기타' },
];

export function ReportModal({
  reportedUserId,
  reportedNickname,
  chatMessageId,
  onClose,
}: ReportModalProps) {
  const [reason, setReason] = useState<ReportReason>('ABUSIVE');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const toast = useToast();

  const handleSubmit = async () => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    try {
      await reportService.createReport(reportedUserId, reason, chatMessageId);
      toast.success('신고가 접수되었습니다');
      onClose();
    } catch {
      toast.error('신고 접수에 실패했습니다');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-[70]">
      <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 max-w-md w-full mx-4">
        <h3 className="text-lg font-bold text-white mb-1">사용자 신고</h3>
        <p className="text-sm text-gray-400 mb-4">
          <span className="text-white font-medium">{reportedNickname}</span>님을 신고합니다.
        </p>

        <div className="space-y-2 mb-6">
          {REASON_OPTIONS.map((option) => (
            <label
              key={option.value}
              className="flex items-center gap-2 rounded border border-gray-700 px-3 py-2 text-sm text-gray-200 cursor-pointer hover:border-purple-500 transition"
            >
              <input
                type="radio"
                name="report-reason"
                value={option.value}
                checked={reason === option.value}
                onChange={() => setReason(option.value)}
                className="accent-purple-500"
              />
              {option.label}
            </label>
          ))}
        </div>

        <div className="flex gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="flex-1 px-4 py-2 bg-gray-700 hover:bg-gray-600 disabled:opacity-60 text-white font-semibold rounded transition"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="flex-1 px-4 py-2 bg-red-500 hover:bg-red-600 disabled:opacity-60 text-white font-semibold rounded transition"
          >
            {isSubmitting ? '신고 중...' : '신고하기'}
          </button>
        </div>
      </div>
    </div>
  );
}

