import { useState } from 'react';
import { Bell, Users, Gamepad2, Star, Trash2, Check } from 'lucide-react';
import {
  useNotifications,
  useMarkNotificationAsRead,
  useMarkAllNotificationsAsRead,
  useDeleteNotification,
} from '@/hooks/queries/useNotifications';
import { ConfirmModal } from '@/components/common/ConfirmModal';

export default function NotificationsPage() {
  const { data: notifications = [], isLoading } = useNotifications();
  const markAsReadMutation = useMarkNotificationAsRead();
  const markAllAsReadMutation = useMarkAllNotificationsAsRead();
  const deleteMutation = useDeleteNotification();

  const [showMarkAllConfirm, setShowMarkAllConfirm] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const handleMarkAsRead = (id: number) => {
    markAsReadMutation.mutate(id);
  };

  const handleMarkAllAsRead = () => {
    setShowMarkAllConfirm(true);
  };

  const handleDelete = (id: number) => {
    setDeletingId(id);
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'FRIEND_REQUEST':
      case 'FRIEND_ACCEPTED':
        return <Users size={24} className="text-blue-400" />;
      case 'ROOM_INVITATION':
      case 'ROOM_JOIN':
        return <Gamepad2 size={24} className="text-purple-400" />;
      case 'EVALUATION_RECEIVED':
        return <Star size={24} className="text-yellow-400" />;
      default:
        return <Bell size={24} className="text-gray-400" />;
    }
  };

  const getNotificationBorder = (type: string) => {
    switch (type) {
      case 'FRIEND_REQUEST':
      case 'FRIEND_ACCEPTED':
        return 'border-blue-500';
      case 'ROOM_INVITATION':
      case 'ROOM_JOIN':
        return 'border-purple-500';
      case 'EVALUATION_RECEIVED':
        return 'border-yellow-500';
      default:
        return 'border-gray-700';
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return '방금 전';
    if (diffMins < 60) return `${diffMins}분 전`;
    if (diffHours < 24) return `${diffHours}시간 전`;
    if (diffDays < 7) return `${diffDays}일 전`;
    return date.toLocaleDateString('ko-KR');
  };

  const unreadCount = notifications.filter((n: any) => !n.isRead).length;

  return (
    <>
    {showMarkAllConfirm && (
      <ConfirmModal
        message="모든 알림을 읽음 처리하시겠습니까?"
        onConfirm={() => { markAllAsReadMutation.mutate(); setShowMarkAllConfirm(false); }}
        onCancel={() => setShowMarkAllConfirm(false)}
        confirmLabel="읽음 처리"
      />
    )}
    {deletingId !== null && (
      <ConfirmModal
        message="이 알림을 삭제하시겠습니까?"
        onConfirm={() => { deleteMutation.mutate(deletingId); setDeletingId(null); }}
        onCancel={() => setDeletingId(null)}
        confirmLabel="삭제"
        danger
      />
    )}
    <div className="min-h-screen bg-[#0e0e10] text-white">
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <Bell size={28} className="text-purple-400" />
            <div>
              <h1 className="text-3xl font-bold">알림</h1>
              {unreadCount > 0 && (
                <p className="text-gray-400 mt-0.5 text-sm">
                  읽지 않은 알림 <span className="text-purple-400 font-semibold">{unreadCount}</span>개
                </p>
              )}
            </div>
          </div>
          {unreadCount > 0 && (
            <button
              onClick={handleMarkAllAsRead}
              disabled={markAllAsReadMutation.isPending}
              className="flex items-center gap-2 px-4 py-2 bg-purple-500 hover:bg-purple-600 disabled:bg-gray-600 rounded font-semibold transition"
            >
              <Check size={16} />
              모두 읽음 처리
            </button>
          )}
        </div>

        {/* Notifications List */}
        <div className="space-y-3">
          {isLoading ? (
            <div className="text-center text-gray-400 py-12">불러오는 중...</div>
          ) : notifications.length === 0 ? (
            <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-12 text-center">
              <Bell size={56} className="text-gray-600 mx-auto mb-4" />
              <div className="text-gray-400 text-lg">알림이 없습니다</div>
            </div>
          ) : (
            notifications.map((notification: any) => (
              <div
                key={notification.id}
                className={`bg-[#18181b] border-2 ${
                  notification.isRead ? 'border-gray-700' : getNotificationBorder(notification.type)
                } rounded-lg p-5 ${notification.isRead ? 'opacity-60' : ''} transition`}
              >
                <div className="flex items-start gap-4">
                  <div className="flex-shrink-0 mt-0.5">
                    {getNotificationIcon(notification.type)}
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-white font-medium">{notification.message}</p>
                        <p className="text-gray-400 text-sm mt-1">{formatDate(notification.createdAt)}</p>
                      </div>

                      <div className="flex gap-2 flex-shrink-0">
                        {!notification.isRead && (
                          <button
                            onClick={() => handleMarkAsRead(notification.id)}
                            disabled={markAsReadMutation.isPending}
                            className="p-1.5 bg-purple-500 hover:bg-purple-600 disabled:bg-gray-600 rounded transition"
                            title="읽음 처리"
                          >
                            <Check size={14} />
                          </button>
                        )}
                        <button
                          onClick={() => handleDelete(notification.id)}
                          disabled={deleteMutation.isPending}
                          className="p-1.5 bg-red-500/20 hover:bg-red-500/40 disabled:bg-gray-600 text-red-400 rounded transition"
                          title="삭제"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>

                    {notification.relatedUrl && (
                      <a
                        href={notification.relatedUrl}
                        className="inline-block mt-2 text-sm text-purple-400 hover:text-purple-300 transition"
                      >
                        자세히 보기 →
                      </a>
                    )}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
    </>
  );
}
