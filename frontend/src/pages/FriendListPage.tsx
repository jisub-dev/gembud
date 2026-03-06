import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { UserPlus, UserCheck, UserX, Users, Send, Trash2, Clock } from 'lucide-react';
import {
  useFriends,
  useFriendRequests,
  useSentFriendRequests,
  useSendFriendRequest,
  useAcceptFriendRequest,
  useRejectFriendRequest,
  useRemoveFriend,
} from '@/hooks/queries/useFriends';
import { useToast } from '@/hooks/useToast';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import type { Friend, FriendRequest } from '@/types/friend';
import { userService } from '@/services/userService';

const statusBadgeClass: Record<FriendRequest['status'], string> = {
  PENDING: 'bg-amber-500/20 text-amber-300 border border-amber-400/40',
  ACCEPTED: 'bg-emerald-500/20 text-emerald-300 border border-emerald-400/40',
  REJECTED: 'bg-rose-500/20 text-rose-300 border border-rose-400/40',
};

const statusLabel: Record<FriendRequest['status'], string> = {
  PENDING: '대기중',
  ACCEPTED: '수락됨',
  REJECTED: '거절됨',
};

/**
 * Friend list page with search / request / accept / reject workflows.
 */
export default function FriendListPage() {
  const [activeTab, setActiveTab] = useState<'friends' | 'received' | 'sent'>('friends');
  const [searchQuery, setSearchQuery] = useState('');
  const [removingFriend, setRemovingFriend] = useState<{ id: number; nickname: string } | null>(null);
  const [sendingUserId, setSendingUserId] = useState<number | null>(null);

  const { data: friends = [], isLoading: friendsLoading } = useFriends();
  const { data: requests = [], isLoading: requestsLoading } = useFriendRequests();
  const { data: sentRequests = [], isLoading: sentLoading } = useSentFriendRequests();

  const sendRequestMutation = useSendFriendRequest();
  const acceptMutation = useAcceptFriendRequest();
  const rejectMutation = useRejectFriendRequest();
  const removeMutation = useRemoveFriend();
  const toast = useToast();

  const { data: searchedUsers = [], isLoading: searchingUsers } = useQuery({
    queryKey: ['userSearch', searchQuery],
    queryFn: () => userService.searchUsers(searchQuery.trim()),
    enabled: searchQuery.trim().length >= 2,
    staleTime: 30_000,
  });

  const blockedUserIds = useMemo(() => {
    return new Set<number>([
      ...friends.map((friend) => friend.friendId),
      ...requests.map((request) => request.userId),
      ...sentRequests.map((request) => request.friendId),
    ]);
  }, [friends, requests, sentRequests]);

  const filteredUsers = useMemo(
    () => searchedUsers.filter((user) => !blockedUserIds.has(user.id)),
    [searchedUsers, blockedUserIds]
  );

  const handleSendRequest = (targetUserId: number, nickname: string) => {
    setSendingUserId(targetUserId);
    sendRequestMutation.mutate(targetUserId, {
      onSuccess: () => {
        toast.success(`${nickname}님에게 친구 요청을 보냈습니다`);
        setSearchQuery('');
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || '친구 요청 실패');
      },
      onSettled: () => {
        setSendingUserId(null);
      },
    });
  };

  const handleAccept = (requestId: number) => {
    acceptMutation.mutate(requestId, {
      onSuccess: () => toast.success('친구 요청을 수락했습니다'),
      onError: (error: any) => toast.error(error.response?.data?.message || '수락 실패'),
    });
  };

  const handleReject = (requestId: number) => {
    rejectMutation.mutate(requestId, {
      onSuccess: () => toast.info('친구 요청을 거절했습니다'),
      onError: (error: any) => toast.error(error.response?.data?.message || '거절 실패'),
    });
  };

  const confirmRemove = () => {
    if (!removingFriend) return;
    removeMutation.mutate(removingFriend.id, {
      onSuccess: () => {
        toast.info('친구를 삭제했습니다');
        setRemovingFriend(null);
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || '삭제 실패');
        setRemovingFriend(null);
      },
    });
  };

  return (
    <>
      {removingFriend && (
        <ConfirmModal
          message={`${removingFriend.nickname}님을 친구 목록에서 삭제하시겠습니까?`}
          onConfirm={confirmRemove}
          onCancel={() => setRemovingFriend(null)}
          confirmLabel="삭제"
          danger
        />
      )}

      <div className="min-h-screen bg-[#0e0e10] text-white">
        <div className="container mx-auto max-w-4xl px-4 py-8">
          <h1 className="mb-6 flex items-center gap-3 text-3xl font-bold">
            <Users size={28} className="text-purple-400" />
            친구
          </h1>

          <section className="mb-6 rounded-lg border-2 border-gray-700 bg-[#18181b] p-6">
            <h2 className="mb-4 flex items-center gap-2 text-xl font-semibold">
              <UserPlus size={20} className="text-purple-400" />
              친구 검색
            </h2>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="닉네임/이메일로 사용자 검색 (2자 이상)"
              className="w-full rounded border border-gray-600 bg-[#0e0e10] px-4 py-2 focus:border-purple-500 focus:outline-none"
            />
            {searchQuery.trim().length >= 2 && (
              <div className="mt-3 max-h-56 space-y-2 overflow-y-auto">
                {searchingUsers ? (
                  <p className="text-sm text-gray-400">검색 중...</p>
                ) : filteredUsers.length === 0 ? (
                  <p className="text-sm text-gray-400">요청 가능한 사용자가 없습니다</p>
                ) : (
                  filteredUsers.map((user) => {
                    const isSending = sendingUserId === user.id && sendRequestMutation.isPending;
                    return (
                      <div
                        key={user.id}
                        className="flex items-center justify-between rounded border border-gray-700 bg-[#0e0e10] px-3 py-2"
                      >
                        <div>
                          <p className="text-sm font-semibold">{user.nickname}</p>
                          <p className="text-xs text-gray-400">{user.email}</p>
                        </div>
                        <button
                          type="button"
                          onClick={() => handleSendRequest(user.id, user.nickname)}
                          disabled={sendRequestMutation.isPending}
                          className="flex items-center gap-1.5 rounded bg-purple-500 px-3 py-2 text-sm font-semibold transition hover:bg-purple-600 disabled:bg-gray-600"
                        >
                          <Send size={14} />
                          {isSending ? '전송 중...' : '요청 보내기'}
                        </button>
                      </div>
                    );
                  })
                )}
              </div>
            )}
          </section>

          <div className="mb-6 flex gap-2">
            <button
              onClick={() => setActiveTab('friends')}
              className={`flex items-center gap-2 rounded px-6 py-2 font-semibold transition ${
                activeTab === 'friends' ? 'bg-purple-500 text-white' : 'bg-[#18181b] text-gray-400 hover:text-white'
              }`}
            >
              <Users size={15} />
              친구 목록 ({friends.length})
            </button>
            <button
              onClick={() => setActiveTab('received')}
              className={`flex items-center gap-2 rounded px-6 py-2 font-semibold transition ${
                activeTab === 'received' ? 'bg-purple-500 text-white' : 'bg-[#18181b] text-gray-400 hover:text-white'
              }`}
            >
              <UserPlus size={15} />
              받은 요청 ({requests.length})
            </button>
            <button
              onClick={() => setActiveTab('sent')}
              className={`flex items-center gap-2 rounded px-6 py-2 font-semibold transition ${
                activeTab === 'sent' ? 'bg-purple-500 text-white' : 'bg-[#18181b] text-gray-400 hover:text-white'
              }`}
            >
              <Clock size={15} />
              보낸 요청 ({sentRequests.length})
            </button>
          </div>

          <div className="rounded-lg border-2 border-gray-700 bg-[#18181b] p-6">
            {activeTab === 'friends' && (
              <div>
                {friendsLoading ? (
                  <div className="py-8 text-center text-gray-400">Loading...</div>
                ) : friends.length === 0 ? (
                  <div className="flex flex-col items-center gap-3 py-8 text-gray-400">
                    <Users size={40} className="text-gray-600" />
                    아직 친구가 없습니다. 친구를 추가해보세요!
                  </div>
                ) : (
                  <div className="space-y-3">
                    {friends.map((friend: Friend) => (
                      <div
                        key={friend.id}
                        className="flex items-center justify-between rounded bg-[#0e0e10] p-4 transition hover:bg-[#1a1a1f]"
                      >
                        <div>
                          <div className="font-semibold">{friend.friendNickname}</div>
                          <span
                            className={`mt-1 inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${statusBadgeClass[friend.status]}`}
                          >
                            {statusLabel[friend.status]}
                          </span>
                        </div>
                        <button
                          onClick={() => setRemovingFriend({ id: friend.friendId, nickname: friend.friendNickname })}
                          className="flex items-center gap-1.5 rounded bg-red-500 px-3 py-2 text-sm font-semibold transition hover:bg-red-600"
                        >
                          <Trash2 size={14} />
                          삭제
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'received' && (
              <div>
                {requestsLoading ? (
                  <div className="py-8 text-center text-gray-400">Loading...</div>
                ) : requests.length === 0 ? (
                  <div className="flex flex-col items-center gap-3 py-8 text-gray-400">
                    <UserPlus size={40} className="text-gray-600" />
                    받은 친구 요청이 없습니다
                  </div>
                ) : (
                  <div className="space-y-3">
                    {requests.map((request: FriendRequest) => (
                      <div key={request.id} className="flex items-center justify-between rounded bg-[#0e0e10] p-4">
                        <div>
                          <div className="font-semibold">{request.userNickname}</div>
                          <span
                            className={`mt-1 inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${statusBadgeClass[request.status]}`}
                          >
                            {statusLabel[request.status]}
                          </span>
                        </div>
                        {request.status === 'PENDING' ? (
                          <div className="flex gap-2">
                            <button
                              onClick={() => handleAccept(request.id)}
                              disabled={acceptMutation.isPending}
                              className="flex items-center gap-1.5 rounded bg-green-500 px-3 py-2 text-sm font-semibold transition hover:bg-green-600 disabled:bg-gray-600"
                            >
                              <UserCheck size={14} />
                              수락
                            </button>
                            <button
                              onClick={() => handleReject(request.id)}
                              disabled={rejectMutation.isPending}
                              className="flex items-center gap-1.5 rounded bg-red-500 px-3 py-2 text-sm font-semibold transition hover:bg-red-600 disabled:bg-gray-600"
                            >
                              <UserX size={14} />
                              거절
                            </button>
                          </div>
                        ) : null}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'sent' && (
              <div>
                {sentLoading ? (
                  <div className="py-8 text-center text-gray-400">Loading...</div>
                ) : sentRequests.length === 0 ? (
                  <div className="flex flex-col items-center gap-3 py-8 text-gray-400">
                    <Clock size={40} className="text-gray-600" />
                    보낸 친구 요청이 없습니다
                  </div>
                ) : (
                  <div className="space-y-3">
                    {sentRequests.map((request: FriendRequest) => (
                      <div key={request.id} className="flex items-center justify-between rounded bg-[#0e0e10] p-4">
                        <div>
                          <div className="font-semibold">{request.friendNickname}</div>
                          <span
                            className={`mt-1 inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${statusBadgeClass[request.status]}`}
                          >
                            {statusLabel[request.status]}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
