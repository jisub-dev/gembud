import { useEffect, useMemo, useRef } from 'react';
import type { SetURLSearchParams } from 'react-router-dom';
import type { Room } from '@/types/room';

const RECOMMENDATION_EXCLUSION_KEY = 'roomRecommendations:excluded';
const RECOMMENDATION_ACTIVE_KEY = 'roomRecommendations:active';

type RecommendationSource = 'manual' | 'auto';

interface RecommendedRoomItem {
  room?: Room | null;
}

interface UseRoomRecommendationsParams {
  gameId: number;
  rooms?: Room[];
  roomsLoading: boolean;
  recommendedRooms: RecommendedRoomItem[];
  myJoinedRoomPublicIds: Set<string>;
  searchParams: URLSearchParams;
  setSearchParams: SetURLSearchParams;
  onJoinRoom: (room: Room) => Promise<void>;
  onNoRoom: (source: RecommendationSource) => void;
}

export function useRoomRecommendations({
  gameId,
  rooms,
  roomsLoading,
  recommendedRooms,
  myJoinedRoomPublicIds,
  searchParams,
  setSearchParams,
  onJoinRoom,
  onNoRoom,
}: UseRoomRecommendationsParams) {
  const recommendationHandledRef = useRef<string | null>(null);
  const onJoinRoomRef = useRef(onJoinRoom);
  const onNoRoomRef = useRef(onNoRoom);

  useEffect(() => {
    onJoinRoomRef.current = onJoinRoom;
  }, [onJoinRoom]);

  useEffect(() => {
    onNoRoomRef.current = onNoRoom;
  }, [onNoRoom]);

  const excludedRecommendedRoomIds = useMemo(
    () => getExcludedRecommendedRooms(gameId),
    [gameId, searchParams],
  );

  const getNextRecommendedRoom = (extraExcludedRoomIds?: Set<string>) => {
    const excludedRoomIds = extraExcludedRoomIds ?? excludedRecommendedRoomIds;
    for (const recommendation of recommendedRooms) {
      const room = recommendation.room;
      if (!room) continue;
      if (room.status !== 'OPEN') continue;
      if (room.isPrivate) continue;
      if (myJoinedRoomPublicIds.has(room.publicId)) continue;
      if (excludedRoomIds.has(room.publicId)) continue;
      return room;
    }
    return null;
  };

  const handleRecommendedJoin = async (
    source: RecommendationSource,
    extraExcludedRoomIds?: Set<string>,
  ) => {
    const nextRecommendedRoom = getNextRecommendedRoom(extraExcludedRoomIds);
    if (!nextRecommendedRoom) {
      onNoRoomRef.current(source);
      return;
    }

    await onJoinRoomRef.current(nextRecommendedRoom);
  };

  useEffect(() => {
    const shouldAutoRecommend = searchParams.get('recommend') === 'true';
    const excludedRoomPublicId = searchParams.get('exclude')?.trim();
    if (!shouldAutoRecommend || roomsLoading) {
      return;
    }

    const recommendationKey = `${searchParams.toString()}::${rooms?.length ?? 0}`;
    if (recommendationHandledRef.current === recommendationKey) {
      return;
    }
    recommendationHandledRef.current = recommendationKey;

    const nextExcludedRoomIds = new Set(excludedRecommendedRoomIds);
    if (excludedRoomPublicId) {
      addExcludedRecommendedRoom(gameId, excludedRoomPublicId);
      nextExcludedRoomIds.add(excludedRoomPublicId);
    }

    void handleRecommendedJoin('auto', nextExcludedRoomIds).finally(() => {
      const nextParams = new URLSearchParams(searchParams);
      nextParams.delete('recommend');
      nextParams.delete('exclude');
      setSearchParams(nextParams, { replace: true });
    });
  }, [excludedRecommendedRoomIds, gameId, rooms, roomsLoading, searchParams, setSearchParams]);

  return {
    handleRecommendedJoin,
  };
}

function getExcludedRecommendedRooms(gameId: number) {
  if (!gameId) return new Set<string>();
  try {
    const raw = localStorage.getItem(RECOMMENDATION_EXCLUSION_KEY);
    if (!raw) return new Set<string>();
    const parsed = JSON.parse(raw) as Record<string, string[]>;
    return new Set(parsed[String(gameId)] ?? []);
  } catch {
    return new Set<string>();
  }
}

export function addExcludedRecommendedRoom(gameId: number, roomPublicId: string) {
  if (!gameId || !roomPublicId) return;
  try {
    const raw = localStorage.getItem(RECOMMENDATION_EXCLUSION_KEY);
    const parsed = raw ? (JSON.parse(raw) as Record<string, string[]>) : {};
    const gameKey = String(gameId);
    const nextValues = new Set(parsed[gameKey] ?? []);
    nextValues.add(roomPublicId);
    parsed[gameKey] = [...nextValues];
    localStorage.setItem(RECOMMENDATION_EXCLUSION_KEY, JSON.stringify(parsed));
  } catch {
    // Ignore localStorage failures for non-critical recommendation history.
  }
}

export function markRecommendedRoomActive(roomPublicId: string, gameId: number) {
  if (!roomPublicId || !gameId) return;
  try {
    const raw = localStorage.getItem(RECOMMENDATION_ACTIVE_KEY);
    const parsed = raw ? (JSON.parse(raw) as Record<string, { gameId: number }>) : {};
    parsed[roomPublicId] = { gameId };
    localStorage.setItem(RECOMMENDATION_ACTIVE_KEY, JSON.stringify(parsed));
  } catch {
    // Ignore localStorage failures for non-critical recommendation tracking.
  }
}

export function consumeRecommendedRoomActive(roomPublicId: string, gameId: number) {
  if (!roomPublicId || !gameId) return false;
  try {
    const raw = localStorage.getItem(RECOMMENDATION_ACTIVE_KEY);
    if (!raw) return false;
    const parsed = JSON.parse(raw) as Record<string, { gameId: number }>;
    const active = parsed[roomPublicId];
    if (!active || active.gameId !== gameId) {
      return false;
    }
    delete parsed[roomPublicId];
    localStorage.setItem(RECOMMENDATION_ACTIVE_KEY, JSON.stringify(parsed));
    return true;
  } catch {
    return false;
  }
}
