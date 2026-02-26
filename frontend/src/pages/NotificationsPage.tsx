import {
  useNotifications,
  useMarkNotificationAsRead,
  useMarkAllNotificationsAsRead,
  useDeleteNotification,
} from '@/hooks/queries/useNotifications';

/**
 * Notifications page
 *
 * @author Gembud Team
 * @since 2026-02-26
 */
export default function NotificationsPage() {
  const { data: notifications = [], isLoading } = useNotifications();
  const markAsReadMutation = useMarkNotificationAsRead();
  const markAllAsReadMutation = useMarkAllNotificationsAsRead();
  const deleteMutation = useDeleteNotification();

  const handleMarkAsRead = (id: number) => {
    markAsReadMutation.mutate(id);
  };

  const handleMarkAllAsRead = () => {
    if (confirm('모든 알림을 읽음 처리하시겠습니까?')) {
      markAllAsReadMutation.mutate();
    }
  };

  const handleDelete = (id: number) => {
    if (confirm('이 알림을 삭제하시겠습니까?')) {
      deleteMutation.mutate(id);
    }
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'FRIEND_REQUEST':
        return '👥';
      case 'ROOM_INVITATION':
        return '🎮';
      case 'EVALUATION_RECEIVED':
        return '⭐';
      case 'SYSTEM':
        return '🔔';
      default:
        return '📬';
    }
  };

  const getNotificationColor = (type: string) => {
    switch (type) {
      case 'FRIEND_REQUEST':
        return 'border-blue-500';
      case 'ROOM_INVITATION':
        return 'border-purple-500';
      case 'EVALUATION_RECEIVED':
        return 'border-yellow-500';
      case 'SYSTEM':
        return 'border-gray-500';
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
    <div className="min-h-screen bg-[#0e0e10] text-white">
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-3xl font-bold">알림</h1>
            {unreadCount > 0 && (
              <p className="text-gray-400 mt-1">
                읽지 않은 알림 <span className="text-purple-400 font-semibold">{unreadCount}</span>개
              </p>
            )}
          </div>
          {unreadCount > 0 && (
            <button
              onClick={handleMarkAllAsRead}
              disabled={markAllAsReadMutation.isPending}
              className="px-4 py-2 bg-purple-500 hover:bg-purple-600 disabled:bg-gray-600 rounded font-semibold transition"
            >
              모두 읽음 처리
            </button>
          )}
        </div>

        {/* Notifications List */}
        <div className="space-y-3">
          {isLoading ? (
            <div className="text-center text-gray-400 py-12">Loading...</div>
          ) : notifications.length === 0 ? (
            <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-12 text-center">
              <div className="text-6xl mb-4">🔔</div>
              <div className="text-gray-400 text-lg">알림이 없습니다</div>
            </div>
          ) : (
            notifications.map((notification: any) => (
              <div
                key={notification.id}
                className={`bg-[#18181b] border-2 ${
                  notification.isRead ? 'border-gray-700' : getNotificationColor(notification.type)
                } rounded-lg p-5 ${notification.isRead ? 'opacity-60' : ''} transition`}
              >
                <div className="flex items-start gap-4">
                  {/* Icon */}
                  <div className="text-3xl flex-shrink-0">{getNotificationIcon(notification.type)}</div>

                  {/* Content */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-white font-medium">{notification.message}</p>
                        <p className="text-gray-400 text-sm mt-1">{formatDate(notification.createdAt)}</p>
                      </div>

                      {/* Actions */}
                      <div className="flex gap-2 flex-shrink-0">
                        {!notification.isRead && (
                          <button
                            onClick={() => handleMarkAsRead(notification.id)}
                            disabled={markAsReadMutation.isPending}
                            className="text-xs px-3 py-1 bg-purple-500 hover:bg-purple-600 disabled:bg-gray-600 rounded transition"
                            title="읽음 처리"
                          >
                            ✓
                          </button>
                        )}
                        <button
                          onClick={() => handleDelete(notification.id)}
                          disabled={deleteMutation.isPending}
                          className="text-xs px-3 py-1 bg-red-500/20 hover:bg-red-500/40 disabled:bg-gray-600 text-red-400 rounded transition"
                          title="삭제"
                        >
                          ✕
                        </button>
                      </div>
                    </div>

                    {/* Related Link (if available) */}
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
  );
}
