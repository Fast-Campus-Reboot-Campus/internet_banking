'use client'

import Link from 'next/link'
import { useState } from 'react'
import { usePathname } from 'next/navigation'
import ConsultModal from '@/components/layout/ConsultModal'

const MY_MENUS = [
  { label: '전체계좌조회', href: '/inquiry/accounts' },
  { label: '계좌이체', href: '/transfer/account' },
]

function IconHome() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 12L12 4l9 8" />
      <path d="M5 10v9a1 1 0 001 1h4v-5h4v5h4a1 1 0 001-1v-9" />
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

export default function FloatingSidebar() {
  const pathname = usePathname()
  const [fontSize, setFontSize] = useState<'normal' | 'large'>('normal')
  const [showConsult, setShowConsult] = useState(false)
  const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

  if (pathname === '/login' || pathname.startsWith('/biz')) return null

  const itemCls = "w-full flex flex-col items-center py-3 border-b border-kb-border hover:bg-kb-beige transition-colors duration-150 gap-1 text-kb-text-muted hover:text-kb-text"
  const labelCls = "text-[12px] leading-tight text-center"

  return (
    <>
    {showConsult && <ConsultModal onClose={() => setShowConsult(false)} />}
    <div className="fixed right-0 top-[8%] z-50 flex flex-col items-center w-[80px] shadow-xl rounded-l-2xl overflow-hidden bg-white border border-kb-border border-r-0">

      {/* 홈 */}
      <Link
        href="/"
        className="w-full flex flex-col items-center py-3.5 border-b border-kb-border transition-opacity gap-1 text-white hover:opacity-80"
        style={{ backgroundColor: '#0D5C47' }}
      >
        <IconHome />
        <span className="text-[10px] font-semibold tracking-wide">HOME</span>
      </Link>

      {/* MY MENU */}
      <div className="w-full border-b border-kb-border">
        <p className="text-[10px] text-kb-text-muted text-center py-1.5 bg-kb-beige-light font-bold tracking-widest uppercase">My</p>
        {MY_MENUS.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className="block text-kb-text-body text-[12px] text-center py-2 px-1 hover:bg-kb-beige hover:text-kb-text transition-colors duration-150 border-t border-kb-border leading-tight font-medium"
          >
            {item.label}
          </Link>
        ))}
      </div>

      {/* 계산기 */}
      <Link href="/calculator/deposit" className={itemCls}>
        <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <rect x="4" y="2" width="16" height="20" rx="2"/>
          <line x1="8" y1="7" x2="16" y2="7"/>
          <line x1="8" y1="11" x2="10" y2="11"/>
          <line x1="14" y1="11" x2="16" y2="11"/>
          <line x1="8" y1="15" x2="10" y2="15"/>
          <line x1="14" y1="15" x2="16" y2="15"/>
        </svg>
        <span className={labelCls}>예금계산기</span>
      </Link>
      <Link href="/calculator/loan" className={itemCls}>
        <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="9"/>
          <line x1="12" y1="7" x2="12" y2="17"/>
          <line x1="7" y1="12" x2="17" y2="12"/>
        </svg>
        <span className={labelCls}>대출계산기</span>
      </Link>

      {/* 상담신청 */}
      <button onClick={() => setShowConsult(true)} className={itemCls}>
        <IconPhone />
        <span className={labelCls}>상담신청</span>
      </button>

      {/* 고객센터 */}
      <Link href="tel:15880000" className={itemCls}>
        <IconPhone />
        <span className={labelCls}>고객센터</span>
        <span className="text-[10px] leading-tight text-center font-bold" style={{ color: '#0D5C47' }}>1588-0000</span>
      </Link>

      {/* 글자 크기 */}
      <div className="w-full flex items-center justify-center gap-3 py-3 border-b border-kb-border bg-kb-beige-light">
        <button
          onClick={() => { setFontSize('normal'); document.documentElement.style.fontSize = '14px' }}
          className={`w-7 h-7 rounded-full flex items-center justify-center text-[13px] font-bold transition-all duration-150 ${
            fontSize === 'normal'
              ? 'bg-[#0D5C47] text-white shadow-sm'
              : 'text-kb-text-muted hover:text-kb-text'
          }`}
        >가</button>
        <button
          onClick={() => { setFontSize('large'); document.documentElement.style.fontSize = '16px' }}
          className={`w-7 h-7 rounded-full flex items-center justify-center text-[13px] font-bold transition-all duration-150 ${
            fontSize === 'large'
              ? 'bg-[#0D5C47] text-white shadow-sm'
              : 'text-kb-text-muted hover:text-kb-text'
          }`}
        >가</button>
      </div>

      {/* TOP */}
      <button
        onClick={scrollToTop}
        className="w-full flex flex-col items-center py-3 transition-opacity gap-0.5 text-white hover:opacity-80"
        style={{ backgroundColor: '#0D5C47' }}
      >
        <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="18 15 12 9 6 15" />
        </svg>
        <span className="text-[10px] font-semibold tracking-wide">TOP</span>
      </button>
    </div>
    </>
  )
}
