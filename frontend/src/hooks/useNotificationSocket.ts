import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import { notificationKeys } from '@/hooks/queries/useNotifications';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const WS_URL = API_BASE_URL + '/ws';

export function useNotificationSocket() {
  const queryClient = useQueryClient();
  const user = useAuthStore((state) => state.user);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const stompClientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!isAuthenticated || !user) {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
        stompClientRef.current = null;
      }
      return;
    }

    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
      stompClientRef.current = null;
    }

    let active = true;
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL, null, { transports: ['websocket', 'xhr-streaming', 'xhr-polling'] }),
      reconnectDelay: 5000,
      onConnect: () => {
        if (!active) return;
        client.subscribe('/user/queue/notifications', () => {
          if (!active) return;
          queryClient.invalidateQueries({ queryKey: notificationKeys.list() });
          queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount() });
        });
      },
    });

    stompClientRef.current = client;
    client.activate();

    return () => {
      active = false;
      stompClientRef.current = null;
      client.deactivate();
    };
  }, [isAuthenticated, user, queryClient]);
}

