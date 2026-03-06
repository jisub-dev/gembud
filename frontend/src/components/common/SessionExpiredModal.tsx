interface SessionExpiredModalProps {
  isOpen: boolean;
  onConfirm: () => void;
}

export function SessionExpiredModal({ isOpen, onConfirm }: SessionExpiredModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl p-6 mx-4 max-w-sm w-full">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
          세션 만료
        </h2>
        <p className="text-sm text-gray-600 dark:text-gray-300 mb-6">
          다른 기기에서 로그인되어 세션이 종료되었습니다.
        </p>
        <button
          onClick={onConfirm}
          className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-md transition-colors"
        >
          로그인 페이지로 이동
        </button>
      </div>
    </div>
  );
}
