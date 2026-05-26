'use client'

import { useState, type ReactNode } from 'react'
import Link from 'next/link'

type Product = {
  badge: string
  badgeColor: string
  icon: ReactNode
  subLabel: string
  name: string
  valueLabel: string
  value: string
  href: string
}

function PiggyIcon() {
  return (
    <svg viewBox="0 0 60 52" fill="none" className="w-16 h-14 mx-auto" stroke="#BBBBBB" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <ellipse cx="28" cy="32" rx="17" ry="13" />
      <circle cx="21" cy="27" r="1.8" fill="#BBBBBB" stroke="none" />
      <path d="M45 30 C49 30 51 27 51 25 C51 23 49 23 45 24" />
      <path d="M28 19 C28 17 30 16 33 16 C35 16 36 18 34 19" />
      <path d="M20 44 L18 50" />
      <path d="M36 44 L38 50" />
      <path d="M11 33 Q13 39 16 39" />
    </svg>
  )
}

function MoneyBagIcon() {
  return (
    <svg viewBox="0 0 60 60" fill="none" className="w-16 h-16 mx-auto" stroke="#BBBBBB" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 26 C17 31 15 36 15 40 C15 49 22 54 30 54 C38 54 45 49 45 40 C45 36 43 31 39 26 Z" />
      <path d="M25 26 C25 22 27 20 30 20 C33 20 35 22 35 26" />
      <path d="M25 13 L35 13 Q37 20 30 20 Q23 20 25 13 Z" />
      <text x="30" y="43" textAnchor="middle" fontSize="12" fontWeight="bold" stroke="none" fill="#BBBBBB">W</text>
    </svg>
  )
}


const PRODUCT_SLIDES: Product[][] = [
  [
    {
      badge: '예금',
      badgeColor: '#6B4C35',
      icon: <PiggyIcon />,
      subLabel: 'AX풀뱅크의 대표 정기예금',
      name: 'AX풀뱅크 Star 정기예금',
      valueLabel: '1~36개월,',
      value: '연 2.4% ~ 2.9%',
      href: '/products/deposit',
    },
    {
      badge: '신용대출',
      badgeColor: '#2D6A4F',
      icon: <MoneyBagIcon />,
      subLabel: '직장인이라면',
      name: 'AX풀뱅크 직장인든든 신용대출',
      valueLabel: '최고',
      value: '3억원',
      href: '/products/loan',
    },
  ],
]

export default function ProductShowcase() {
  const [slide, setSlide] = useState(0)
  const products = PRODUCT_SLIDES[slide]

  return (
    <section className="py-6" style={{ backgroundColor: '#EDE0D4' }}>
      <div className="max-w-kb-container mx-auto px-6">
        <div className="grid grid-cols-3 gap-4" style={{ minHeight: '300px' }}>

          {/* 좌측: AX풀뱅크 대표상품 */}
          <div
            className="p-6 flex flex-col items-center text-center"
            style={{ backgroundColor: '#5BC9A8' }}
          >
            <p className="text-sm text-black/70 mb-2 mt-2">고객님들의 행복한 삶을 위한</p>
            <h2 className="text-3xl font-bold text-kb-text leading-tight">
              AX풀뱅크 대표상품
            </h2>
            <div className="flex-1" />
            <div className="mb-0">
              <p className="text-sm text-black/60">⊙ 2026.05.24 기준</p>
              <p className="text-sm text-black/60">세금공제전, 우대금리포함</p>
            </div>
            <div className="flex-1" />
            <Link
              href="/products"
              className="text-base font-bold text-kb-text border-b-2 border-black pb-0.5 inline-flex items-center gap-1 mt-6"
            >
              바로가기 →
            </Link>
            <div className="mt-4" />
          </div>

          {/* 우측: 상품 카드 2개 */}
          {products.map((product) => (
            <div
              key={product.name}
              className="border border-[#E0E0E0] bg-white relative pt-8 px-5 pb-5 flex flex-col hover:bg-[#F9F9F9] transition-colors"
            >
              {/* 뱃지 + 수평선 */}
              <div className="absolute top-0 left-0 right-0 flex items-center" style={{ transform: 'translateY(-50%)' }}>
                <div className="ml-5">
                  <span
                    className="px-4 py-1.5 text-sm text-white font-bold rounded-full whitespace-nowrap"
                    style={{ backgroundColor: product.badgeColor }}
                  >
                    {product.badge}
                  </span>
                </div>
                <div className="flex-1 h-px mx-3" style={{ backgroundColor: product.badgeColor }} />
              </div>

              {/* 아이콘 */}
              <div className="flex justify-center items-center mb-3 h-12">{product.icon}</div>

              {/* 서브 라벨 */}
              <p className="text-sm text-kb-text-muted text-center mb-1">{product.subLabel}</p>

              {/* 상품명 */}
              <p className="text-xl text-kb-text text-center mb-4 leading-snug min-h-[48px]">{product.name}</p>

              <div className="flex-1" />
              {/* 값 라벨 + 값 */}
              <p className="text-base text-kb-text-muted text-center">{product.valueLabel}</p>
              <p className="text-3xl font-bold text-kb-text text-center my-2">{product.value}</p>

              {/* 바로가기 버튼 */}
              <Link
                href={product.href}
                className="w-2/3 mx-auto mt-3 mb-2 py-2 rounded-full text-white text-base font-medium text-center block hover:opacity-90 transition-opacity"
                style={{ backgroundColor: product.badgeColor }}
              >
                바로가기
              </Link>
            </div>
          ))}
        </div>

        {/* 슬라이드 컨트롤 */}
        <div className="flex justify-center items-center gap-2 mt-6">
          <button
            onClick={() => setSlide((s) => (s - 1 + PRODUCT_SLIDES.length) % PRODUCT_SLIDES.length)}
            className="text-[#8B5E3C] text-sm hover:text-kb-text"
          >‹</button>
          {PRODUCT_SLIDES.map((_, i) => (
            <button
              key={i}
              onClick={() => setSlide(i)}
              className={`rounded-full transition-all ${
                i === slide ? 'w-5 h-2 bg-[#2D6A4F]' : 'w-2 h-2 bg-[#D4D4D4]'
              }`}
            />
          ))}
          <button
            onClick={() => setSlide((s) => (s + 1) % PRODUCT_SLIDES.length)}
            className="text-[#8B5E3C] text-sm hover:text-kb-text"
          >›</button>
        </div>
      </div>
    </section>
  )
}
