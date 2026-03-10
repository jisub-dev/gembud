import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, Users, Gamepad2, Star, Trash2, Check, ArrowRight, Filter } from 'lucide-react';
import {
  useNotifications,
  useMarkNotificationAsRead,
  useMarkAllNotificationsAsRead,
  useDeleteNotification,
} from '@/hooks/queries/useNotifications';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import type { Notification } from '@/types/notification';

type NotificationTab = 'all' | 'unread';
type NotificationCategory = 'all' | 'friend' | 'room' | 'evaluation' | 'other';

function getNotificationCategory(type: Notification['type']): NotificationCategory {
  switch (type) {
    case 'FRIEND_REQUEST':
    case 'FRIEND_ACCEPTED':
      return 'friend';
    case 'ROOM_INVITATION':
    case 'ROOM_JOIN':
    case 'ROOM_INVITE':
      return 'room';
    case 'EVALUATION_RECEIVED':
    case 'EVALUATION':
      return 'evaluation';
    default:
      return 'other';
  }
}

function getCtaLabel(type: Notification['type']): string {
  switch (type) {
    case 'FRIEND_REQUEST':
    case 'FRIEND_ACCEPTED':
      return '친구 페이지로 이동';
    case 'ROOM_INVITATION':
    case 'ROOM_INVITE':
    case 'ROOM_JOIN':
      return '방으로 이동';
    case 'EVALUATION_RECEIVED':
    case 'EVALUATION':
      return '평가 확인';
    default:
      return '자세히 보기';
  }
}

function formatDate(dateString: string) {
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
}

export default function NotificationsPage() {
  const navigate = useNavigate();
  const { data: notifications = [], isLoading } = useNotifications();
  const markAsReadMutation = useMarkNotificationAsRead();
  const markAllAsReadMutation = useMarkAllNotificationsAsRead();
  const deleteMutation = useDeleteNotification();

  const [activeTab, setActiveTab] = useState<NotificationTab>('all');
  const [activeCategory, setActiveCategory] = useState<NotificationCategory>('all');
  const [showMarkAllConfirm, setShowMarkAllConfirm] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const unreadCount = useMemo(
    () => notifications.filter((notification) => !notification.isRead).length,
    [notifications]
  );

  const filteredNotifications = useMemo(() => {
    return notifications.filter((notification) => {
      if (activeTab === 'unread' && notification.isRead) {
        return false;
      }

      if (activeCategory === 'all') {
        return true;
      }

      return getNotificationCategory(notification.type) === activeCategory;
    });
  }, [notifications, activeTab, activeCategory]);

  const handleMarkAsRead = (id: number) => {
    markAsReadMutation.mutate(id);
  };

  const handleMarkAllAsRead = () => {
    setShowMarkAllConfirm(true);
  };

  const handleDelete = (id: number) => {
    setDeletingId(id);
  };

  const handleNotificationClick = (notification: Notification) => {
    if (!notification.isRead) {
      markAsReadMutation.mutate(notification.id);
    }

    if (notification.type === 'FRIEND_REQUEST' || notification.type === 'FRIEND_ACCEPTED') {
      navigate('/friends');
      return;
    }

    if (notification.relatedUrl) {
      if (notification.relatedUrl.startsWith('/')) {
        navigate(notification.relatedUrl);
      } else {
        window.location.href = notification.relatedUrl;
      }
    }
  };

  const getNotificationIcon = (type: Notification['type']) => {
    switch (type) {
      case 'FRIEND_REQUEST':
      case 'FRIEND_ACCEPTED':
        return <Users size={22} className="text-blue-400" />;
      case 'ROOM_INVITATION':
      case 'ROOM_JOIN':
      case 'ROOM_INVITE':
        return <Gamepad2 size={22} className="text-emerald-400" />;
      case 'EVALUATION_RECEIVED':
      case 'EVALUATION':
        return <Star size={22} className="text-amber-400" />;
      default:
        return <Bell size={22} className="text-gray-400" />;
    }
  };

  const getNotificationBorder = (type: Notification['type']) => {
    switch (type) {
      case 'FRIEND_REQUEST':
      case 'FRIEND_ACCEPTED':
        return 'border-blue-500/60';
      case 'ROOM_INVITATION':
      case 'ROOM_JOIN':
      case 'ROOM_INVITE':
        return 'border-emerald-500/60';
      case 'EVALUATION_RECEIVED':
      case 'EVALUATION':
        return 'border-amber-500/60';
      default:
        return 'border-gray-700';
    }
  };

  const filterButtons: Array<{ key: NotificationCategory; label: string }> = [
    { key: 'all', label: '전체 타입' },
    { key: 'friend', label: '친구' },
    { key: 'room', label: '방' },
    { key: 'evaluation', label: '평가' },
    { key: 'other', label: '기타' },
  ];

  const emptyMessage =
    activeTab === 'unread'
      ? '읽지 않은 알림이 없습니다'
      : activeCategory === 'all'
        ? '알림이 없습니다'
        : '선택한 타입의 알림이 없습니다';

  return (
    <>
      {showMarkAllConfirm && (
        <ConfirmModal
          message="모든 알림을 읽음 처리하시겠습니까?"
          onConfirm={() => {
            markAllAsReadMutation.mutate();
            setShowMarkAllConfirm(false);
          }}
          onCancel={() => setShowMarkAllConfirm(false)}
          confirmLabel="읽음 처리"
        />
      )}
      {deletingId !== null && (
        <ConfirmModal
          message="이 알림을 삭제하시겠습니까?"
          onConfirm={() => {
            deleteMutation.mutate(deletingId);
            setDeletingId(null);
          }}
          onCancel={() => setDeletingId(null)}
          confirmLabel="삭제"
          danger
        />
      )}

      <div className="min-h-screen bg-[#0e0e10] text-white">
        <div className="container mx-auto max-w-5xl px-4 py-8">
          <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <Bell size={28} className="text-indigo-400" />
              <div>
                <h1 className="text-3xl font-bold">알림 센터</h1>
                <p className="mt-0.5 text-sm text-gray-400">최신 알림을 확인하고 바로 이동하세요</p>
              </div>
            </div>

            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllAsRead}
                disabled={markAllAsReadMutation.isPending}
                className="inline-flex items-center justify-center gap-2 rounded-lg bg-indigo-500 px-4 py-2 font-semibold transition hover:bg-indigo-600 disabled:bg-gray-600"
              >
                <Check size={16} />
                모두 읽음 처리
              </button>
            )}
          </div>

          <div className="mb-5 grid gap-3 sm:grid-cols-2">
            <div className="rounded-xl border border-gray-800 bg-[#15151a] p-4">
              <p className="text-xs text-gray-400">총 알림 수</p>
              <p className="mt-1 text-2xl font-bold text-white">{notifications.length}</p>
            </div>
            <div className="rounded-xl border border-indigo-500/40 bg-indigo-500/10 p-4">
              <p className="text-xs text-indigo-200">안 읽은 알림</p>
              <p className="mt-1 text-2xl font-bold text-indigo-300">{unreadCount}</p>
            </div>
          </div>

          <div className="mb-4 flex flex-wrap gap-2">
            <button
              onClick={() => setActiveTab('all')}
              className={`rounded-lg px-3 py-2 text-sm font-medium transition ${
                activeTab === 'all'
                  ? 'bg-white text-black'
                  : 'border border-gray-700 bg-[#18181b] text-gray-300 hover:border-gray-500'
              }`}
            >
              전체
            </button>
            <button
              onClick={() => setActiveTab('unread')}
              className={`rounded-lg px-3 py-2 text-sm font-medium transition ${
                activeTab === 'unread'
                  ? 'bg-white text-black'
                  : 'border border-gray-700 bg-[#18181b] text-gray-300 hover:border-gray-500'
              }`}
            >
              안 읽음
            </button>
          </div>

          <div className="mb-6 rounded-xl border border-gray-800 bg-[#15151a] p-3">
            <div className="mb-2 flex items-center gap-2 text-xs text-gray-400">
              <Filter size={14} /> 타입 필터
            </div>
            <div className="flex flex-wrap gap-2">
              {filterButtons.map((button) => (
                <button
                  key={button.key}
                  onClick={() => setActiveCategory(button.key)}
                  className={`rounded-lg px-3 py-1.5 text-sm transition ${
                    activeCategory === button.key
                      ? 'bg-indigo-500 text-white'
                      : 'border border-gray-700 bg-[#18181b] text-gray-300 hover:border-gray-500'
                  }`}
                >
                  {button.label}
                </button>
              ))}
            </div>
          </div>

          <div className="space-y-3">
            {isLoading ? (
              <LoadingSpinner className="py-12" />
            ) : filteredNotifications.length === 0 ? (
              <div className="rounded-lg border-2 border-gray-700 bg-[#18181b] p-12 text-center">
                <Bell size={56} className="mx-auto mb-4 text-gray-600" />
                <p className="mb-2 text-lg text-gray-300">{emptyMessage}</p>
                <p className="text-sm text-gray-500">필터를 초기화하거나 잠시 후 다시 확인해 주세요.</p>
                {(activeTab !== 'all' || activeCategory !== 'all') && (
                  <button
                    onClick={() => {
                      setActiveTab('all');
                      setActiveCategory('all');
                    }}
                    className="mt-4 rounded-lg border border-gray-600 px-4 py-2 text-sm text-gray-200 transition hover:border-gray-400"
                  >
                    필터 초기화
                  </button>
                )}
              </div>
            ) : (
              filteredNotifications.map((notification) => (
                <div
                  key={notification.id}
                  onClick={() => handleNotificationClick(notification)}
                  className={`cursor-pointer rounded-lg border-2 bg-[#18181b] p-4 transition hover:border-indigo-400/60 sm:p-5 ${
                    notification.isRead ? 'border-gray-700 opacity-70' : getNotificationBorder(notification.type)
                  }`}
                >
                  <div className="flex items-start gap-3 sm:gap-4">
                    <div className="mt-0.5 flex-shrink-0">{getNotificationIcon(notification.type)}</div>

                    <div className="min-w-0 flex-1">
                      <div className="mb-2 flex flex-col gap-2 sm:mb-0 sm:flex-row sm:items-start sm:justify-between">
                        <div className="min-w-0">
                          <p className="break-words font-medium text-white">{notification.message}</p>
                          <p className="mt-1 text-sm text-gray-400">{formatDate(notification.createdAt)}</p>
                        </div>

                        <div className="flex flex-wrap items-center gap-2 sm:justify-end">
                          {!notification.isRead && (
                            <button
                              onClick={(event) => {
                                event.stopPropagation();
                                handleMarkAsRead(notification.id);
                              }}
                              disabled={markAsReadMutation.isPending}
                              className="inline-flex items-center gap-1 rounded-md bg-indigo-500 px-2.5 py-1.5 text-xs font-medium transition hover:bg-indigo-600 disabled:bg-gray-600"
                              title="읽음 처리"
                            >
                              <Check size={13} /> 읽음
                            </button>
                          )}
                          <button
                            onClick={(event) => {
                              event.stopPropagation();
                              handleDelete(notification.id);
                            }}
                            disabled={deleteMutation.isPending}
                            className="inline-flex items-center gap-1 rounded-md bg-red-500/20 px-2.5 py-1.5 text-xs font-medium text-red-300 transition hover:bg-red-500/35 disabled:bg-gray-600 disabled:text-gray-300"
                            title="삭제"
                          >
                            <Trash2 size={13} /> 삭제
                          </button>
                        </div>
                      </div>

                      {notification.relatedUrl && (
                        <button
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            handleNotificationClick(notification);
                          }}
                          className="mt-2 inline-flex items-center gap-1 text-sm text-indigo-300 transition hover:text-indigo-200"
                        >
                          {getCtaLabel(notification.type)} <ArrowRight size={14} />
                        </button>
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
