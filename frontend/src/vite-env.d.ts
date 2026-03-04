/// <reference types="vite/client" />

declare module 'sockjs-client' {
  class SockJS {
    constructor(url: string, _reserved?: unknown, options?: { transports?: string[] });
    close(): void;
    send(data: string): void;
  }
  export = SockJS;
}
