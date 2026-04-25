/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_FEATURE_PREMIUM_ENABLED?: string;
  readonly VITE_ADSENSE_CLIENT_ID?: string;
  readonly VITE_ADSENSE_SLOT_LEADERBOARD?: string;
  readonly VITE_ADSENSE_SLOT_RECTANGLE?: string;
  readonly VITE_ADSENSE_SLOT_INLINE?: string;
  readonly VITE_SENTRY_DSN?: string;
  readonly VITE_SENTRY_ENVIRONMENT?: string;
  readonly VITE_SENTRY_RELEASE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module 'sockjs-client' {
  class SockJS {
    constructor(url: string, _reserved?: unknown, options?: { transports?: string[] });
    close(): void;
    send(data: string): void;
  }
  export = SockJS;
}
