'use client'

import { useEffect, useCallback } from 'react'
import Image from 'next/image'
import Link from 'next/link'

export const HERO_SLIDES = [
  {
    badge: 'AXful 아파트담보대출',
    title: '영업점 방문없이 신청부터 서류제출까지',
    desc: '24시간 365일 쉽고 간편하게!',
    href: '/products/loan',
    bg: '#E0F5EF',
  },
  {
    badge: 'AXful 정기예금',
    title: '최고 연 3.80% 금리 혜택',
    desc: '지금 바로 가입하세요!',
    href: '/products/deposit',
    bg: '#F7EDE4',
  },
  {
    badge: 'AXful 만능 ISA',
    title: '비과세 한도 최고 4백만원',
    desc: '전문가가 운용해주는 종합자산관리',
    href: '/products/isa',
    bg: '#EBF2E8',
  },
]

interface HeroCarouselProps {
  current: number
  paused: boolean
  onChangeTo: (index: number) => void
  onPausedChange: (paused: boolean) => void
}

export default function HeroCarousel({ current, paused, onChangeTo, onPausedChange }: HeroCarouselProps) {
  const next = useCallback(() => onChangeTo((current + 1) % HERO_SLIDES.length), [onChangeTo, current])
  const prev = () => onChangeTo((current - 1 + HERO_SLIDES.length) % HERO_SLIDES.length)

  useEffect(() => {
    if (paused) return
    const timer = setInterval(next, 4500)
    return () => clearInterval(timer)
  }, [paused, next])

  const slide = HERO_SLIDES[current]

  return (
    <div
      className="relative w-full overflow-hidden"
      style={{ backgroundColor: slide.bg, height: '380px' }}
    >
      <div className="max-w-kb-container mx-auto px-6 h-full flex items-center justify-between">
        <div className="pl-14 flex flex-col gap-3 max-w-[55%]">
          {/* 제목 */}
          <p className="text-[26px] font-normal text-kb-text whitespace-pre-line leading-snug">
            {slide.title}
          </p>

          {/* 배지 — 큰 강조 블록 */}
          <span
            style={{
              display: 'inline-block',
              alignSelf: 'flex-start',
              height: '52px',
              lineHeight: '52px',
              padding: '0 20px',
              backgroundColor: '#2D6A4F',
              color: '#fff',
              fontWeight: 700,
              fontSize: '30px',
              whiteSpace: 'nowrap',
              borderRadius: '0 8px 0 8px',
            }}
          >
            {slide.badge}
          </span>

          {/* 설명 */}
          <p className="text-base text-kb-text-body leading-relaxed whitespace-pre-line">
            {slide.desc}
          </p>

          {/* 바로가기 */}
          <Link href={slide.href} className="text-base text-kb-text underline mt-1">
            바로가기
          </Link>

          {/* 네비게이션 */}
          <div className="flex items-center gap-2 mt-2">
            <button onClick={prev} className="w-6 h-6 flex items-center justify-center text-kb-text-muted hover:text-kb-text" aria-label="이전">‹</button>
            {HERO_SLIDES.map((_, i: number) => (
              <button
                key={i}
                onClick={() => onChangeTo(i)}
                className={`rounded-full transition-all ${
                  i === current ? 'w-5 h-2 bg-kb-text' : 'w-2 h-2 bg-kb-text/30'
                }`}
                aria-label={`슬라이드 ${i + 1}`}
              />
            ))}
            <button onClick={next} className="w-6 h-6 flex items-center justify-center text-kb-text-muted hover:text-kb-text" aria-label="다음">›</button>
            <button
              onClick={() => onPausedChange(!paused)}
              className="w-6 h-6 flex items-center justify-center text-kb-text-muted hover:text-kb-text text-xs"
              aria-label={paused ? '자동재생' : '일시정지'}
            >
              {paused ? '▶' : '⏸'}
            </button>
          </div>
        </div>

        {/* 우측 이미지 */}
        <div className="relative flex-shrink-0" style={{ width: '680px', height: '380px' }}>
          <Image src="/images/personal-hero1.png" alt="hero" fill className="object-contain" />
        </div>
      </div>
    </div>
  )
}
