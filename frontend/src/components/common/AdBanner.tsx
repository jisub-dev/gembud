import { useEffect, useRef } from 'react';
import type { AdData } from '@/services/adService';
import { adService } from '@/services/adService';

type BannerType = 'leaderboard' | 'rectangle' | 'inline';

interface AdBannerProps {
  type: BannerType;
  adData?: AdData | null;
  className?: string;
}

const SIZES: Record<BannerType, { width: number; height: number; label: string }> = {
  leaderboard: { width: 728, height: 90, label: '728×90' },
  rectangle: { width: 300, height: 250, label: '300×250' },
  inline: { width: 320, height: 100, label: '320×100' },
};

/**
 * AdBanner component.
 * If adData is provided, renders a self-hosted ad.
 * Otherwise renders an AdSense placeholder (or nothing if AdSense not configured).
 * Policy compliance: labelled as "광고", spaced from content.
 */
export default function AdBanner({ type, adData, className = '' }: AdBannerProps) {
  const viewRecorded = useRef(false);
  const size = SIZES[type];

  // Record view once per mount for self-hosted ads
  useEffect(() => {
    if (adData && !viewRecorded.current) {
      viewRecorded.current = true;
      adService.recordView(adData.id).catch(() => {/* ignore */});
    }
  }, [adData]);

  if (!adData) {
    // AdSense fallback slot — rendered inert if script not loaded
    return (
      <div className={`flex flex-col items-center ${className}`} style={{ minHeight: size.height }}>
        <span className="text-text-muted text-xs mb-1 self-start">광고</span>
        <ins
          className="adsbygoogle block"
          style={{ display: 'block', width: size.width, height: size.height }}
          data-ad-client="ca-pub-XXXXXXXXXXXXXXXX"
          data-ad-slot="0000000000"
          data-ad-format="fixed"
        />
      </div>
    );
  }

  const content = (
    <div
      className="relative bg-dark-secondary border border-neon-purple/10 rounded overflow-hidden"
      style={{ width: '100%', maxWidth: size.width, height: size.height }}
    >
      {adData.imageUrl ? (
        <img
          src={adData.imageUrl}
          alt={adData.title}
          className="w-full h-full object-cover"
        />
      ) : (
        <div className="flex items-center gap-4 h-full px-4">
          <div className="flex-1 min-w-0">
            <p className="text-text-primary font-medium text-sm truncate">{adData.title}</p>
            {adData.description && (
              <p className="text-text-muted text-xs truncate">{adData.description}</p>
            )}
          </div>
        </div>
      )}
    </div>
  );

  return (
    <div className={`flex flex-col items-center ${className}`}>
      <span className="text-text-muted text-xs mb-1 self-start">광고</span>
      {adData.targetUrl ? (
        <a
          href={adData.targetUrl}
          target="_blank"
          rel="noopener noreferrer sponsored"
          className="block w-full"
          style={{ maxWidth: size.width }}
        >
          {content}
        </a>
      ) : (
        content
      )}
    </div>
  );
}
