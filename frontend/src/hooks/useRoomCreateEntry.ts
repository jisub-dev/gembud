import { useEffect, useRef, useState } from 'react';

interface UseRoomCreateEntryParams {
  searchParams: URLSearchParams;
}

export function useRoomCreateEntry({ searchParams }: UseRoomCreateEntryParams) {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const createHandledRef = useRef(false);

  useEffect(() => {
    const shouldOpenCreateModal = searchParams.get('create') === 'true';

    if (shouldOpenCreateModal && !createHandledRef.current) {
      setShowCreateModal(true);
      createHandledRef.current = true;
    }

    if (!shouldOpenCreateModal) {
      createHandledRef.current = false;
    }
  }, [searchParams]);

  return {
    closeCreateModal: () => setShowCreateModal(false),
    openCreateModal: () => setShowCreateModal(true),
    showCreateModal,
  };
}
