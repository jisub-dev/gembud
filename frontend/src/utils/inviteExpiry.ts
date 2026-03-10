export interface InviteExpiryInfo {
  expiresAtLabel: string;
  remainingLabel: string;
  isExpired: boolean;
  isExpiringSoon: boolean;
}

export function getInviteExpiryInfo(iso?: string, nowMs = Date.now()): InviteExpiryInfo {
  if (!iso) {
    return {
      expiresAtLabel: '미설정',
      remainingLabel: '미설정',
      isExpired: false,
      isExpiringSoon: false,
    };
  }

  const expiresAt = new Date(iso);
  if (Number.isNaN(expiresAt.getTime())) {
    return {
      expiresAtLabel: '미설정',
      remainingLabel: '미설정',
      isExpired: false,
      isExpiringSoon: false,
    };
  }

  const remainingMs = expiresAt.getTime() - nowMs;
  const isExpired = remainingMs <= 0;
  const isExpiringSoon = remainingMs > 0 && remainingMs <= 10 * 60 * 1000;

  return {
    expiresAtLabel: formatDateTime(iso),
    remainingLabel: isExpired ? '만료됨' : formatRemainingDuration(remainingMs),
    isExpired,
    isExpiringSoon,
  };
}

function formatDateTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '미설정';
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatRemainingDuration(remainingMs: number): string {
  const totalMinutes = Math.max(1, Math.ceil(remainingMs / (60 * 1000)));
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  if (hours > 0) {
    return `${hours}시간 ${minutes}분`;
  }
  return `${minutes}분`;
}
