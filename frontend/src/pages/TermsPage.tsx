export default function TermsPage() {
  return (
    <div className="min-h-screen bg-[#0e0e10] text-white">
      <div className="container mx-auto px-4 py-12 max-w-3xl">
        <h1 className="text-3xl font-bold mb-2">이용약관</h1>
        <p className="text-gray-400 mb-8">최종 수정일: 2026년 3월 4일</p>

        <div className="space-y-8 text-gray-300 leading-relaxed">
          <section>
            <h2 className="text-xl font-bold text-white mb-3">제1조 (목적)</h2>
            <p>본 약관은 Gembud(이하 "서비스")가 제공하는 게임 파티원 매칭 플랫폼 서비스의 이용과 관련하여 서비스와 이용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.</p>
          </section>

          <section>
            <h2 className="text-xl font-bold text-white mb-3">제2조 (서비스 이용)</h2>
            <ul className="list-disc list-inside space-y-1 ml-4">
              <li>서비스는 만 14세 이상 이용 가능합니다.</li>
              <li>회원가입은 이메일 또는 소셜 계정(Google, Discord)으로 가능합니다.</li>
              <li>하나의 계정만 생성할 수 있습니다.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-bold text-white mb-3">제3조 (이용자 의무)</h2>
            <p>이용자는 다음 행위를 해서는 안 됩니다:</p>
            <ul className="list-disc list-inside mt-2 space-y-1 ml-4">
              <li>타인에 대한 욕설, 비방, 차별적 발언</li>
              <li>허위 정보 제공 또는 타인 사칭</li>
              <li>서비스의 정상적인 운영을 방해하는 행위</li>
              <li>불법 또는 유해한 콘텐츠 게시</li>
              <li>저작권 등 지적재산권 침해 행위</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-bold text-white mb-3">제4조 (프리미엄 서비스)</h2>
            <p>프리미엄 구독 서비스는 테스트 운영 중이며, 구독 취소 시 남은 기간에 대한 환불은 제공되지 않습니다. 향후 유료 결제 도입 시 별도 안내합니다.</p>
          </section>

          <section>
            <h2 className="text-xl font-bold text-white mb-3">제5조 (서비스 변경 및 중단)</h2>
            <p>서비스는 서비스 내용을 변경하거나 중단할 수 있으며, 이로 인한 이용자의 손해에 대해 배상하지 않습니다. 단, 유료 서비스의 경우 별도 고지합니다.</p>
          </section>

          <section>
            <h2 className="text-xl font-bold text-white mb-3">제6조 (면책조항)</h2>
            <p>서비스는 이용자 간의 분쟁에 대해 책임지지 않습니다. 이용자 간 발생한 문제는 당사자 간에 해결하여야 하며, 서비스는 이에 관여하지 않습니다.</p>
          </section>

          <section>
            <h2 className="text-xl font-bold text-white mb-3">제7조 (문의)</h2>
            <p>
              약관 관련 문의: <a href="mailto:support@gembud.com" className="text-purple-400 hover:text-purple-300">support@gembud.com</a>
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
