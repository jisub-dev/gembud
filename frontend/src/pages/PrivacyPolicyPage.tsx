export default function PrivacyPolicyPage() {
  return (
    <div className="min-h-screen bg-[#0e0e10] text-white">
      <div className="container mx-auto px-4 py-12 max-w-3xl">
        <h1 className="text-3xl font-bold mb-2">개인정보처리방침</h1>
        <p className="text-gray-400 mb-8">최종 수정일: 2026년 3월 4일</p>

        <div className="space-y-8 text-gray-300 leading-relaxed">
          <section>
            <h2 className="text-xl font-bold text-white mb-3">1. 수집하는 개인정보</h2>
            <p>Gembud(이하 "서비스")는 다음과 같은 개인정보를 수집합니다:</p>
            <ul className="list-disc list-inside mt-2 space-y-1 ml-4">
              <li>이메일 주소 (회원가입 및 로그인)</li>
              <li>닉네임</li>
              <li>OAuth 소셜 로그인 정보 (Google, Discord)</li>
              <li>서비스 이용 기록 (방 참가, 채팅, 평가 내역)</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-bold text-white mb-3">2. 개인정보 이용 목적</h2>
            <ul className="list-disc list-inside space-y-1 ml-4">
              <li>서비스 제공 및 회원 관리</li>
              <li>파티원 매칭 서비스 운영</li>
              <li>서비스 개선 및 신규 기능 개발</li>
              <li>불량 이용자 제재 및 커뮤니티 안전 관리</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-bold text-white mb-3">3. 개인정보 보유 기간</h2>
            <p>회원 탈퇴 시까지 보유하며, 탈퇴 후 즉시 파기합니다. 단, 관련 법령에 따라 보존이 필요한 경우 해당 기간 동안 보관합니다.</p>
          </section>

          <section>
            <h2 className="text-xl font-bold text-white mb-3">4. 제3자 제공</h2>
            <p>서비스는 원칙적으로 이용자의 개인정보를 외부에 제공하지 않습니다. 단, 법령에 의한 요청이 있는 경우 예외로 합니다.</p>
          </section>

          <section>
            <h2 className="text-xl font-bold text-white mb-3">5. 광고 서비스</h2>
            <p>본 서비스는 Google AdSense를 통한 광고를 게재할 수 있으며, Google은 쿠키를 사용하여 맞춤형 광고를 제공할 수 있습니다. 자세한 내용은 <a href="https://policies.google.com/privacy" target="_blank" rel="noopener noreferrer" className="text-purple-400 hover:text-purple-300">Google 개인정보처리방침</a>을 참조하세요.</p>
          </section>

          <section>
            <h2 className="text-xl font-bold text-white mb-3">6. 이용자의 권리</h2>
            <p>이용자는 언제든지 자신의 개인정보에 대한 열람, 수정, 삭제를 요청할 수 있습니다. 문의는 <a href="mailto:support@gembud.com" className="text-purple-400 hover:text-purple-300">support@gembud.com</a>으로 연락해주세요.</p>
          </section>

          <section>
            <h2 className="text-xl font-bold text-white mb-3">7. 문의처</h2>
            <p>
              개인정보 관련 문의: <a href="mailto:support@gembud.com" className="text-purple-400 hover:text-purple-300">support@gembud.com</a>
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
