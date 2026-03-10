import { useEffect, useState } from 'react';

const PWA_UPDATE_READY_EVENT = 'pwa:update-ready';
const PWA_APPLY_UPDATE_EVENT = 'pwa:apply-update';

export function PwaUpdateBanner() {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const showBanner = () => setIsVisible(true);
    window.addEventListener(PWA_UPDATE_READY_EVENT, showBanner);
    return () => window.removeEventListener(PWA_UPDATE_READY_EVENT, showBanner);
  }, []);

  const handleApplyUpdate = () => {
    window.dispatchEvent(new Event(PWA_APPLY_UPDATE_EVENT));
  };

  if (!isVisible) return null;

  return (
    <div className="fixed top-4 left-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 rounded-lg border border-cyan-600/50 bg-[#102027] px-4 py-3 shadow-xl">
      <p className="text-sm font-semibold text-white">새 버전이 준비되었습니다.</p>
      <p className="mt-1 text-xs text-cyan-100">업데이트를 적용하면 최신 기능으로 새로고침됩니다.</p>
      <div className="mt-3 flex justify-end gap-2">
        <button
          type="button"
          onClick={() => setIsVisible(false)}
          className="rounded border border-cyan-700/60 px-3 py-1.5 text-xs text-cyan-100 hover:bg-cyan-900/40"
        >
          나중에
        </button>
        <button
          type="button"
          onClick={handleApplyUpdate}
          className="rounded bg-cyan-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-cyan-500"
        >
          지금 업데이트
        </button>
      </div>
    </div>
  );
}
