import { useParams } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

/**
 * User profile page
 *
 * @author Gembud Team
 * @since 2026-02-26
 */
export default function ProfilePage() {
  const { userId } = useParams<{ userId: string }>();
  const { user } = useAuthStore();

  // For now, showing placeholder. Full implementation would fetch user data from API
  const isOwnProfile = user && userId === user.id.toString();

  return (
    <div className="min-h-screen bg-[#0e0e10] text-white">
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        {/* Profile Header */}
        <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-8 mb-6">
          <div className="flex items-start gap-6">
            {/* Avatar */}
            <div className="w-24 h-24 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center text-4xl font-bold">
              {user?.nickname?.[0] || '?'}
            </div>

            {/* Info */}
            <div className="flex-1">
              <h1 className="text-3xl font-bold mb-2">{isOwnProfile ? user?.nickname : `사용자 ${userId}`}</h1>
              <p className="text-gray-400 mb-4">{isOwnProfile ? user?.email : 'email@example.com'}</p>

              {/* Stats */}
              <div className="flex gap-6">
                <div>
                  <div className="text-2xl font-bold text-purple-400">36.5°C</div>
                  <div className="text-sm text-gray-400">매너 온도</div>
                </div>
                <div>
                  <div className="text-2xl font-bold">124</div>
                  <div className="text-sm text-gray-400">게임 수</div>
                </div>
                <div>
                  <div className="text-2xl font-bold">98</div>
                  <div className="text-sm text-gray-400">받은 평가</div>
                </div>
              </div>
            </div>

            {/* Edit Button (own profile only) */}
            {isOwnProfile && (
              <button className="px-4 py-2 bg-purple-500 hover:bg-purple-600 rounded font-semibold transition">
                프로필 수정
              </button>
            )}
          </div>
        </div>

        {/* Temperature Bar */}
        <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">매너 온도</h2>
          <div className="relative h-6 bg-gray-700 rounded-full overflow-hidden">
            <div
              className="absolute inset-y-0 left-0 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500 rounded-full transition-all"
              style={{ width: '65%' }}
            />
          </div>
          <p className="text-sm text-gray-400 mt-2">
            게임 매너가 좋으면 온도가 올라갑니다!
          </p>
        </div>

        {/* Evaluation Tags */}
        <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">받은 평가 태그</h2>
          <div className="flex flex-wrap gap-3">
            {['친절해요', '실력이 좋아요', '의사소통이 원활해요', '게임을 잘 리드해요', '시간을 잘 지켜요'].map((tag) => (
              <span
                key={tag}
                className="px-4 py-2 bg-purple-500/20 border border-purple-500 text-purple-300 rounded-full text-sm"
              >
                {tag} <span className="ml-1 font-semibold">24</span>
              </span>
            ))}
          </div>
        </div>

        {/* Recent Evaluations */}
        <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6">
          <h2 className="text-xl font-semibold mb-4">최근 받은 평가</h2>
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="p-4 bg-[#0e0e10] rounded">
                <div className="flex items-center justify-between mb-2">
                  <span className="font-semibold">익명의 평가자</span>
                  <div className="flex items-center gap-1">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <span key={star} className="text-yellow-400">★</span>
                    ))}
                  </div>
                </div>
                <p className="text-gray-300 text-sm">
                  함께 게임하기 정말 좋았습니다. 실력도 좋고 커뮤니케이션도 원활해서 재미있게 플레이했어요!
                </p>
                <p className="text-gray-500 text-xs mt-2">2일 전 • League of Legends</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
