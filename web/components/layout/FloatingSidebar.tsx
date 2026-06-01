'use client'

import Link from 'next/link'
import { useState } from 'react'
import { usePathname } from 'next/navigation'
import ConsultModal from '@/components/layout/ConsultModal'

const MY_MENUS = [
  { label: '전체계좌조회', href: '/inquiry/accounts' },
  { label: '계좌이체', href: '/transfer/account' },
  { label: '자동이체관리', href: '/transfer/auto' },
  { label: '메뉴설정', href: '#' },
]

function IconHome() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 12L12 4l9 8" />
      <path d="M5 10v9a1 1 0 001 1h4v-5h4v5h4a1 1 0 001-1v-9" />
    </svg>
  )
}

function IconCart() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z" />
      <line x1="3" y1="6" x2="21" y2="6" />
      <path d="M16 10a4 4 0 01-8 0" />
    </svg>
  )
}

function IconClock() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="9" />
      <polyline points="12 7 12 12 15 15" />
    </svg>
  )
}

function IconPhone() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72c.127.96.361 1.903.7 2.81a2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0122 16.92z" />
    </svg>
  )
}

function IconPin() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 10c0 7-9 13-9 13S3 17 3 10a9 9 0 0118 0z" />
      <circle cx="12" cy="10" r="3" />
    </svg>
  )
}

export default function FloatingSidebar() {
  const pathname = usePathname()
  const [fontSize, setFontSize] = useState<'normal' | 'large'>('normal')
  const [showConsult, setShowConsult] = useState(false)
  const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

  if (pathname === '/login') return null

  const itemCls = "w-full flex flex-col items-center py-3 border-b border-kb-border hover:bg-kb-beige transition-colors gap-1 text-kb-text-muted hover:text-kb-text"
  const labelCls = "text-[11px] leading-tight text-center"

  return (
    <>
    {showConsult && <ConsultModal onClose={() => setShowConsult(false)} />}
    <div className="fixed right-0 top-[8%] z-50 flex flex-col items-center w-[80px] shadow-lg rounded-l-xl overflow-hidden bg-white border border-kb-border border-r-0">

      {/* 홈 */}
      <Link href="/personal" className="w-full flex flex-col items-center py-3 border-b border-kb-border transition-colors gap-1 text-white hover:opacity-90" style={{ backgroundColor: '#6B4C35' }}>
        <IconHome />
      </Link>

      {/* 장바구니 */}
      <Link href="#" className={itemCls}>
        <IconCart />
        <span className={labelCls}>장바구니</span>
      </Link>

      {/* 최근본상품 */}
      <Link href="#" className={itemCls}>
        <IconClock />
        <span className={labelCls}>최근본상품</span>
      </Link>

      {/* MY MENU */}
      <div className="w-full border-b border-kb-border">
        <p className="text-[11px] text-kb-text-muted text-center py-1.5 bg-kb-beige-light font-semibold tracking-wide">MY MENU</p>
        {MY_MENUS.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className="block text-kb-text-body text-[11px] text-center py-2 px-1 hover:bg-kb-beige hover:text-kb-text transition-colors border-t border-kb-border leading-tight"
          >
            {item.label}
          </Link>
        ))}
      </div>

      {/* 상담신청 */}
      <button onClick={() => setShowConsult(true)} className={itemCls}>
        <IconPhone />
        <span className={labelCls}>상담신청</span>
      </button>

      {/* 고객센터 */}
      <Link href="tel:15880000" className={itemCls}>
        <IconPhone />
        <span className={labelCls}>고객센터</span>
        <span className="text-[11px] leading-tight text-center font-medium" style={{ color: '#2D6A4F' }}>1588-0000</span>
      </Link>

      {/* 지점찾기 */}
      <Link href="#" className={itemCls}>
        <IconPin />
        <span className={labelCls}>지점찾기</span>
      </Link>

      {/* 글자 크기 */}
      <div className="w-full flex items-center justify-center gap-2 py-3 border-b border-kb-border">
        <button
          onClick={() => { setFontSize('normal'); document.documentElement.style.fontSize = '14px' }}
          className={`text-[12px] font-medium transition-colors ${fontSize === 'normal' ? 'text-[#2D6A4F] underline' : 'text-kb-text-muted hover:text-kb-text'}`}
        >가</button>
        <button
          onClick={() => { setFontSize('large'); document.documentElement.style.fontSize = '16px' }}
          className={`text-[15px] font-medium transition-colors ${fontSize === 'large' ? 'text-[#2D6A4F] underline' : 'text-kb-text-muted hover:text-kb-text'}`}
        >가</button>
      </div>

      {/* TOP */}
      <button
        onClick={scrollToTop}
        className="w-full flex flex-col items-center py-3 transition-colors gap-0.5 text-white hover:opacity-90"
        style={{ backgroundColor: '#6B4C35' }}
      >
        <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="18 15 12 9 6 15" />
        </svg>
        <span className="text-[11px] leading-tight">TOP</span>
      </button>
    </div>
    </>
  )
}
