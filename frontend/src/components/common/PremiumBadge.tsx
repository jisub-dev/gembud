import { Crown } from 'lucide-react';

interface PremiumBadgeProps {
  className?: string;
}

export default function PremiumBadge({ className = '' }: PremiumBadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1 px-2 py-0.5 bg-gradient-to-r from-yellow-500/20 to-amber-500/20 border border-yellow-500/40 rounded-full text-yellow-400 text-xs font-gaming ${className}`}
    >
      <Crown size={11} />
      PRO
    </span>
  );
}
