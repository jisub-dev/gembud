import { useState } from 'react';
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

/**
 * Friend list page with friend requests
 *
 * @author Gembud Team
 * @since 2026-02-26
 */
export default function FriendListPage() {
  const [activeTab, setActiveTab] = useState<'friends' | 'requests' | 'sent'>('friends');
  const [newFriendEmail, setNewFriendEmail] = useState('');
  const [removingFriend, setRemovingFriend] = useState<{ id: number; nickname: string } | null>(null);

  const { data: friends = [], isLoading: friendsLoading } = useFriends();
  const { data: requests = [], isLoading: requestsLoading } = useFriendRequests();
  const { data: sentRequests = [], isLoading: sentLoading } = useSentFriendRequests();

  const sendRequestMutation = useSendFriendRequest();
  const acceptMutation = useAcceptFriendRequest();
  const rejectMutation = useRejectFriendRequest();
  const removeMutation = useRemoveFriend();
  const toast = useToast();

  const handleSendRequest = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFriendEmail.trim()) return;

    sendRequestMutation.mutate(newFriendEmail, {
      onSuccess: () => {
        toast.success('친구 요청을 보냈습니다');
        setNewFriendEmail('');
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || '친구 요청 실패');
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

  const handleRemove = (friendId: number, nickname: string) => {
    setRemovingFriend({ id: friendId, nickname });
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
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        <h1 className="flex items-center gap-3 text-3xl font-bold mb-6">
          <Users size={28} className="text-purple-400" />
          친구
        </h1>

        {/* Add Friend Form */}
        <form onSubmit={handleSendRequest} className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 mb-6">
          <h2 className="flex items-center gap-2 text-xl font-semibold mb-4">
            <UserPlus size={20} className="text-purple-400" />
            친구 추가
          </h2>
          <div className="flex gap-3">
            <input
              type="email"
              value={newFriendEmail}
              onChange={(e) => setNewFriendEmail(e.target.value)}
              placeholder="친구 이메일 입력"
              className="flex-1 px-4 py-2 bg-[#0e0e10] border border-gray-600 rounded focus:border-purple-500 focus:outline-none"
            />
            <button
              type="submit"
              disabled={sendRequestMutation.isPending}
              className="flex items-center gap-2 px-6 py-2 bg-purple-500 hover:bg-purple-600 disabled:bg-gray-600 rounded font-semibold transition"
            >
              <Send size={15} />
              {sendRequestMutation.isPending ? '전송 중...' : '요청 보내기'}
            </button>
          </div>
        </form>

        {/* Tabs */}
        <div className="flex gap-2 mb-6">
          <button
            onClick={() => setActiveTab('friends')}
            className={`flex items-center gap-2 px-6 py-2 rounded font-semibold transition ${
              activeTab === 'friends' ? 'bg-purple-500 text-white' : 'bg-[#18181b] text-gray-400 hover:text-white'
            }`}
          >
            <Users size={15} />
            친구 목록 ({friends.length})
          </button>
          <button
            onClick={() => setActiveTab('requests')}
            className={`flex items-center gap-2 px-6 py-2 rounded font-semibold transition ${
              activeTab === 'requests' ? 'bg-purple-500 text-white' : 'bg-[#18181b] text-gray-400 hover:text-white'
            }`}
          >
            <UserPlus size={15} />
            받은 요청 ({requests.length})
          </button>
          <button
            onClick={() => setActiveTab('sent')}
            className={`flex items-center gap-2 px-6 py-2 rounded font-semibold transition ${
              activeTab === 'sent' ? 'bg-purple-500 text-white' : 'bg-[#18181b] text-gray-400 hover:text-white'
            }`}
          >
            <Clock size={15} />
            보낸 요청 ({sentRequests.length})
          </button>
        </div>

        {/* Content */}
        <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6">
          {activeTab === 'friends' && (
            <div>
              {friendsLoading ? (
                <div className="text-center text-gray-400 py-8">Loading...</div>
              ) : friends.length === 0 ? (
                <div className="flex flex-col items-center py-8 text-gray-400 gap-3">
                  <Users size={40} className="text-gray-600" />
                  아직 친구가 없습니다. 친구를 추가해보세요!
                </div>
              ) : (
                <div className="space-y-3">
                  {friends.map((friend: Friend) => (
                    <div key={friend.id} className="flex items-center justify-between p-4 bg-[#0e0e10] rounded hover:bg-[#1a1a1f] transition">
                      <div>
                        <div className="font-semibold">{friend.friendNickname}</div>
                      </div>
                      <button
                        onClick={() => handleRemove(friend.friendId, friend.friendNickname)}
                        className="flex items-center gap-1.5 px-3 py-2 bg-red-500 hover:bg-red-600 rounded text-sm font-semibold transition"
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

          {activeTab === 'requests' && (
            <div>
              {requestsLoading ? (
                <div className="text-center text-gray-400 py-8">Loading...</div>
              ) : requests.length === 0 ? (
                <div className="flex flex-col items-center py-8 text-gray-400 gap-3">
                  <UserPlus size={40} className="text-gray-600" />
                  받은 친구 요청이 없습니다
                </div>
              ) : (
                <div className="space-y-3">
                  {requests.map((request: FriendRequest) => (
                    <div key={request.id} className="flex items-center justify-between p-4 bg-[#0e0e10] rounded">
                      <div>
                        <div className="font-semibold">{request.userNickname}</div>
                      </div>
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleAccept(request.id)}
                          disabled={acceptMutation.isPending}
                          className="flex items-center gap-1.5 px-3 py-2 bg-green-500 hover:bg-green-600 disabled:bg-gray-600 rounded text-sm font-semibold transition"
                        >
                          <UserCheck size={14} />
                          수락
                        </button>
                        <button
                          onClick={() => handleReject(request.id)}
                          disabled={rejectMutation.isPending}
                          className="flex items-center gap-1.5 px-3 py-2 bg-red-500 hover:bg-red-600 disabled:bg-gray-600 rounded text-sm font-semibold transition"
                        >
                          <UserX size={14} />
                          거절
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'sent' && (
            <div>
              {sentLoading ? (
                <div className="text-center text-gray-400 py-8">Loading...</div>
              ) : sentRequests.length === 0 ? (
                <div className="flex flex-col items-center py-8 text-gray-400 gap-3">
                  <Clock size={40} className="text-gray-600" />
                  보낸 친구 요청이 없습니다
                </div>
              ) : (
                <div className="space-y-3">
                  {sentRequests.map((request: FriendRequest) => (
                    <div key={request.id} className="flex items-center justify-between p-4 bg-[#0e0e10] rounded">
                      <div>
                        <div className="font-semibold">{request.friendNickname}</div>
                        <div className="flex items-center gap-1 text-xs text-purple-400 mt-1">
                          <Clock size={11} />
                          대기 중
                        </div>
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
