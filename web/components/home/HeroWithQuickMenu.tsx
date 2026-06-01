'use client'

import React, { useState } from 'react'
import Link from 'next/link'
import HeroCarousel, { HERO_SLIDES } from './HeroCarousel'

function IconInquiry() {
  return (
    <svg viewBox="0 0 56 56" fill="none" className="w-14 h-14" strokeLinecap="round" strokeLinejoin="round">
      <rect x="8" y="4" width="32" height="26" rx="2" fill="white" stroke="currentColor" strokeWidth="2.2" />
      <rect x="12" y="8" width="24" height="18" rx="1" fill="white" stroke="currentColor" strokeWidth="1.3" />
      <line x1="16" y1="13" x2="32" y2="13" stroke="currentColor" strokeWidth="1.5" />
      <line x1="16" y1="18" x2="26" y2="18" stroke="currentColor" strokeWidth="1.5" />
      <rect x="4" y="32" width="40" height="10" rx="1" fill="white" stroke="currentColor" strokeWidth="2" />
      <rect x="16" y="35" width="16" height="4" rx="1" fill="none" stroke="currentColor" strokeWidth="1.3" />
      <circle cx="37" cy="37" r="10" fill="#5BC9A8" stroke="currentColor" strokeWidth="2.2" />
      <line x1="44.1" y1="44.1" x2="51" y2="51" stroke="currentColor" strokeWidth="2.8" strokeLinecap="round" />
    </svg>
  )
}

function IconTransfer() {
  return (
    <svg viewBox="0 0 56 56" fill="none" className="w-14 h-14" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="22" cy="28" r="18" fill="white" stroke="currentColor" strokeWidth="2.2" />
      <circle cx="22" cy="28" r="13" fill="#5BC9A8" stroke="none" />
      <text x="22" y="33" textAnchor="middle" fontSize="13" fontWeight="bold" stroke="none" fill="currentColor" fontFamily="sans-serif">W</text>
      <line x1="42" y1="28" x2="54" y2="28" stroke="currentColor" strokeWidth="2.2" />
      <polyline points="50,23 54,28 50,33" stroke="currentColor" strokeWidth="2.2" fill="none" />
    </svg>
  )
}

function IconAutoTransfer() {
  return (
    <svg viewBox="0 0 56 56" fill="none" className="w-14 h-14" strokeLinecap="round" strokeLinejoin="round">
      {/* 메인: 시계 */}
      <circle cx="22" cy="27" r="18" fill="white" stroke="currentColor" strokeWidth="2.2" />
      {/* 시침 (10시 방향) */}
      <line x1="22" y1="27" x2="14" y2="18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      {/* 분침 (2시 방향) */}
      <line x1="22" y1="27" x2="30" y2="19" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      {/* 중심점 */}
      <circle cx="22" cy="27" r="1.5" fill="currentColor" stroke="none" />
      {/* 배지: 계좌이체 W 원 */}
      <circle cx="39" cy="43" r="10" fill="white" stroke="currentColor" strokeWidth="1.8" />
      <circle cx="39" cy="43" r="7" fill="#5BC9A8" stroke="none" />
      <text x="39" y="47" textAnchor="middle" fontSize="9" fontWeight="bold" stroke="none" fill="currentColor" fontFamily="sans-serif">W</text>
    </svg>
  )
}

function IconAsset() {
  return (
    <svg viewBox="0 0 56 56" fill="none" className="w-14 h-14" strokeLinecap="round" strokeLinejoin="round">
      {/* 졸업모 - 민트 */}
      <path d="M14 23 l14 -7 l14 7 -14 7 Z" fill="#5BC9A8" stroke="currentColor" strokeWidth="1.5" />
      <line x1="42" y1="23" x2="42" y2="31" stroke="currentColor" strokeWidth="2" />
      <circle cx="42" cy="33" r="2" fill="currentColor" stroke="none" />
      {/* 올빼미 몸통 - 흰색 */}
      <ellipse cx="28" cy="40" rx="12" ry="9" fill="white" stroke="currentColor" strokeWidth="2" />
      {/* 안경 */}
      <circle cx="23" cy="38" r="4" fill="white" stroke="currentColor" strokeWidth="1.5" />
      <circle cx="33" cy="38" r="4" fill="white" stroke="currentColor" strokeWidth="1.5" />
      <line x1="27" y1="38" x2="29" y2="38" stroke="currentColor" strokeWidth="1.5" />
      <line x1="17" y1="38" x2="19" y2="38" stroke="currentColor" strokeWidth="1.5" />
      <line x1="37" y1="38" x2="39" y2="38" stroke="currentColor" strokeWidth="1.5" />
      {/* 눈동자 */}
      <circle cx="23" cy="38" r="1.5" fill="currentColor" stroke="none" />
      <circle cx="33" cy="38" r="1.5" fill="currentColor" stroke="none" />
      {/* 부리 */}
      <path d="M26.5 43 L28 45.5 L29.5 43 Z" fill="currentColor" stroke="none" />
    </svg>
  )
}

const QUICK_MENUS = [
  { label: '전체계좌조회', href: '/inquiry/accounts', Icon: IconInquiry },
  { label: '계좌이체', href: '/transfer/account', Icon: IconTransfer },
  { label: '자동이체', href: '/transfer/auto', Icon: IconAutoTransfer },
  { label: '자산관리', href: '/products', Icon: IconAsset },
]

export default function HeroWithQuickMenu() {
  const [current, setCurrent] = useState(0)
  const [paused, setPaused] = useState(false)
  const topColor = HERO_SLIDES[current]?.bg ?? HERO_SLIDES[0].bg

  return (
    <>
      <HeroCarousel
        current={current}
        paused={paused}
        onChangeTo={setCurrent}
        onPausedChange={setPaused}
      />

      {/* stacked div로 gradient 구현 — backgroundColor은 transition-colors 적용 가능 */}
      <section className="py-5 relative">
        <div
          className="absolute inset-x-0 top-0 bottom-1/2"
          style={{ backgroundColor: topColor }}
        />
        <div className="absolute inset-x-0 top-1/2 bottom-0" style={{ backgroundColor: '#EDE0D4' }} />
        <div className="relative z-10 max-w-kb-container mx-auto px-6">
          <div className="bg-white overflow-hidden flex">
            {QUICK_MENUS.map((menu, idx) => (
              <React.Fragment key={menu.href}>
                {idx > 0 && (
                  <div className="self-center h-12 w-px bg-[#E0E0E0] flex-shrink-0" />
                )}
                <Link
                  href={menu.href}
                  className="flex-1 flex items-center gap-4 py-[38px] px-6"
                >
                  <menu.Icon />
                  <div>
                    <p className="text-lg text-kb-text-body">{menu.label}</p>
                    <p className="text-sm text-kb-text font-medium mt-0.5">
                      바로가기 &gt;
                    </p>
                  </div>
                </Link>
              </React.Fragment>
            ))}
          </div>
        </div>
      </section>
    </>
  )
}
