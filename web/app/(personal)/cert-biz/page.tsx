'use client'

import Link from 'next/link'
import { useState } from 'react'

const CERT_BIZ_TABS = [
  'AXful인증서(기업)',
  'AXful인증서',
  '공동인증서',
  '금융인증서',
  '전자세금용인증서',
  '인증서 발급안내',
  '인증센터 FAQ',
]

const SUB_MENUS: Record<string, string[]> = {
  'AXful인증서(기업)': [
    'AXful인증서(기업)이란?',
    '인증서 발급/재발급',
    '인증서 관리',
    '영수증/세금계산서',
  ],
  '공동인증서': [
    '인증서 발급/재발급',
    '타행·타기관인증서 등록/해제',
    '인증서 갱신',
    '인증서 관리',
    '인증서폐기/수수료환급등록',
    '영수증/세금계산서',
    '스마트폰 인증서 복사',
  ],
  '금융인증서': [
    '인증서 발급/재발급',
    '인증서 갱신',
    '인증서 공유',
    '타행(기관)인증서 등록/해제',
    '인증서 관리',
    '인증서 폐기/수수료 환급등록',
    '영수증/세금계산서',
    '인증서 이용안내',
  ],
  '전자세금용인증서': [
    '인증서 발급/재발급',
    '인증서 갱신',
    '인증서폐기/수수료 환급등록',
    '영수증/세금계산서',
  ],
}

const QUICK_ITEMS = [
  { label: '스마트폰 인증서 복사', icon: '📲' },
  { label: '인증서 관리', icon: '🔧' },
  { label: '인증서 폐기', icon: '🗑️' },
  { label: '영수증 세금계산서', icon: '🧾' },
  { label: '수수료 납부', icon: '💳' },
]

const FAQ_ITEMS = [
  '개인용 공동인증서는 어떻게 발급받나요?',
  '인증서 관련 거래시(발급/재발급/갱신/수수료 환급 등) 페이지 오류 또는 스...',
  '기업용 인증서 발급절차는 어떻게 되나요?',
]

export default function CertBizPage() {
  const [activeTab, setActiveTab] = useState('AXful인증서(기업)')
  const [hoveredTab, setHoveredTab] = useState<string | null>(null)

  return (
    <>
      {/* 서브 네비게이션 */}
      <nav style={{ backgroundColor: '#3D4F47' }} onMouseLeave={() => setHoveredTab(null)}>
        <div className="max-w-kb-container mx-auto px-6">
          <div className="flex">
            {CERT_BIZ_TABS.map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                onMouseEnter={() => setHoveredTab(tab)}
                className={`px-5 py-4 text-base transition-colors whitespace-nowrap
                  ${activeTab === tab
                    ? 'text-white font-bold border-b-2 border-white'
                    : 'text-white/60 hover:text-white hover:bg-black/20'
                  }`}
              >
                {tab}
              </button>
            ))}
          </div>
        </div>
      </nav>

      {/* 세부 메뉴 */}
      {SUB_MENUS[hoveredTab ?? activeTab] && (
        <div className="border-b border-kb-border bg-white">
          <div className="max-w-kb-container mx-auto px-6 py-8">
            <div className="grid grid-cols-5 gap-x-6 gap-y-8">
              {SUB_MENUS[hoveredTab ?? activeTab]!.map((item) => (
                <Link
                  key={item}
                  href="#"
                  className="text-base text-kb-text font-medium hover:text-kb-taupe hover:underline leading-snug"
                >
                  {item}
                </Link>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 히어로 섹션 */}
      <div style={{ backgroundColor: '#EEF2FA' }}>
        <div className="max-w-kb-container mx-auto px-6 py-14 flex items-center justify-between">
          <div className="space-y-4 max-w-lg">
            <h2 className="text-[32px] font-bold text-kb-text leading-tight">
              AXful인증서(기업)
            </h2>
            <p className="text-base text-kb-text-body leading-relaxed">
              OTP 없이 6자리 비밀번호로 인증 완료!<br />
              지금 발급 받으면 3년간 무료
            </p>
            <Link href="#" className="block text-base text-kb-text font-medium underline underline-offset-2">
              자세히 보기
            </Link>
          </div>

          {/* 기업 방패 일러스트 */}
          <div className="flex-shrink-0 flex items-center gap-6" style={{ width: 340 }}>
            {/* 자물쇠 */}
            <div className="text-6xl opacity-80">🔒</div>

            {/* KB 기업 방패 */}
            <div className="relative">
              <svg viewBox="0 0 160 190" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ width: 160, height: 190 }}>
                <path d="M80 8L12 34V100C12 142 45 170 80 180C115 170 148 142 148 100V34L80 8Z" fill="#4A6FA5"/>
                <path d="M80 8L12 34V100C12 142 45 170 80 180C115 170 148 142 148 100V34L80 8Z" fill="#6B8EC4" clipPath="url(#biz-left)"/>
                <clipPath id="biz-left">
                  <rect x="0" y="0" width="80" height="200"/>
                </clipPath>
                <text x="80" y="100" textAnchor="middle" fontSize="32" fontWeight="900" fill="white" letterSpacing="-1">AX</text>
                <text x="80" y="125" textAnchor="middle" fontSize="14" fontWeight="bold" fill="white">기업</text>
              </svg>
              {/* 체크 뱃지 */}
              <div className="absolute -bottom-2 -right-2 w-10 h-10 rounded-full bg-green-500 flex items-center justify-center shadow-md">
                <span className="text-white font-bold text-lg">✓</span>
              </div>
            </div>

            {/* PIN 입력 표시 */}
            <div className="bg-white rounded-lg shadow-md px-4 py-2 flex items-center gap-1">
              {Array.from({ length: 6 }).map((_, i) => (
                <span key={i} className="text-kb-text font-bold text-lg">✱</span>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* 메인 카드 3종 */}
      <div style={{ backgroundColor: '#EEF2FA' }} className="pb-12">
        <div className="max-w-kb-container mx-auto px-6">
          <div className="grid grid-cols-3 gap-6">
            {[
              {
                title: 'AXful인증서(기업)\n전자세금계산서 발급',
                desc: 'AXful인증서(기업)으로\n세금계산서도 발급하세요',
              },
              {
                title: '공동인증서 발급/재발급',
                desc: '인증서를 처음 발급하시거나 유효기간\n만료 등으로 폐기된 경우 재발급하세요',
              },
              {
                title: '타행·타기관인증서\n등록/해제',
                desc: '타행이나 타기관에서 발급한 인증서를\n등록하시고 편리하게 이용하세요',
              },
            ].map((card) => (
              <div key={card.title} className="bg-white rounded-xl p-7 flex flex-col gap-3">
                <h3 className="text-base font-bold text-kb-text leading-snug whitespace-pre-line">{card.title}</h3>
                <p className="text-sm text-kb-text-muted leading-relaxed whitespace-pre-line">{card.desc}</p>
                <Link href="#" className="text-sm text-kb-text-body hover:text-kb-text mt-auto">
                  바로가기 &gt;
                </Link>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 빠른 바로가기 5종 */}
      <div className="max-w-kb-container mx-auto px-6 py-10">
        <div className="border border-kb-border-dark rounded-xl overflow-hidden">
          <div className="grid grid-cols-5 divide-x divide-kb-border">
            {QUICK_ITEMS.map((item) => (
              <div key={item.label} className="p-6 flex flex-col gap-3">
                <p className="text-base font-semibold text-kb-text">{item.label}</p>
                <Link href="#" className="text-sm text-kb-text-muted hover:text-kb-text">
                  바로가기 &gt;
                </Link>
                <div className="flex justify-end mt-2">
                  <span className="text-3xl opacity-60">{item.icon}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* FAQ */}
      <div className="max-w-kb-container mx-auto px-6 pb-12">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-xl font-bold text-kb-text">인증센터 FAQ</h3>
          <Link href="#" className="text-sm text-kb-blue hover:underline">더보기</Link>
        </div>
        <div className="grid grid-cols-2">
          <ul className="border border-kb-border divide-y divide-kb-border">
            {FAQ_ITEMS.map((item) => (
              <li key={item}>
                <Link href="#" className="block px-5 py-4 text-base text-kb-text-body hover:bg-kb-beige-light transition-colors">
                  {item}
                </Link>
              </li>
            ))}
          </ul>
          <div className="border border-kb-border border-l-0 bg-kb-beige-light flex items-center justify-center p-8">
            <div className="text-center space-y-2">
              <div className="text-4xl mb-3">📋</div>
              <p className="text-base font-bold text-kb-text">전자금융거래 보안 10계명</p>
              <p className="text-sm text-kb-text-muted leading-relaxed">
                전자금융 거래시 고객님의 필수 정보가<br />유출되지 않도록 꼭 지켜주세요
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
