import { useState } from 'react';
import { userService } from '@/services/userService';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';


interface EditProfileModalProps {
  currentNickname: string;
  onClose: () => void;
}

export function EditProfileModal({ currentNickname, onClose }: EditProfileModalProps) {
  const [nickname, setNickname] = useState(currentNickname);
  const [isLoading, setIsLoading] = useState(false);
  const toast = useToast();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = nickname.trim();
    if (!trimmed) {
      toast.error('닉네임을 입력해주세요');
      return;
    }
    if (trimmed.length < 2 || trimmed.length > 20) {
      toast.error('닉네임은 2~20자 사이여야 합니다');
      return;
    }

    setIsLoading(true);
    try {
      const updated = await userService.updateProfile({ nickname: trimmed });
      // Sync auth store with updated nickname
      useAuthStore.setState((state) => ({
        user: state.user ? { ...state.user, nickname: updated.nickname } : null,
      }));
      toast.success('프로필이 수정되었습니다');
      onClose();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '프로필 수정에 실패했습니다');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg max-w-md w-full">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-700">
          <h2 className="text-xl font-bold text-white">프로필 수정</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white text-2xl font-bold w-8 h-8 flex items-center justify-center"
          >
            ×
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-semibold text-gray-300 mb-2">
              닉네임 <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="2~20자"
              maxLength={20}
              className="w-full px-4 py-3 bg-[#0e0e10] border border-gray-600 rounded focus:border-purple-500 focus:outline-none text-white"
            />
          </div>

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white font-semibold rounded transition"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="flex-1 px-4 py-2 bg-purple-500 hover:bg-purple-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-semibold rounded transition"
            >
              {isLoading ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
