import { useState } from 'react';

interface PasswordModalProps {
  onConfirm: (password: string) => void;
  onCancel: () => void;
}

export function PasswordModal({ onConfirm, onCancel }: PasswordModalProps) {
  const [password, setPassword] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!password.trim()) return;
    onConfirm(password);
  };

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-[60]">
      <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 max-w-sm w-full mx-4">
        <h3 className="text-white font-bold text-lg mb-4">비밀번호 입력</h3>
        <form onSubmit={handleSubmit}>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호를 입력하세요"
            autoFocus
            className="w-full px-4 py-3 bg-[#0e0e10] border border-gray-600 rounded focus:border-purple-500 focus:outline-none text-white mb-4"
          />
          <div className="flex gap-3">
            <button
              type="button"
              onClick={onCancel}
              className="flex-1 px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white font-semibold rounded transition"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={!password.trim()}
              className="flex-1 px-4 py-2 bg-purple-500 hover:bg-purple-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-semibold rounded transition"
            >
              입장
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
