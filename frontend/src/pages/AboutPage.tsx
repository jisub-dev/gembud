import { Gamepad2, Users, Star, Shield } from 'lucide-react';

export default function AboutPage() {
  return (
    <div className="min-h-screen bg-[#0e0e10] text-white">
      <div className="container mx-auto px-4 py-12 max-w-3xl">
        <div className="text-center mb-12">
          <div className="flex items-center justify-center gap-3 mb-4">
            <Gamepad2 size={40} className="text-purple-400" />
            <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
              Gembud
            </h1>
          </div>
          <p className="text-gray-400 text-lg">게임 파티원 매칭 플랫폼</p>
        </div>

        <div className="space-y-8">
          <section className="bg-[#18181b] border border-gray-700 rounded-lg p-6">
            <h2 className="text-xl font-bold text-white mb-3">서비스 소개</h2>
            <p className="text-gray-300 leading-relaxed">
              Gembud는 게임을 함께 즐길 파티원을 찾는 커뮤니티 매칭 플랫폼입니다.
              원하는 게임의 대기방을 만들거나 참가하고, 실시간 채팅으로 파티원과 소통하세요.
              온도 시스템을 통해 신뢰할 수 있는 파티원을 찾을 수 있습니다.
            </p>
          </section>

          <section className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-[#18181b] border border-gray-700 rounded-lg p-5 text-center">
              <Users size={32} className="text-purple-400 mx-auto mb-3" />
              <h3 className="font-bold text-white mb-2">파티원 매칭</h3>
              <p className="text-gray-400 text-sm">게임별 대기방에서 원하는 파티원을 찾으세요</p>
            </div>
            <div className="bg-[#18181b] border border-gray-700 rounded-lg p-5 text-center">
              <Star size={32} className="text-yellow-400 mx-auto mb-3" />
              <h3 className="font-bold text-white mb-2">온도 시스템</h3>
              <p className="text-gray-400 text-sm">게임 후 상호 평가로 신뢰도를 쌓아보세요</p>
            </div>
            <div className="bg-[#18181b] border border-gray-700 rounded-lg p-5 text-center">
              <Shield size={32} className="text-green-400 mx-auto mb-3" />
              <h3 className="font-bold text-white mb-2">안전한 커뮤니티</h3>
              <p className="text-gray-400 text-sm">신고 및 제재 시스템으로 건강한 커뮤니티를 유지합니다</p>
            </div>
          </section>

          <section className="bg-[#18181b] border border-gray-700 rounded-lg p-6">
            <h2 className="text-xl font-bold text-white mb-3">문의</h2>
            <p className="text-gray-300">
              서비스 관련 문의사항은 아래 이메일로 연락해주세요.<br />
              <a href="mailto:support@gembud.com" className="text-purple-400 hover:text-purple-300">
                support@gembud.com
              </a>
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
