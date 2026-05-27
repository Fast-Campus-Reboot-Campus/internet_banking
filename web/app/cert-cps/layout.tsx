import Link from 'next/link'

export default function CertCpsLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col" style={{ fontFamily: 'sans-serif' }}>
      {/* 헤더 */}
      <header className="border-b border-gray-200 bg-white">
        <div className="max-w-[1100px] mx-auto px-6 py-4 flex items-center justify-between">
          <Link href="/cert-cps" className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full border-2 border-[#3D4F47] flex items-center justify-center">
              <span className="text-[10px] font-black text-[#3D4F47]">AX</span>
            </div>
            <span className="text-[17px] font-bold text-gray-800">AXful인증서</span>
          </Link>
          <nav className="flex items-center gap-8 text-[17px] text-gray-600">
            {['AXful인증서 Lite', '자주 묻는 질문', '제휴 신청하기', '개발가이드', '서비스관리'].map((item) => (
              <Link key={item} href="#" className="hover:text-gray-900 transition-colors">
                {item}
              </Link>
            ))}
          </nav>
        </div>
      </header>

      {/* 본문 */}
      <main className="flex-1 max-w-[1100px] mx-auto w-full px-6 py-10">
        {children}
      </main>

      {/* 푸터 */}
      <footer className="border-t border-gray-200 bg-white mt-16">
        <div className="max-w-[1100px] mx-auto px-6 py-8 space-y-3">
          <div className="flex items-center gap-6 text-[16px] text-gray-500">
            <Link href="#" className="hover:text-gray-800">AXful인증서 개인정보 처리방침</Link>
            <span className="text-gray-300">|</span>
            <Link href="/cert-cps" className="hover:text-gray-800 font-medium text-gray-700">AXful인증서 인증업무준칙(CPS)</Link>
            <span className="text-gray-300">|</span>
            <Link href="#" className="hover:text-gray-800">AXful인증서 Lite 인증업무준칙(CPS)</Link>
          </div>
          <p className="text-[16px] text-gray-500">
            📱 고객센터 1588-0000, 1599-0000, 1644-0000&nbsp;&nbsp;|&nbsp;&nbsp;해외 +82-2-0000-0000
          </p>
          <Link href="#" className="inline-flex items-center gap-1 text-[16px] text-gray-500 hover:text-gray-800">
            💬 채팅·이메일상담 &gt;
          </Link>
          <p className="text-[15px] text-gray-400 pt-2">Copyright AXful Bank. All Rights Reserved.</p>
        </div>
      </footer>
    </div>
  )
}
