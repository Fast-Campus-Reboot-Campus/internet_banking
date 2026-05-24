'use client'

import Link from 'next/link'
import { useState } from 'react'

const CERT_TABS = [
  'AXful인증서',
  'AXful인증서 Lite',
  '금융인증서',
  '공동인증서(구 공인인증서)',
  '기타인증서비스',
]

type SubMenuItem = string | { label: string; href: string }

const SUB_MENUS: Record<string, SubMenuItem[]> = {
  '금융인증서': [
    { label: '인증서 발급/재발급', href: '/cert/fin-cert-issue' },
    '타행·타기관 인증서 등록',
    '타행·타기관 인증서 해제',
    '인증서 갱신',
    '인증서 폐기',
    { label: '인증서 관리', href: '/cert/cert-management' },
    '이용안내',
  ],
  '기타인증서비스': [
    '인증서휴대폰저장서비스(유료)',
  ],
  '공동인증서(구 공인인증서)': [
    { label: '공동인증서 발급/재발급', href: '/cert/joint-cert-issue' },
    '타행·타기관인증서 등록/해제',
    '인증서 갱신',
    { label: '인증서 관리', href: '/cert/joint-cert-management' },
    '인증서폐기/수수료환급등록',
    '영수증/세금계산서',
    '스마트폰 인증서 복사',
    'AXful인증서로 발급받기',
  ],
}

const FAQ_ITEMS = [
  '개인용 공동인증서는 어떻게 발급받나요?',
  '인증서 관련 거래시(발급/재발급/갱신/수수료 환급 등) 페이지 오류 또는 스...',
  '기업용 인증서 발급절차는 어떻게 되나요?',
]

export default function CertPage() {
  const [activeTab, setActiveTab] = useState('AXful인증서')
  const [hoveredTab, setHoveredTab] = useState<string | null>(null)

  return (
    <>
      {/* 서브 네비게이션 */}
      <nav style={{ backgroundColor: '#3D4F47' }} onMouseLeave={() => setHoveredTab(null)}>
        <div className="max-w-kb-container mx-auto px-6">
          <div className="flex">
            {CERT_TABS.map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                onMouseEnter={() => setHoveredTab(tab)}
                className={`px-6 py-4 text-base transition-colors whitespace-nowrap
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
              {SUB_MENUS[hoveredTab ?? activeTab]!.map((item) => {
                const label = typeof item === 'string' ? item : item.label
                const href = typeof item === 'string' ? '#' : item.href
                return (
                  <Link
                    key={label}
                    href={href}
                    className="text-base text-kb-text font-medium hover:text-kb-taupe hover:underline leading-snug"
                  >
                    {label}
                  </Link>
                )
              })}
            </div>
          </div>
        </div>
      )}

      {/* 히어로 섹션 */}
      <div style={{ backgroundColor: '#F0F8F4' }}>
        <div className="max-w-kb-container mx-auto px-6 py-16 flex items-center justify-between">
          <div className="space-y-4 max-w-lg">
            <h2 className="text-[32px] font-bold text-kb-text leading-tight">
              금융을 넘어 일상생활까지<br />
              나를 인증해주는 AXful인증서
            </h2>
            <p className="text-base text-kb-text-body leading-relaxed">
              간편하게 발급해서 다양한 사이트에서 안전하고 쉽게 이용가능해요
            </p>
            <div className="flex gap-6 pt-2">
              <Link href="#" className="text-base text-kb-text font-medium underline underline-offset-2">바로가기</Link>
              <Link href="#" className="text-base text-kb-text font-medium underline underline-offset-2">제휴 신청하기</Link>
            </div>
            <div className="flex items-center gap-4 pt-4 border-t border-kb-border">
              {/* QR 플레이스홀더 */}
              <div className="w-[72px] h-[72px] bg-white border border-kb-border flex items-center justify-center flex-shrink-0">
                <svg width="52" height="52" viewBox="0 0 100 100" fill="none" opacity="0.4">
                  <rect x="5" y="5" width="35" height="35" rx="2" stroke="#333" strokeWidth="3" fill="none"/>
                  <rect x="13" y="13" width="19" height="19" fill="#333"/>
                  <rect x="60" y="5" width="35" height="35" rx="2" stroke="#333" strokeWidth="3" fill="none"/>
                  <rect x="68" y="13" width="19" height="19" fill="#333"/>
                  <rect x="5" y="60" width="35" height="35" rx="2" stroke="#333" strokeWidth="3" fill="none"/>
                  <rect x="13" y="68" width="19" height="19" fill="#333"/>
                  <rect x="60" y="60" width="10" height="10" fill="#333"/>
                  <rect x="75" y="60" width="10" height="10" fill="#333"/>
                  <rect x="60" y="75" width="10" height="10" fill="#333"/>
                  <rect x="75" y="75" width="10" height="10" fill="#333"/>
                </svg>
              </div>
              <div>
                <p className="text-base font-bold text-kb-text">AXful인증서 발급</p>
                <p className="text-sm text-kb-text-muted leading-relaxed">
                  AXful 뱅킹에서<br />즉시 발급받으세요
                </p>
              </div>
            </div>
          </div>

          {/* 우측: KB 방패 일러스트 */}
          <div className="flex-shrink-0 flex items-center justify-center" style={{ width: 340 }}>
            <div className="relative flex items-center justify-center">
              {/* 받침대 */}
              <div className="absolute bottom-0 w-56 h-8 rounded-full bg-gradient-to-b from-gray-300 to-gray-400 opacity-40" />
              {/* 방패 */}
              <svg viewBox="0 0 160 190" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ width: 200, height: 240 }}>
                <path d="M80 8L12 34V100C12 142 45 170 80 180C115 170 148 142 148 100V34L80 8Z" fill="#544C40"/>
                <path d="M80 8L12 34V100C12 142 45 170 80 180C115 170 148 142 148 100V34L80 8Z" fill="#FFCC00" clipPath="url(#left-half)"/>
                <clipPath id="left-half">
                  <rect x="0" y="0" width="80" height="200"/>
                </clipPath>
                <text x="80" y="108" textAnchor="middle" fontSize="38" fontWeight="900" fill="white" letterSpacing="-1">AX</text>
              </svg>
              {/* Trust e-Sign 뱃지 */}
              <div className="absolute top-4 right-4 w-14 h-14 rounded-full border-2 border-blue-700 bg-white flex flex-col items-center justify-center shadow">
                <p className="text-[7px] font-bold text-blue-700 leading-tight text-center">TRUST<br/>e-Sign</p>
              </div>
              {/* 본인확인기관 뱃지 */}
              <div className="absolute bottom-8 right-0 w-12 h-12 rounded-full bg-white border border-kb-border flex items-center justify-center shadow">
                <div className="w-8 h-8 rounded-full bg-gradient-to-b from-blue-600 to-red-600" />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 본문 영역 */}
      <div className="max-w-kb-container mx-auto px-6 py-12 space-y-12">

        {/* 타기관 인증서 등록 카드 */}
        <div className="grid grid-cols-3 gap-6">
          {[
            {
              title: '타기관 공동인증서 등록',
              icon: (
                <svg viewBox="0 0 48 48" fill="none" className="w-10 h-10">
                  <rect x="6" y="6" width="28" height="36" rx="3" fill="#FFCC00" opacity="0.3"/>
                  <rect x="6" y="6" width="28" height="36" rx="3" stroke="#FFCC00" strokeWidth="2"/>
                  <circle cx="36" cy="36" r="9" fill="#FFCC00"/>
                  <path d="M33 36l2 2 4-4" stroke="#333" strokeWidth="1.8" strokeLinecap="round"/>
                </svg>
              ),
            },
            {
              title: '타기관 금융인증서 등록',
              icon: (
                <svg viewBox="0 0 48 48" fill="none" className="w-10 h-10">
                  <circle cx="24" cy="22" r="14" fill="#FFCC00" opacity="0.3" stroke="#FFCC00" strokeWidth="2"/>
                  <path d="M17 22 Q24 14 31 22" stroke="#FFCC00" strokeWidth="2"/>
                  <circle cx="36" cy="36" r="9" fill="#FFCC00"/>
                  <path d="M33 36l2 2 4-4" stroke="#333" strokeWidth="1.8" strokeLinecap="round"/>
                </svg>
              ),
            },
            {
              title: '공동인증서 스마트폰 인증서 복사',
              icon: (
                <svg viewBox="0 0 48 48" fill="none" className="w-10 h-10">
                  <rect x="8" y="6" width="18" height="28" rx="3" fill="#FFCC00" opacity="0.3" stroke="#FFCC00" strokeWidth="2"/>
                  <rect x="22" y="14" width="18" height="28" rx="3" fill="#FFCC00" opacity="0.6" stroke="#FFCC00" strokeWidth="2"/>
                  <circle cx="36" cy="36" r="9" fill="#FFCC00"/>
                  <path d="M33 36l2 2 4-4" stroke="#333" strokeWidth="1.8" strokeLinecap="round"/>
                </svg>
              ),
            },
          ].map((card) => (
            <div
              key={card.title}
              className="border border-kb-border-dark rounded-xl p-6 flex items-center justify-between hover:shadow-sm transition-shadow cursor-pointer"
            >
              <div>
                <p className="text-base font-semibold text-kb-text mb-2">{card.title}</p>
                <Link href="#" className="text-sm text-kb-blue hover:underline">
                  바로가기 &gt;
                </Link>
              </div>
              <div className="flex-shrink-0">{card.icon}</div>
            </div>
          ))}
        </div>

        {/* FAQ */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-xl font-bold text-kb-text">자주 찾는 질문(FAQ)</h3>
            <Link href="#" className="text-sm text-kb-blue hover:underline">더보기</Link>
          </div>
          <div className="grid grid-cols-2">
            <ul className="border border-kb-border divide-y divide-kb-border">
              {FAQ_ITEMS.map((item) => (
                <li key={item}>
                  <Link
                    href="#"
                    className="block px-5 py-4 text-base text-kb-text-body hover:bg-kb-beige-light transition-colors"
                  >
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

      </div>
    </>
  )
}
