type SessionExpiredHandler = () => void;

let sessionExpiredHandler: SessionExpiredHandler | null = null;

export function setSessionExpiredHandler(handler: SessionExpiredHandler | null) {
  sessionExpiredHandler = handler;
}

export function notifySessionExpired() {
  if (sessionExpiredHandler) {
    sessionExpiredHandler();
    return;
  }

  if (typeof location !== 'undefined') {
    location.replace('/login');
  }
}
