'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState, useEffect } from 'react'

export default function SecurityInstallPage() {
  const router = useRouter()
  const [checking, setChecking] = useState(true)

  useEffect(() => {
    const t = setTimeout(() => setChecking(false), 2000)
    return () => clearTimeout(t)
  }, [])

  return (
    <div className="min-h-screen flex flex-col" style={{ backgroundColor: '#f5f5f5' }}>

      {/* 헤더 */}
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-[1200px] mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-kb-yellow text-xl font-black">✱</span>
            <span className="text-[20px] font-bold text-gray-800">보안프로그램 설치</span>
          </div>
          <nav className="flex items-center gap-5 text-[14px] text-gray-600">
            {[
              { label: '개인', href: '/personal' },
              { label: '기업', href: '/biz' },
              { label: '자산관리', href: '#' },
              { label: '부동산', href: '#' },
              { label: '퇴직연금', href: '#' },
              { label: '카드', href: '#' },
            ].map((item, i) => (
              <span key={item.label} className="flex items-center gap-5">
                {i > 0 && <span className="text-gray-300">|</span>}
                <Link href={item.href} className="hover:text-gray-900">{item.label}</Link>
              </span>
            ))}
            <span className="text-gray-300">|</span>
            <Link href="#" className="hover:text-gray-900">전체서비스 ▼</Link>
            <Link href="#" className="hover:text-gray-900">GLOBAL ▼</Link>
            <button className="hover:text-gray-900">🔍</button>
            <button className="hover:text-gray-900">☰</button>
          </nav>
        </div>
      </header>

      {/* 본문 */}
      <main className="flex-1 max-w-[900px] mx-auto w-full px-6 py-12 space-y-6">

        {/* 히어로 */}
        <div className="flex items-center justify-between">
          <div className="space-y-2">
            <h1 className="text-[22px] font-bold text-gray-800">
              안전한 인터넷뱅킹을 위해 보안프로그램 사용을 권장합니다.
            </h1>
            <p className="text-[14px] text-gray-500">
              통합 보안프로그램의 경우 OTP 이용시 자율적으로 선택 설치가 가능합니다.
            </p>
          </div>
          {/* 자물쇠+모니터 일러스트 */}
          <div className="flex-shrink-0 relative" style={{ width: 120, height: 90 }}>
            <div className="absolute right-0 bottom-0 w-24 h-16 border-2 border-gray-300 rounded bg-white flex items-center justify-center">
              <div className="w-10 h-10 rounded bg-kb-yellow/20 border border-kb-yellow flex items-center justify-center">
                <span className="text-xl">🛡️</span>
              </div>
            </div>
            <div className="absolute left-0 bottom-4 text-4xl">🔒</div>
          </div>
        </div>

        {/* 경고 문구 */}
        <div className="space-y-1">
          <p className="text-[13px] text-gray-600">
            ※ 보안프로그램 설치 후에도 설치안내 팝업이 뜨는 경우 다음 이용 안내 확인이 필요합니다.
          </p>
          <Link href="#" className="text-[13px] text-[#8B6914] underline underline-offset-2 hover:text-kb-taupe">
            브라우저 업데이트에 대한 ASTx 이용안내 &gt;
          </Link>
        </div>

        {/* 프로그램 카드 */}
        <div className="bg-white border border-gray-200 p-6 space-y-6">
          <div className="grid grid-cols-2 divide-x divide-gray-200 gap-0">

            {/* 공동인증프로그램 */}
            <div className="pr-8 space-y-4">
              <div className="flex items-center gap-2">
                <span className="text-2xl">📁</span>
                <h2 className="text-[17px] font-bold text-gray-800">공동인증프로그램</h2>
              </div>
              <div className="space-y-1">
                <p className="text-[13px] text-gray-500">프로그램명 :
                  <span className="text-[#C47A1E] font-medium ml-1">WizIn-Delfino G3</span>
                </p>
                <p className="text-[13px] text-gray-600 leading-relaxed">
                  공동인증서 로그인과 거래내역에 대한 전자서명 등<br />사용자 인증에 사용됩니다.
                </p>
              </div>

              {checking ? (
                <div className="flex justify-center py-4">
                  <div className="border border-gray-300 bg-white px-6 py-2 text-[13px] text-gray-500 flex items-center gap-2">
                    <svg className="animate-spin w-4 h-4 text-gray-400" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4l3-3-3-3v4a8 8 0 00-8 8h4z"/>
                    </svg>
                    checking
                  </div>
                </div>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  <button className="col-span-1 row-span-2 bg-kb-yellow py-3 text-[14px] font-bold text-gray-800 hover:brightness-95 flex items-center justify-center gap-1">
                    ⬇ 설치하기
                  </button>
                  <button className="border border-gray-300 py-2 text-[13px] text-gray-600 hover:bg-gray-50">FAQ</button>
                  <button className="border border-gray-300 py-2 text-[13px] text-gray-600 hover:bg-gray-50">이용안내</button>
                </div>
              )}
            </div>

            {/* 통합 보안프로그램 */}
            <div className="pl-8 space-y-4">
              <div className="flex items-center gap-2">
                <span className="text-2xl">🛡️</span>
                <h2 className="text-[17px] font-bold text-gray-800">통합 보안프로그램</h2>
              </div>
              <div className="space-y-1">
                <p className="text-[13px] text-gray-500">프로그램명 :
                  <span className="text-[#C47A1E] font-medium ml-1">AhnLab Safe Transaction</span>
                </p>
                <p className="text-[13px] text-gray-600 leading-relaxed">
                  키보드 보안, 백신/방화벽, 피싱/파밍 방지, 단말환경<br />수집 등 종합적인 보안을 제공합니다.
                </p>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <button className="col-span-1 row-span-2 bg-kb-yellow py-3 text-[14px] font-bold text-gray-800 hover:brightness-95 flex items-center justify-center gap-1">
                  ⬇ 설치하기
                </button>
                <button className="border border-gray-300 py-2 text-[13px] text-gray-600 hover:bg-gray-50">FAQ</button>
                <button className="border border-gray-300 py-2 text-[13px] text-gray-600 hover:bg-gray-50">이용안내</button>
              </div>
            </div>
          </div>

          {/* 자세히보기 */}
          <div className="border-t border-gray-200 pt-4 flex justify-center">
            <button className="text-[14px] text-gray-600 flex items-center gap-1 hover:text-gray-900">
              자세히보기 <span>⊙</span>
            </button>
          </div>
        </div>

        {/* 새로고침 / 홈으로 이동 */}
        <div className="flex justify-center gap-3">
          <button
            onClick={() => window.location.reload()}
            className="px-8 py-2.5 border border-gray-300 text-[14px] text-gray-700 bg-white hover:bg-gray-50 transition-colors"
          >
            새로고침
          </button>
          <button
            onClick={() => router.push('/personal')}
            className="px-8 py-2.5 border border-gray-300 text-[14px] text-gray-700 bg-white hover:bg-gray-50 transition-colors"
          >
            홈으로 이동
          </button>
        </div>

        {/* PC 환경 정보 */}
        <div className="space-y-1 text-[12px] text-gray-500">
          <p>- 고객님 PC환경 : [Windows64,Win64][Chrome,148.0.0.0][Delfino]Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36</p>
          <p>- 수동설치 후에는 <span className="underline cursor-pointer">새로고침</span> 또는 재접속하여 확인하시기 바랍니다.</p>
        </div>
      </main>

      {/* 푸터 */}
      <footer className="bg-white border-t border-gray-200 py-4">
        <div className="max-w-[900px] mx-auto px-6 space-y-2">
          <div className="flex flex-wrap gap-x-4 gap-y-1 text-[12px] text-gray-500">
            {['보호금융상품등록부','전자민원접수','전자금융거래기본약관','개인정보 처리방침','신용정보활용체제','위치기반서비스 이용약관','경영공시'].map((item) => (
              <Link key={item} href="#" className="hover:underline">{item}</Link>
            ))}
          </div>
          <div className="flex flex-wrap gap-x-4 gap-y-1 text-[12px] text-gray-500">
            {['이용상담','보안프로그램','사고신고','그룹 내 고객정보 제공안내','스튜어드십 코드','AXful인증서 제휴문의','AXful 뱅킹 Ads'].map((item) => (
              <Link key={item} href="#" className="hover:underline">{item}</Link>
            ))}
          </div>
        </div>
      </footer>
    </div>
  )
}
