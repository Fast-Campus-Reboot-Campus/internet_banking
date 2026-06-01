'use client'

import { useState, useEffect } from 'react'
import Link from 'next/link'

type SlideItem = { title: string; desc: string; icon: string; iconBg: string }

const SLIDES: SlideItem[][] = [
  [
    {
      title: 'AXful소상공인\n신용대출',
      desc: '개인사업자를 대상으로\n모바일 및 기업인터넷뱅킹을\n통해 다양한 대출조건을\n하나로!',
      icon: '💰',
      iconBg: '#E879A0',
    },
    {
      title: 'AX풀뱅크 특별출연\n수출입금융 지원안내',
      desc: '수출입 중소·중견기업\n지원을 위한!',
      icon: '🧾',
      iconBg: '#7B9DC8',
    },
    {
      title: 'AXful System',
      desc: '신속·정확·편리한 재무\n및 세무정보 전송 네트워크',
      icon: '💲',
      iconBg: '#5BC9A8',
    },
  ],
  [
    {
      title: 'AXful사장님+\n마이너스통장',
      desc: '사장님, 필요한 만큼 사용하고\n언제든 상환하세요',
      icon: '🏪',
      iconBg: '#FF8C00',
    },
    {
      title: 'AXful Payment\nUsance',
      desc: '전자무역(EDI)을 통한 서비스 신청\nOPEN!',
      icon: '💳',
      iconBg: '#4A90D9',
    },
    {
      title: '급여명세서 간편 작성,\n5대 법정의무교육',
      desc: '사장님 필수 콘텐츠에서\n무료로 해결하세요!',
      icon: '📝',
      iconBg: '#E85D8A',
    },
  ],
  [
    {
      title: 'AXful셀러우대\n신용대출',
      desc: '최대 3% 우대금리 제공!\n온라인 셀러를 위한\n금리우대 신용대출',
      icon: '💰',
      iconBg: '#E879A0',
    },
    {
      title: 'ONE AXful\n기업우대대출',
      desc: '대출신청 대상 간소화,\n대출기간 및 상환방식의\n다각화, 우대서비스 제공',
      icon: '🏦',
      iconBg: '#5BC9A8',
    },
    {
      title: 'AXful기업대표상품',
      desc: 'AXful 기업 고객을 대상으로한\n다양한 상품을 만나보세요!',
      icon: '💎',
      iconBg: '#D85CA8',
    },
  ],
]

const FOURTH_CARD = {
  title: 'AXful기업뱅킹은\n무엇이 좋을까요?',
  desc: '사업자 고객님을 위한\n맞춤형 기업서비스를 소개합니다.',
  icon: '🐻',
  iconBg: '#6B5A40',
}

export default function BizProductCarousel() {
  const [current, setCurrent] = useState(0)
  const [paused, setPaused] = useState(false)

  useEffect(() => {
    if (paused) return
    const t = setInterval(() => setCurrent((c) => (c + 1) % SLIDES.length), 4500)
    return () => clearInterval(t)
  }, [paused])

  const items = SLIDES[current]

  return (
    <section className="py-16" style={{ backgroundColor: '#d9e4fa' }}>
      <div className="max-w-kb-container mx-auto px-6">
        <div className="grid gap-7" style={{ gridTemplateColumns: '2.7fr 1fr' }}>

          {/* 슬라이딩 큰 카드 */}
          <div className="bg-white rounded-lg overflow-hidden flex h-[320px] shadow-md">
            {items.map((p, i) => (
              <div key={i}
                className="flex-1 flex flex-col justify-between p-6 relative">
                {i > 0 && <div className="absolute left-0 top-6 bottom-6 w-px bg-kb-border" />}
                <div>
                  <p className="text-xl font-bold text-kb-text whitespace-pre-line leading-snug">
                    {p.title}
                  </p>
                  <p className="text-base text-kb-text-muted mt-2 whitespace-pre-line leading-relaxed">
                    {p.desc}
                  </p>
                </div>
                <div className="mt-4">
                  <Link href="#" className="text-base text-kb-text-body underline">바로가기</Link>
                </div>
              </div>
            ))}
          </div>

          {/* 고정 작은 카드 */}
          <div className="rounded-lg p-6 flex flex-col justify-between h-[320px] shadow-md
                         hover:shadow-lg hover:scale-[1.02] transition-all duration-150"
               style={{ backgroundColor: '#eefaff' }}>
            <div>
              <p className="text-xl font-bold text-kb-text whitespace-pre-line leading-snug">
                {FOURTH_CARD.title}
              </p>
              <p className="text-base text-kb-text-muted mt-2 whitespace-pre-line leading-relaxed">
                {FOURTH_CARD.desc}
              </p>
            </div>
            <div className="mt-4">
              <Link href="#" className="text-base text-kb-text-body underline">바로가기</Link>
            </div>
          </div>
        </div>

        {/* 컨트롤 */}
        <div className="flex items-center justify-center gap-2 mt-5">
          <button onClick={() => setCurrent((c) => (c - 1 + SLIDES.length) % SLIDES.length)}
            className="w-6 h-6 flex items-center justify-center text-kb-text-muted hover:text-kb-text text-lg">‹</button>
          {SLIDES.map((_, i) => (
            <button key={i} onClick={() => setCurrent(i)}
              className={`rounded-full transition-all ${i === current ? 'w-5 h-2 bg-kb-text' : 'w-2 h-2 bg-kb-text/30'}`} />
          ))}
          <button onClick={() => setCurrent((c) => (c + 1) % SLIDES.length)}
            className="w-6 h-6 flex items-center justify-center text-kb-text-muted hover:text-kb-text text-lg">›</button>
          <button onClick={() => setPaused(!paused)}
            className="w-6 h-6 flex items-center justify-center text-kb-text-muted hover:text-kb-text text-xs">
            {paused ? '▶' : '⏸'}
          </button>
        </div>
      </div>
    </section>
  )
}
