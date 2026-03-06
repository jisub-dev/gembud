import { afterEach, describe, expect, it, vi } from 'vitest';

describe('feature flags', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  it('defaults premium feature to false when env is missing', async () => {
    vi.unstubAllEnvs();
    const mod = await import('@/config/features');

    expect(mod.featureFlags.premium).toBe(false);
    expect(mod.isPremiumActive(true)).toBe(false);
  });

  it('enables premium feature when env is true', async () => {
    vi.stubEnv('VITE_FEATURE_PREMIUM_ENABLED', 'true');
    const mod = await import('@/config/features');

    expect(mod.featureFlags.premium).toBe(true);
    expect(mod.isPremiumActive(true)).toBe(true);
    expect(mod.isPremiumActive(false)).toBe(false);
  });
});
