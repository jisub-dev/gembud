import { useEffect, useState } from 'react';

export function OfflineStatusBanner() {
  const [isOffline, setIsOffline] = useState(typeof navigator !== 'undefined' ? !navigator.onLine : false);

  useEffect(() => {
    const handleOffline = () => setIsOffline(true);
    const handleOnline = () => setIsOffline(false);

    window.addEventListener('offline', handleOffline);
    window.addEventListener('online', handleOnline);

    return () => {
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('online', handleOnline);
    };
  }, []);

  if (!isOffline) return null;

  return (
    <div className="fixed top-4 left-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 rounded-lg border border-amber-600/50 bg-[#2b2111] px-4 py-3 shadow-xl">
      <p className="text-sm font-semibold text-amber-100">오프라인 상태입니다.</p>
      <p className="mt-1 text-xs text-amber-200">
        일부 기능이 제한될 수 있지만, 캐시된 화면은 계속 볼 수 있습니다.
      </p>
    </div>
  );
}
