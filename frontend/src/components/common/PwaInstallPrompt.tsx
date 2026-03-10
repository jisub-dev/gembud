import { useEffect, useState } from 'react';

interface DeferredPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

export function PwaInstallPrompt() {
  const [deferredPrompt, setDeferredPrompt] = useState<DeferredPromptEvent | null>(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const handleBeforeInstallPrompt = (event: Event) => {
      event.preventDefault();
      setDeferredPrompt(event as DeferredPromptEvent);
      setIsVisible(true);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    return () => window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
  }, []);

  const handleInstallClick = async () => {
    if (!deferredPrompt) return;
    await deferredPrompt.prompt();
    await deferredPrompt.userChoice;
    setDeferredPrompt(null);
    setIsVisible(false);
  };

  if (!isVisible || !deferredPrompt) {
    return null;
  }

  return (
    <div className="fixed bottom-4 left-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 rounded-lg border border-gray-700 bg-[#15161a] px-4 py-3 shadow-xl">
      <p className="text-sm font-semibold text-white">Gembud 앱을 설치할 수 있습니다.</p>
      <p className="mt-1 text-xs text-gray-300">홈 화면에 추가해서 더 빠르게 접속하세요.</p>
      <div className="mt-3 flex justify-end gap-2">
        <button
          type="button"
          onClick={() => setIsVisible(false)}
          className="rounded border border-gray-600 px-3 py-1.5 text-xs text-gray-200 hover:bg-gray-700"
        >
          닫기
        </button>
        <button
          type="button"
          onClick={handleInstallClick}
          className="rounded bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-blue-500"
        >
          설치
        </button>
      </div>
    </div>
  );
}
