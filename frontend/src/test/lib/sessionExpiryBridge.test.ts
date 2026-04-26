import { afterEach, describe, expect, it, vi } from 'vitest';
import { notifySessionExpired, setSessionExpiredHandler } from '@/lib/sessionExpiryBridge';

describe('sessionExpiryBridge', () => {
  afterEach(() => {
    setSessionExpiredHandler(null);
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('calls the registered handler when present', () => {
    const handler = vi.fn();

    setSessionExpiredHandler(handler);
    notifySessionExpired();

    expect(handler).toHaveBeenCalledTimes(1);
  });

  it('redirects to login when no handler is registered', () => {
    const replace = vi.fn();
    vi.stubGlobal('location', { replace });

    setSessionExpiredHandler(null);
    notifySessionExpired();

    expect(replace).toHaveBeenCalledWith('/login');
  });
});
