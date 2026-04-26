import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import * as Sentry from '@sentry/react'
import App from './App.tsx'
import './index.css'

// Initialize Sentry only when DSN is configured (disabled by default)
if (import.meta.env.VITE_SENTRY_DSN) {
  Sentry.init({
    dsn: import.meta.env.VITE_SENTRY_DSN,
    environment: import.meta.env.VITE_SENTRY_ENVIRONMENT || 'production',
    release: import.meta.env.VITE_SENTRY_RELEASE,
    tracesSampleRate: 0.1,
    integrations: [
      Sentry.browserTracingIntegration(),
    ],
    // Don't send PII (GDPR-friendly)
    sendDefaultPii: false,
  });
}

const PWA_UPDATE_READY_EVENT = 'pwa:update-ready';
const PWA_APPLY_UPDATE_EVENT = 'pwa:apply-update';
const DEV_SW_RESET_KEY = 'gembud:dev-sw-reset';

// Dynamically inject AdSense script if Publisher ID is configured
const adsenseId = import.meta.env.VITE_ADSENSE_CLIENT_ID;
if (adsenseId) {
  const script = document.createElement('script');
  script.async = true;
  script.src = `https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${adsenseId}`;
  script.crossOrigin = 'anonymous';
  document.head.appendChild(script);
}

// Create a client for TanStack Query
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      retry: 1,
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </React.StrictMode>
)

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    if (import.meta.env.DEV) {
      void resetServiceWorkerInDevelopment();
      return;
    }

    let isRefreshing = false;

    navigator.serviceWorker.register('/sw.js').then((registration) => {
      const dispatchUpdateReady = () => {
        window.dispatchEvent(new Event(PWA_UPDATE_READY_EVENT));
      };

      if (registration.waiting) {
        dispatchUpdateReady();
      }

      registration.addEventListener('updatefound', () => {
        const newWorker = registration.installing;
        if (!newWorker) return;
        newWorker.addEventListener('statechange', () => {
          if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
            dispatchUpdateReady();
          }
        });
      });

      const applyUpdate = () => {
        if (registration.waiting) {
          registration.waiting.postMessage({ type: 'SKIP_WAITING' });
        }
      };

      window.addEventListener(PWA_APPLY_UPDATE_EVENT, applyUpdate);
    }).catch(() => {
      // Keep startup resilient if service worker registration fails.
    });

    navigator.serviceWorker.addEventListener('controllerchange', () => {
      if (isRefreshing) return;
      isRefreshing = true;
      window.location.reload();
    });
  });
}

async function resetServiceWorkerInDevelopment() {
  try {
    const registrations = await navigator.serviceWorker.getRegistrations();
    const hasExistingRegistration =
      registrations.length > 0 || Boolean(navigator.serviceWorker.controller);

    await Promise.all(registrations.map((registration) => registration.unregister()));

    if ('caches' in window) {
      const cacheKeys = await caches.keys();
      await Promise.all(
        cacheKeys
          .filter((cacheKey) => cacheKey.startsWith('gembud-'))
          .map((cacheKey) => caches.delete(cacheKey))
      );
    }

    if (
      hasExistingRegistration &&
      !window.sessionStorage.getItem(DEV_SW_RESET_KEY)
    ) {
      window.sessionStorage.setItem(DEV_SW_RESET_KEY, '1');
      window.location.reload();
    }
  } catch {
    // Keep development startup resilient if service worker cleanup fails.
  }
}
