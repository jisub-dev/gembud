const toBoolean = (value: string | undefined, defaultValue: boolean): boolean => {
  if (value == null) {
    return defaultValue;
  }
  return value.toLowerCase() === 'true';
};

export const featureFlags = {
  premium: toBoolean(import.meta.env.VITE_FEATURE_PREMIUM_ENABLED, false),
} as const;

export const isPremiumActive = (isPremium?: boolean | null): boolean =>
  featureFlags.premium && Boolean(isPremium);
