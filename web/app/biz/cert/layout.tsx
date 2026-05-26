'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'

const CERT_NAV_TABS = [
  { label: 'AXful인증서(기업)', href: '#', prefix: '' },
  { label: 'AXful인증서', href: '#', prefix: '' },
  { label: '공동인증서', href: '/biz/cert/joint-cert-issue', prefix: '/biz/cert/joint' },
  { label: '금융인증서', href: '/biz/cert/fin-cert-issue', prefix: '/biz/cert/fin' },
  { label: '전자세금용인증서', href: '#', prefix: '' },
  { label: '인증서 발급안내', href: '#', prefix: '' },
  { label: '인증센터 FAQ', href: '#', prefix: '' },
]

const JOINT_CERT_ITEMS = [
  { label: '인증서 발급/재발급', href: '/biz/cert/joint-cert-issue' },
  { label: '타행·타기관인증서 등록/해제', href: '#' },
  { label: '인증서 갱신', href: '#' },
  { label: '인증서 관리', href: '/biz/cert/joint-cert-management' },
  { label: '인증서폐기/수수료환급동록', href: '#' },
  { label: '영수증/세금계산서', href: '#' },
  { label: '스마트폰 인증서 복사', href: '#' },
]

const FIN_CERT_ITEMS = [
  { label: '인증서 발급/재발급', href: '/biz/cert/fin-cert-issue' },
  { label: '인증서 갱신', href: '#' },
  { label: '인증서 공유', href: '#' },
  { label: '타행(기관)인증서 등록/해제', href: '#' },
  { label: '인증서 관리', href: '/biz/cert/fin-cert-management' },
  { label: '인증서 폐기/수수료 환급동록', href: '#' },
  { label: '영수증/세금계산서', href: '#' },
  { label: '인증서 이용안내', href: '#' },
]

const SIDEBAR_SECTIONS = [
  { label: 'AXful인증서(기업)', prefix: '', items: null },
  { label: 'AXful인증서', prefix: '', items: null },
  { label: '공동인증서', prefix: '/biz/cert/joint', items: JOINT_CERT_ITEMS },
  { label: '금융인증서', prefix: '/biz/cert/fin', items: FIN_CERT_ITEMS },
  { label: '전자세금용 인증서', prefix: '', items: null },
  { label: '인증서 발급안내', prefix: '', items: null },
  { label: '인증센터 FAQ', prefix: '', items: null },
]

export default function BizCertLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()

  return (
    <div>
      {/* 페이지 타이틀 바 */}
      <div className="bg-white border-b border-kb-border">
        <div className="max-w-kb-container mx-auto px-6 py-4">
          <h1 className="text-2xl font-bold text-kb-text">인증센터(기업)</h1>
        </div>
      </div>

      {/* 인증센터 탭 네비 */}
      <nav style={{ backgroundColor: '#3D4F47' }}>
        <div className="max-w-kb-container mx-auto px-6 flex">
          {CERT_NAV_TABS.map((tab) => {
            const isActive = !!tab.prefix && pathname.startsWith(tab.prefix)
            return (
              <Link
                key={tab.label}
                href={tab.href}
                className={`px-5 py-3.5 text-sm font-medium transition-colors whitespace-nowrap
                  ${isActive
                    ? 'text-white border-b-2 border-kb-yellow'
                    : 'text-white/80 hover:text-white hover:bg-white/10'
                  }`}
              >
                {tab.label}
              </Link>
            )
          })}
        </div>
      </nav>

      {/* 본문 */}
      <div className="max-w-kb-container mx-auto px-6 py-8 flex gap-8">

        {/* 좌측 사이드바 */}
        <aside className="w-52 flex-shrink-0">
          <div className="bg-white border border-kb-border">
            <div className="bg-kb-text px-4 py-3">
              <p className="text-base font-bold text-white">인증센터(기업)</p>
            </div>
            {SIDEBAR_SECTIONS.map((section) => {
              const hasItems = !!section.items?.length
              const isExpanded = !!section.prefix && pathname.startsWith(section.prefix)
              return (
                <div key={section.label}>
                  <div
                    className={`flex items-center justify-between px-4 py-2.5 border-b border-kb-border
                      ${isExpanded ? 'bg-kb-beige' : 'bg-kb-beige-light hover:bg-kb-beige'}`}
                  >
                    <p className="text-base font-bold text-kb-text">{section.label}</p>
                    {hasItems && (
                      <span className="text-[10px] text-kb-text-muted">{isExpanded ? '▲' : '▼'}</span>
                    )}
                  </div>
                  {isExpanded && section.items?.map((item) => (
                    <Link
                      key={item.label}
                      href={item.href}
                      className={`block px-5 py-2.5 text-sm border-b border-kb-border transition-colors
                        ${pathname === item.href
                          ? 'bg-kb-yellow font-bold text-kb-text'
                          : 'text-kb-text-body hover:bg-kb-beige-light'
                        }`}
                    >
                      {item.label}
                    </Link>
                  ))}
                </div>
              )
            })}
          </div>

          {/* 사이드바 하단 프로모션 */}
          <div className="mt-3 border border-kb-border bg-white px-4 py-4">
            <p className="text-[11px] text-kb-text-muted mb-0.5">1,500만명의 선택</p>
            <p className="text-sm font-bold text-kb-text mb-2">AXful인증서 제휴신청</p>
            <div className="flex items-center justify-between">
              <Link href="#" className="text-sm text-kb-blue hover:underline">바로가기 &gt;</Link>
              <div className="w-9 h-9 rounded-full border-2 border-kb-text flex items-center justify-center">
                <span className="text-[9px] font-black text-kb-text">AX</span>
              </div>
            </div>
          </div>
        </aside>

        {/* 우측 메인 */}
        <main className="flex-1 min-w-0">
          {children}
        </main>
      </div>
    </div>
  )
}
