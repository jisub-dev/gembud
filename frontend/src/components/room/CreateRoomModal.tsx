import { useState } from 'react';
import { X, Gamepad2, Lock } from 'lucide-react';
import { useCreateRoom } from '@/hooks/queries/useRooms';
import { useToast } from '@/hooks/useToast';
import type { CreateRoomRequest } from '@/types/room';

interface CreateRoomModalProps {
  gameId: number;
  gameName: string;
  onClose: () => void;
  onSuccess?: () => void;
}

export function CreateRoomModal({ gameId, gameName, onClose, onSuccess }: CreateRoomModalProps) {
  const [formData, setFormData] = useState<CreateRoomRequest>({
    gameId,
    title: '',
    description: '',
    maxParticipants: 5,
    isPrivate: false,
    password: '',
  });

  const createRoomMutation = useCreateRoom();
  const toast = useToast();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.title.trim()) {
      toast.error('방 제목을 입력해주세요');
      return;
    }

    if (formData.isPrivate && !formData.password?.trim()) {
      toast.error('비공개 방은 비밀번호가 필요합니다');
      return;
    }

    const dataToSubmit = {
      ...formData,
      password: formData.isPrivate ? formData.password : undefined,
    };

    createRoomMutation.mutate(dataToSubmit, {
      onSuccess: () => {
        toast.success('방이 생성되었습니다!');
        onSuccess?.();
        onClose();
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || '방 생성에 실패했습니다');
      },
    });
  };

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-700">
          <h2 className="text-2xl font-bold text-white">방 만들기</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white w-8 h-8 flex items-center justify-center rounded hover:bg-gray-700 transition"
          >
            <X size={20} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* Game Name (Read-only) */}
          <div>
            <label className="block text-sm font-semibold text-gray-300 mb-2">게임</label>
            <div className="flex items-center gap-2 px-4 py-3 bg-[#0e0e10] border border-gray-600 rounded text-gray-400">
              <Gamepad2 size={16} />
              {gameName}
            </div>
          </div>

          {/* Title */}
          <div>
            <label className="block text-sm font-semibold text-gray-300 mb-2">
              방 제목 <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              placeholder="예: 골드 이상만 | 랭크 같이 가요"
              className="w-full px-4 py-3 bg-[#0e0e10] border border-gray-600 rounded focus:border-purple-500 focus:outline-none text-white"
              maxLength={50}
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-semibold text-gray-300 mb-2">방 설명</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="함께 게임할 때 원하는 조건이나 플레이 스타일을 적어주세요"
              className="w-full px-4 py-3 bg-[#0e0e10] border border-gray-600 rounded focus:border-purple-500 focus:outline-none text-white resize-none"
              rows={4}
              maxLength={500}
            />
          </div>

          {/* Max Participants */}
          <div>
            <label className="block text-sm font-semibold text-gray-300 mb-2">최대 인원</label>
            <select
              value={formData.maxParticipants}
              onChange={(e) => setFormData({ ...formData, maxParticipants: Number(e.target.value) })}
              className="w-full px-4 py-3 bg-[#0e0e10] border border-gray-600 rounded focus:border-purple-500 focus:outline-none text-white"
            >
              {[2, 3, 4, 5, 6, 8, 10].map((num) => (
                <option key={num} value={num}>{num}명</option>
              ))}
            </select>
          </div>

          {/* Private Room */}
          <div>
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={formData.isPrivate}
                onChange={(e) => setFormData({ ...formData, isPrivate: e.target.checked })}
                className="w-5 h-5 accent-purple-500"
              />
              <span className="flex items-center gap-2 text-sm font-semibold text-gray-300">
                <Lock size={14} />
                비공개 방 (비밀번호 필요)
              </span>
            </label>
          </div>

          {/* Password (if private) */}
          {formData.isPrivate && (
            <div>
              <label className="block text-sm font-semibold text-gray-300 mb-2">
                비밀번호 <span className="text-red-400">*</span>
              </label>
              <input
                type="password"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                placeholder="4~20자"
                className="w-full px-4 py-3 bg-[#0e0e10] border border-gray-600 rounded focus:border-purple-500 focus:outline-none text-white"
                minLength={4}
                maxLength={20}
              />
            </div>
          )}

          {/* Submit Buttons */}
          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-6 py-3 bg-gray-700 hover:bg-gray-600 text-white font-semibold rounded transition"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={createRoomMutation.isPending}
              className="flex-1 px-6 py-3 bg-purple-500 hover:bg-purple-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-semibold rounded transition"
            >
              {createRoomMutation.isPending ? '생성 중...' : '방 만들기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
