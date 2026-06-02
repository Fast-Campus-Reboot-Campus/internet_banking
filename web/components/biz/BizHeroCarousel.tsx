'use client'

import { useState, useEffect } from 'react'
import Image from 'next/image'
import Link from 'next/link'

const HERO_IMAGE = '/images/biz-hero1.png'

const SLIDES = [
  {
    badge: 'AXful 기업금융',
    title: '기업에 답이 필요할 때,\nAXful Bank와 함께 해결해볼까요?',
    desc: '기업의 모든 순간, AXful이 있다.\nAXful 기업금융 영상을 소개합니다.',
  },
  {
    badge: '비대면전용 기업대출상품',
    title: '은행방문이 필요없는\n개인사업자대출',
    desc: 'AXful소상공인 신용대출을 소개합니다!',
  },
  {
    badge: 'AXful 셀러론',
    title: '온라인 셀러를 위한\n쉽고 빠른 선정산 서비스',
    desc: '셀러는 은행에게 판매대금을 선정산 받고\n제휴 마켓은 은행에 대출금을 상환하는 서비스!',
  },
]

const BADGE_BG = '#384d84'

export default function BizHeroCarousel() {
  const [current, setCurrent] = useState(0)
  const [paused, setPaused] = useState(false)

  useEffect(() => {
    if (paused) return
    const t = setInterval(() => setCurrent((c) => (c + 1) % SLIDES.length), 4500)
    return () => clearInterval(t)
  }, [paused])

  const slide = SLIDES[current]

  return (
    <section
      className="overflow-hidden bg-white"
      style={{ height: '400px' }}
    >
      <div className="max-w-kb-container mx-auto px-6 h-full flex items-center justify-between">
        <div className="pl-14 flex flex-col gap-3 max-w-[50%] pt-14">


          {/* 제목 — 고정 높이로 배지 위치 고정 */}
          <p
            className="text-[26px] font-normal text-kb-text whitespace-pre-line leading-snug overflow-hidden"
            style={{ height: '76px' }}
          >
            {slide.title}
          </p>

          {/* 배지 */}
          <span
            style={{
              display: 'inline-block',
              alignSelf: 'flex-start',
              height: '52px',
              lineHeight: '52px',
              padding: '0 20px',
              backgroundColor: BADGE_BG,
              color: '#fff',
              fontWeight: 700,
              fontSize: '34px',
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
          <Link href="#" className="text-base text-kb-text underline mt-1">바로가기</Link>

          {/* 컨트롤 */}
          <div className="flex items-center gap-2 mt-2">
            <button onClick={() => setCurrent((c) => (c - 1 + SLIDES.length) % SLIDES.length)}
              className="w-6 h-6 flex items-center justify-center text-kb-text-muted hover:text-kb-text">‹</button>
            {SLIDES.map((_, i) => (
              <button key={i} onClick={() => setCurrent(i)}
                className={`rounded-full transition-all ${i === current ? 'w-5 h-2 bg-kb-text' : 'w-2 h-2 bg-kb-text/30'}`} />
            ))}
            <button onClick={() => setCurrent((c) => (c + 1) % SLIDES.length)}
              className="w-6 h-6 flex items-center justify-center text-kb-text-muted hover:text-kb-text">›</button>
            <button onClick={() => setPaused(!paused)}
              className="w-6 h-6 flex items-center justify-center text-kb-text-muted hover:text-kb-text text-xs">
              {paused ? '▶' : '⏸'}
            </button>
          </div>

        </div>

        {/* 이미지 영역 */}
        <div className="relative flex-shrink-0 mr-52" style={{ width: '400px', height: '330px' }}>
          <Image src={HERO_IMAGE} alt="hero" fill className="object-contain" />
        </div>

      </div>
    </section>
  )
}
