'use client'

import Link from 'next/link'
import { useState, useEffect, useRef } from 'react'

// ── 메인 헤더 ──
function MainHeader() {
  return (
    <header className="bg-white border-b border-kb-border">
      <div className="max-w-kb-container mx-auto px-6 flex items-center justify-between h-[60px]">
        <Link href="/" className="flex items-center gap-3">
          <div className="w-[3px] self-stretch bg-kb-yellow" />
          <div className="flex flex-col leading-none gap-1">
            <span className="text-[22px] font-bold text-kb-text tracking-[0.1em]">AX풀뱅크</span>
            <span className="text-[10px] font-medium text-kb-text-muted tracking-[0.22em] uppercase">AXFULL BANK</span>
          </div>
        </Link>
        <nav className="flex items-center gap-0 text-body">
          <Link href="/personal" className="px-3 text-kb-text font-medium hover:text-kb-yellow transition-colors">개인</Link>
          <Link href="/biz" className="px-3 text-kb-text-muted hover:text-kb-text transition-colors">기업</Link>
          <span className="text-kb-border mx-1">|</span>
          <Link href="/products/deposit" className="px-3 text-kb-text-muted hover:text-kb-text transition-colors">금융상품</Link>
          <span className="text-kb-border mx-1">|</span>
          {['자산관리', '부동산', '퇴직연금', '카드'].map((item) => (
            <Link key={item} href="#" className="px-3 text-kb-text-muted hover:text-kb-text transition-colors">{item}</Link>
          ))}
          <span className="text-kb-border mx-1">|</span>
          <button className="px-3 text-kb-text-muted hover:text-kb-text flex items-center gap-0.5">전체서비스 <span className="text-[10px]">▾</span></button>
          <button className="px-3 text-kb-text-muted hover:text-kb-text flex items-center gap-0.5">GLOBAL <span className="text-[10px]">▾</span></button>
          <button className="ml-2 text-kb-text-muted hover:text-kb-text">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
            </svg>
          </button>
        </nav>
      </div>
    </header>
  )
}

// ── 히어로 슬라이드 ──
const HERO_SLIDES = [
  {
    bg: '#FFFBE6',
    title: 'FIT하게, 내일(My Job)을 그리다',
    highlight: '2026 제1차 AXful굿잡\n우수기업 취업박람회',
    desc: '2026.4.27(월), 10:00 ~ 17:00\n서울 삼성동 코엑스 1층 A홀',
    icon: '👩‍💼',
  },
  {
    bg: '#F0F4FF',
    title: '언제 어디서나 편리하게',
    highlight: 'AXful뱅킹으로\n스마트한 금융생활',
    desc: '개인·기업 고객 모두 이용 가능\n24시간 365일 금융서비스',
    icon: '📱',
  },
  {
    bg: '#F5FFF8',
    title: '안전하고 빠른 인증',
    highlight: 'AX풀뱅크인증서로\n간편하게 로그인',
    desc: '공동인증서보다 편리한\nAXful 전용 인증서',
    icon: '🔐',
  },
]

function HeroSection() {
  const [current, setCurrent] = useState(0)
  const [paused, setPaused] = useState(false)
  const [showCertMenu, setShowCertMenu] = useState(false)
  const certMenuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!showCertMenu) return
    function onMouseDown(e: MouseEvent) {
      if (certMenuRef.current && !certMenuRef.current.contains(e.target as Node)) {
        setShowCertMenu(false)
      }
    }
    document.addEventListener('mousedown', onMouseDown)
    return () => document.removeEventListener('mousedown', onMouseDown)
  }, [showCertMenu])

  useEffect(() => {
    if (paused) return
    const t = setInterval(() => setCurrent((c) => (c + 1) % HERO_SLIDES.length), 4500)
    return () => clearInterval(t)
  }, [paused])

  const slide = HERO_SLIDES[current]

  return (
    <section className="relative" style={{ backgroundColor: slide.bg, transition: 'background-color 0.5s' }}>
      <div className="max-w-kb-container mx-auto px-6 py-8 flex items-start gap-4">
        {/* 로그인/인증센터 */}
        <div className="flex gap-2 pt-2 flex-shrink-0">
          <Link href="/login"
            className="px-4 py-1.5 border border-kb-border text-base text-kb-text-body bg-white hover:bg-kb-beige-light rounded-lg transition-colors">
            로그인
          </Link>
          <div className="relative" ref={certMenuRef}>
            <button
              onClick={() => setShowCertMenu((v) => !v)}
              className="px-4 py-1.5 border border-kb-border text-base text-kb-text-body bg-white hover:bg-kb-beige-light rounded-lg transition-colors"
            >
              인증센터
            </button>
            {showCertMenu && (
              <div className="absolute left-0 top-full mt-1 bg-white border border-kb-border shadow-md rounded-lg z-[200] flex gap-1 p-2">
                <Link
                  href="/cert"
                  onClick={() => setShowCertMenu(false)}
                  className="px-3 py-1 text-sm text-kb-text hover:bg-kb-yellow transition-colors whitespace-nowrap rounded"
                >
                  개인
                </Link>
                <Link
                  href="/cert-biz"
                  onClick={() => setShowCertMenu(false)}
                  className="px-3 py-1 text-sm text-kb-text hover:bg-kb-yellow transition-colors whitespace-nowrap rounded"
                >
                  기업
                </Link>
              </div>
            )}
          </div>
        </div>

        {/* 히어로 콘텐츠 */}
        <div className="flex-1 flex items-center justify-between py-16">
          <div>
            <p className="text-base text-kb-text-body mb-2">{slide.title}</p>
            <h2 className="text-[40px] font-bold text-kb-text mb-4 whitespace-pre-line leading-snug">
              {slide.highlight}
            </h2>
            <p className="text-base text-kb-text-body leading-relaxed whitespace-pre-line mb-5">{slide.desc}</p>
            <Link href="#" className="text-base text-kb-text-body underline hover:text-kb-text">바로가기</Link>
          </div>
          <div className="w-60 h-44 flex items-center justify-center text-8xl opacity-80">
            {slide.icon}
          </div>
        </div>

        {/* AX풀뱅크인증서 카드 */}
        <div className="flex-shrink-0 w-36 bg-white border border-kb-border rounded-2xl p-6 flex flex-col items-center gap-3 mt-6 shadow-sm">
          <div className="w-12 h-12 bg-kb-yellow rounded-full flex items-center justify-center">
            <span className="text-xl font-bold text-kb-text">AXful</span>
          </div>
          <p className="text-sm text-kb-text text-center font-medium">AX풀뱅크<br />인증서</p>
          <Link href="#" className="text-sm text-kb-text-muted hover:text-kb-text hover:underline">바로가기 &gt;</Link>
        </div>
      </div>

      {/* 컨트롤 */}
      <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-2">
        <button onClick={() => setCurrent((c) => (c - 1 + HERO_SLIDES.length) % HERO_SLIDES.length)}
          className="text-kb-text-muted hover:text-kb-text">‹</button>
        {HERO_SLIDES.map((_, i) => (
          <button key={i} onClick={() => setCurrent(i)}
            className={`rounded-full transition-all ${i === current ? 'w-5 h-2 bg-kb-text' : 'w-2 h-2 bg-kb-text/30'}`} />
        ))}
        <button onClick={() => setCurrent((c) => (c + 1) % HERO_SLIDES.length)}
          className="text-kb-text-muted hover:text-kb-text">›</button>
        <button onClick={() => setPaused(!paused)} className="text-kb-text-muted hover:text-kb-text text-xs ml-1">
          {paused ? '▶' : '⏸'}
        </button>
      </div>
    </section>
  )
}

// ── 퀵메뉴 바 ──
const QUICK_MENUS = [
  { label: '전체계좌조회', href: '/personal', yellow: true },
  { label: '계좌이체', href: '/personal', yellow: true },
  { label: '빠른조회', href: '/personal', yellow: true },
  { label: '보안센터', href: '#', yellow: false },
  { label: '고객우대제도', href: '#', yellow: false },
  { label: '소비자보호', href: '#', yellow: false },
  { label: '상담/예약', href: '#', yellow: false },
  { label: '상품공시실', href: '#', yellow: false },
]

// ── 새소식/이벤트 ──
const NEWS = [
  { title: '「1분 브리핑 증시 안내」 서비스 출시 안내', date: '04.28' },
  { title: '인터넷뱅킹 「AXful금융그룹 통합조회」 서비...', date: '04.27' },
  { title: '「금융투자상품 투자자문서비스 계약서(약...', date: '04.23' },
]
const EVENTS = [
  { title: '케이봇쌤 포트폴리오 고객 챌린지 이...', period: '05.04 ~ 06.30' },
  { title: '소득공제부터 복리이자까지!「노란우산...', period: '04.20 ~ 11.30' },
  { title: 'AX풀뱅크 X AXful증권거래 한번에 ABLE...', period: '04.27 ~ 05.31' },
]

// ── 서비스 카드 ──
const SERVICES = [
  { badge: '서비스', title: 'AXful에스크로 이체', desc: '안전한 상거래를 위해 매매보호\n서비스를 이용하세요' },
  { badge: '서비스', title: '건강한 자산관리 AXful마이데이터', desc: '각 기관에 흩어진 나의 자산을\n한 곳에 모아서 관리해보세요.' },
  { badge: '서비스', title: '이동통신서비스 AXful M', desc: 'AX풀뱅크의 혁신금융서비스로\n금융권 최초로 제공하는 이동통신서비스' },
]

// ── 금융상품 아이콘 ──
function IconDeposit() {
  return (
    <svg viewBox="0 0 48 48" fill="none" className="w-14 h-14" stroke="#222222" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <ellipse cx="22" cy="30" rx="14" ry="11" fill="#5BC9A8" stroke="#222222" strokeWidth="1.6"/>
      <circle cx="15" cy="27" r="1.5" fill="#222222" stroke="none"/>
      <ellipse cx="26" cy="20" rx="3.5" ry="2.5" fill="white" stroke="#222222" strokeWidth="1.4"/>
      <line x1="22" y1="19" x2="22" y2="21" stroke="#222222" strokeWidth="2"/>
      <path d="M36 28C40 27 41 24 39 22"/>
      <ellipse cx="9" cy="32" rx="2.5" ry="2" fill="white" stroke="#222222" strokeWidth="1.2"/>
      <line x1="14" y1="40" x2="12" y2="44"/>
      <line x1="20" y1="41" x2="19" y2="44"/>
      <line x1="26" y1="41" x2="25" y2="44"/>
      <line x1="32" y1="40" x2="34" y2="44"/>
    </svg>
  )
}
function IconFund() {
  return (
    <svg viewBox="0 0 48 48" fill="none" className="w-14 h-14" stroke="#222222" strokeWidth="1.5" strokeLinecap="round">
      <rect x="7" y="28" width="9" height="14" rx="1" fill="#DDDDDD" stroke="#222222"/>
      <rect x="19" y="20" width="9" height="22" rx="1" fill="#5BC9A8" stroke="#222222"/>
      <rect x="31" y="12" width="9" height="30" rx="1" fill="#DDDDDD" stroke="#222222"/>
      <line x1="5" y1="42" x2="43" y2="42" stroke="#222222" strokeWidth="1.5"/>
    </svg>
  )
}
function IconLoan() {
  return (
    <svg viewBox="0 0 48 48" fill="none" className="w-14 h-14" stroke="#222222" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <path d="M15 22C9 28 9 37 9 38C9 43 16 46 24 46C32 46 39 43 39 38C39 37 39 28 33 22Z" fill="#5BC9A8" stroke="#222222"/>
      <path d="M19 22C19 18 21 16 24 16C27 16 29 18 29 22"/>
      <path d="M19 9L29 9Q31 15 24 15Q17 15 19 9Z" fill="white" stroke="#222222" strokeWidth="1.4"/>
      <text x="24" y="37" textAnchor="middle" fontSize="12" fontWeight="bold" stroke="none" fill="white" fontFamily="sans-serif">W</text>
    </svg>
  )
}
function IconTrust() {
  return (
    <svg viewBox="0 0 48 48" fill="none" className="w-14 h-14" stroke="#222222" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <rect x="6" y="8" width="36" height="30" rx="2" fill="#F0F0F0" stroke="#222222"/>
      <circle cx="24" cy="23" r="10" fill="#5BC9A8" stroke="#222222"/>
      <circle cx="24" cy="23" r="3.5" fill="white" stroke="#222222" strokeWidth="1.3"/>
      <line x1="24" y1="13" x2="24" y2="17"/>
      <line x1="24" y1="29" x2="24" y2="33"/>
      <line x1="14" y1="23" x2="18" y2="23"/>
      <line x1="30" y1="23" x2="34" y2="23"/>
      <line x1="14" y1="38" x2="14" y2="42" strokeWidth="2"/>
      <line x1="34" y1="38" x2="34" y2="42" strokeWidth="2"/>
    </svg>
  )
}
function IconISA() {
  return (
    <svg viewBox="0 0 48 48" fill="none" className="w-14 h-14" stroke="#222222" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <rect x="5" y="6" width="38" height="26" rx="2" fill="white" stroke="#222222"/>
      <rect x="8" y="9" width="32" height="20" rx="1" fill="#F8F8F8" stroke="#222222" strokeWidth="1"/>
      <rect x="12" y="23" width="5" height="5" fill="#DDDDDD" stroke="none"/>
      <rect x="20" y="19" width="5" height="9" fill="#5BC9A8" stroke="none"/>
      <rect x="28" y="15" width="5" height="13" fill="#DDDDDD" stroke="none"/>
      <line x1="19" y1="32" x2="29" y2="32" strokeWidth="1.5"/>
      <line x1="24" y1="32" x2="24" y2="38"/>
      <line x1="16" y1="38" x2="32" y2="38" strokeWidth="2"/>
    </svg>
  )
}
function IconInsurance() {
  return (
    <svg viewBox="0 0 48 48" fill="none" className="w-14 h-14" stroke="#222222" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <path d="M6 24C6 14 14 7 24 7C34 7 42 14 42 24Z" fill="#5BC9A8" stroke="#222222"/>
      <line x1="24" y1="7" x2="24" y2="24" stroke="white" strokeWidth="1" opacity="0.5"/>
      <line x1="6" y1="24" x2="42" y2="24" stroke="white" strokeWidth="1" opacity="0.5"/>
      <line x1="24" y1="24" x2="24" y2="40"/>
      <path d="M24 40C24 43 21 45 19 43" fill="none"/>
    </svg>
  )
}
function IconGold() {
  return (
    <svg viewBox="0 0 48 48" fill="none" className="w-14 h-14" stroke="#222222" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <path d="M10 30L14 20H34L38 30Z" fill="#5BC9A8" stroke="#222222"/>
      <rect x="8" y="30" width="32" height="10" rx="1" fill="#5BC9A8" stroke="#222222"/>
      <line x1="14" y1="25" x2="34" y2="25" stroke="white" strokeWidth="1" opacity="0.6"/>
    </svg>
  )
}
function IconForeignDeposit() {
  return (
    <svg viewBox="0 0 48 48" fill="none" className="w-14 h-14" stroke="#222222" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <rect x="5" y="12" width="38" height="26" rx="2" fill="white" stroke="#222222"/>
      <path d="M5 12L24 26L43 12" stroke="#222222" strokeWidth="1.4"/>
      <circle cx="34" cy="30" r="8" fill="#5BC9A8" stroke="#222222"/>
      <text x="34" y="34" textAnchor="middle" fontSize="11" fontWeight="bold" stroke="none" fill="white" fontFamily="sans-serif">$</text>
    </svg>
  )
}

// ── 금융상품 ──
const PRODUCTS = [
  { label: '예금',    icon: <IconDeposit />,        href: '/products/deposit' },
  { label: '펀드',    icon: <IconFund />,            href: '/products/fund' },
  { label: '대출',    icon: <IconLoan />,            href: '/products/loan' },
  { label: '신탁',    icon: <IconTrust />,           href: '/products/trust' },
  { label: 'ISA',    icon: <IconISA />,             href: '/products/isa' },
  { label: '보험/공제', icon: <IconInsurance />,    href: '/products/insurance' },
  { label: '골드',    icon: <IconGold />,            href: '/products/gold' },
  { label: '외화예금', icon: <IconForeignDeposit />, href: '/products/fx-deposit' },
]

// ── KB 앱 ──
const APPS = [
  { label: 'AX풀뱅킹', bg: '#5BC9A8', text: 'Axful' },
  { label: 'AXful기업뱅킹', bg: '#4A4A4A', text: 'Axful' },
  { label: 'AXful부동산', bg: '#F5F5F5', text: 'Axful\n부동산' },
  { label: 'AXful똑똑', bg: '#00BCD4', text: 'AXful' },
]

export default function MainHomePage() {
  return (
    <>
      <MainHeader />
      <main>
        {/* 히어로 */}
        <HeroSection />

        {/* 퀵메뉴 바 */}
        <section>
          <div className="max-w-kb-container mx-auto px-6">
            <div className="flex">
              {QUICK_MENUS.map((menu) => (
                <Link key={menu.label} href={menu.href}
                  className={`flex-1 py-5 text-center text-base font-semibold transition-colors duration-kb
                    ${menu.yellow
                      ? 'bg-kb-yellow text-kb-text hover:bg-kb-yellow-dark'
                      : 'text-kb-text hover:bg-kb-beige-light border-l border-kb-border'
                    }`}>
                  {menu.label}
                </Link>
              ))}
            </div>
          </div>
        </section>

        {/* 새소식 / 이벤트 */}
        <section className="py-12">
          <div className="max-w-kb-container mx-auto px-6 flex gap-16">
            {/* 새소식 */}
            <div className="flex-1">
              <div className="flex items-center justify-between mb-5">
                <h2 className="text-xl font-bold text-kb-text">새소식</h2>
                <Link href="#" className="text-sm text-kb-text-muted hover:text-kb-text hover:underline flex items-center gap-0.5">바로가기 ›</Link>
              </div>
              <ul className="space-y-3">
                {NEWS.map((item, i) => (
                  <li key={i} className="flex items-center justify-between gap-4">
                    <Link href="#" className="text-base text-kb-text-body hover:underline truncate flex-1">{item.title}</Link>
                    <span className="text-sm text-kb-text-body flex-shrink-0">{item.date}</span>
                  </li>
                ))}
              </ul>
            </div>
            <div className="w-px bg-kb-border" />
            {/* 이벤트 */}
            <div className="flex-1">
              <div className="flex items-center justify-between mb-5">
                <h2 className="text-xl font-bold text-kb-text">이벤트</h2>
                <Link href="#" className="text-sm text-kb-text-muted hover:text-kb-text hover:underline flex items-center gap-0.5">바로가기 ›</Link>
              </div>
              <ul className="space-y-3">
                {EVENTS.map((item, i) => (
                  <li key={i} className="flex items-center justify-between gap-4">
                    <Link href="#" className="text-base text-kb-text-body hover:underline truncate flex-1">{item.title}</Link>
                    <span className="text-sm text-kb-text-body flex-shrink-0">{item.period}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </section>

        {/* 서비스 카드 */}
        <section className="py-12">
          <div className="max-w-kb-container mx-auto px-6">
            <div className="grid grid-cols-3 gap-6">
              {SERVICES.map((s) => (
                <div key={s.title} className="border border-kb-border-dark rounded-xl p-8 hover:shadow-md transition-shadow">
                  <span className="inline-block text-sm font-bold text-white bg-[#6C5CE7] px-2 py-0.5 rounded-md mb-4">
                    {s.badge}
                  </span>
                  <h3 className="text-lg font-bold text-kb-text mb-3">{s.title}</h3>
                  <p className="text-sm text-kb-text-muted leading-relaxed whitespace-pre-line">{s.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* 금융상품 */}
        <section className="py-12">
          <div className="max-w-kb-container mx-auto px-6 flex items-start gap-8">
            <div className="flex-shrink-0 w-36">
              <h2 className="text-xl font-bold text-kb-text">금융상품</h2>
              <Link href="/products/deposit" className="text-sm text-kb-text-muted hover:text-kb-text hover:underline flex items-center gap-0.5 mt-1">바로가기 ›</Link>
            </div>
            <div className="flex flex-1 justify-evenly">
              {PRODUCTS.map((p) => (
                <Link key={p.label} href={p.href}
                  className="flex flex-col items-center gap-2 hover:opacity-80 transition-opacity">
                  {p.icon}
                  <span className="text-sm text-kb-text">{p.label}</span>
                </Link>
              ))}
            </div>
          </div>
        </section>

        {/* 금융사고예방 */}
        <section className="py-12 bg-kb-beige-light">
          <div className="max-w-kb-container mx-auto px-6 flex gap-8">
            <h2 className="text-xl font-bold text-kb-text flex-shrink-0 w-36">금융사고예방</h2>
            <div className="flex-1 grid grid-cols-3 gap-10">
              {[
                { title: '전자금융사기예방 서비스', desc: '각종 금융사기수법에 한층 강화된 다양한 전자금융사기예방 서비스로 안전한 인터넷뱅킹 사용이 가능합니다.' },
                { title: '통장(카드) 매매·양도는 불법', desc: '고객님의 자산을 보호하고 금융사기를 예방하기 위한 최선의 방법은 대포통장 근절입니다.' },
                { title: '사진촬영·QR스캔 절대금지', desc: '타인이 OTP/보안카드 번호를 요구(2개 초과) 하는 경우는 금융사기이니 절대 응하지 마십시오.' },
              ].map((item) => (
                <div key={item.title}>
                  <p className="text-base font-bold text-kb-text mb-2">{item.title}</p>
                  <p className="text-sm text-kb-text-body leading-relaxed">{item.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* 소비자권익보호 */}
        <section className="py-10 border-t border-kb-border bg-kb-beige-light">
          <div className="max-w-kb-container mx-auto px-6 flex items-center gap-8">
            <h2 className="text-xl font-bold text-kb-text flex-shrink-0 w-36">소비자권익보호</h2>
            <div className="flex gap-8">
              {['금융감독원 바로가기', '미수령주식 찾기', '비교조회서비스'].map((item) => (
                <Link key={item} href="#"
                  className="text-base text-kb-text hover:text-kb-yellow-dark flex items-center gap-1 transition-colors">
                  {item} <span className="text-kb-text-muted">›</span>
                </Link>
              ))}
            </div>
          </div>
        </section>

        {/* KB APP */}
        <section className="py-12">
          <div className="max-w-kb-container mx-auto px-6 flex items-center gap-8">
            <h2 className="text-xl font-bold text-kb-text flex-shrink-0 w-36 whitespace-nowrap">AX풀뱅크 APP</h2>
            <div className="flex flex-1 justify-evenly">
              {APPS.map((app) => (
                <div key={app.label} className="flex flex-col items-center gap-2">
                  <div className="w-16 h-16 rounded-3xl flex items-center justify-center text-white font-bold"
                    style={{ backgroundColor: app.bg, color: app.bg === '#F5F5F5' ? '#333' : 'white' }}>
                    <span className="text-center whitespace-pre-line text-sm leading-tight">{app.text}</span>
                  </div>
                  <span className="text-sm text-kb-text">{app.label}</span>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>

      {/* 푸터 */}
      <footer className="border-t border-kb-border bg-white py-10">
        <div className="max-w-kb-container mx-auto px-6">
          <p className="text-sm text-kb-text-muted">Copyright AXful Bank. All Rights Reserved.</p>
        </div>
      </footer>
    </>
  )
}
