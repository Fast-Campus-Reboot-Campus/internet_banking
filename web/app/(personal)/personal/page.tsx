'use client'

import Link from 'next/link'
import { useState, useEffect } from 'react'
import HeroWithQuickMenu from '@/components/home/HeroWithQuickMenu'
import ProductShowcase from '@/components/home/ProductShowcase'
import { MOCK_ACCOUNTS, formatNumber } from '@/lib/mock-data'

interface StoredUser { name: string; email: string }

const RECENT_TRANSFERS = [
  { datetime: '2026.05.25 01:39:33', name: '이민준', amount: 50,     bank: '카뱅', account: '110-521-874032' },
  { datetime: '2026.05.21 14:32:16', name: '이민준', amount: 50,     bank: '카뱅', account: '110-521-874032' },
  { datetime: '2026.02.26 20:23:53', name: '이민준', amount: 30_000, bank: '카뱅', account: '110-521-874032' },
]

const NEWS_ITEMS = [
  { type: '새소식', title: '\'1분 브리핑 증시 안내\' 서비스 출시 안내', date: '26.04.28' },
  { type: '새소식', title: '인터넷뱅킹「AXful금융그룹 통합조회」서비스 종...', date: '26.04.27' },
  { type: '이벤트', title: '케이봇쌤 포트폴리오 고객 챌린지 이벤트', date: '26.05.04 ~ 26.06.30' },
  { type: '이벤트', title: '소득공제부터 복리이자까지!「노란우산 비대면...', date: '26.04.20 ~ 26.11.30' },
]

const mainAccount = MOCK_ACCOUNTS[0]

export default function HomePage() {
  const [user, setUser] = useState<StoredUser | null>(null)

  useEffect(() => {
    try {
      const raw = localStorage.getItem('user')
      if (raw) setUser(JSON.parse(raw))
    } catch {}
  }, [])

  /* ── 로그인 후 홈 ── */
  if (user) {
    return (
      <main className="pb-0">
        <div className="max-w-kb-container mx-auto px-6 pb-6">
        <div className="flex gap-0">

          {/* ===== 좌측 ===== */}
          <div className="flex-1 min-w-0 pl-5 pr-8 pt-6 border-r border-kb-border relative">

            {/* 패밀리 뱃지 */}
            <div className="mb-4">
              <span className="border border-kb-border rounded-full px-4 py-1 text-[14px] text-kb-text-body inline-flex items-center gap-1 hover:bg-kb-beige-light cursor-pointer">
                패밀리 <span className="text-[12px]">›</span>
              </span>
            </div>

            {/* 인사 — 이미지 쪽 공간 확보 */}
            <div className="mb-5 pr-[450px]">
              <p className="text-[32px] font-bold text-kb-text mb-2">{user.name} 고객님,</p>
              <p className="text-[16px] font-semibold" style={{ color: '#6B4C35' }}>AXful은 항상 곁에 있습니다.</p>
              <p className="text-[16px] font-semibold" style={{ color: '#6B4C35' }}>24시간, 365일 늘 환영합니다.</p>
            </div>

            {/* 알람 영역 */}
            <div className="flex items-center gap-2 bg-[#F5F5F5] rounded-full px-4 py-2 mb-5 w-fit">
              <svg viewBox="0 0 20 20" fill="none" className="w-4 h-4 text-kb-text-muted" stroke="currentColor" strokeWidth="1.5">
                <path d="M10 2a6 6 0 016 6c0 3 1 4 2 5H2c1-1 2-2 2-5a6 6 0 016-6z"/>
                <path d="M8 17a2 2 0 004 0"/>
              </svg>
              <span className="text-[14px] text-kb-text-muted">알람이 없습니다.</span>
            </div>

            {/* 계좌 카드 — 전체 너비, 이미지 위에 표시 */}
            <div className="bg-[#F0EDEA] rounded-xl p-5 relative z-10">
              <div className="flex items-center justify-between mb-3">
                <Link href="/inquiry/accounts" className="flex items-center gap-1 text-[13px] text-kb-text-body hover:underline">
                  <svg viewBox="0 0 16 16" fill="none" className="w-3.5 h-3.5" stroke="currentColor" strokeWidth="1.5">
                    <circle cx="8" cy="8" r="6"/><line x1="8" y1="5" x2="8" y2="11"/><line x1="5" y1="8" x2="11" y2="8"/>
                  </svg>
                  계좌바꾸기
                </Link>
                <Link href="/inquiry/transactions" className="flex items-center gap-1 text-[13px] text-kb-text-body hover:underline">
                  <svg viewBox="0 0 16 16" fill="none" className="w-3.5 h-3.5" stroke="currentColor" strokeWidth="1.5">
                    <line x1="2" y1="4" x2="14" y2="4"/><line x1="2" y1="8" x2="14" y2="8"/><line x1="2" y1="12" x2="10" y2="12"/>
                  </svg>
                  거래내역보기
                </Link>
              </div>
              <p className="text-[16px] font-bold text-kb-text">{mainAccount.name}</p>
              <p className="text-[14px] text-kb-text-muted mb-4">{mainAccount.number}</p>
              <div className="border-t border-dashed border-kb-border pt-4 mb-4">
                <p className="text-[26px] font-bold text-kb-text text-right">
                  {formatNumber(mainAccount.balance)}원
                </p>
              </div>
              <div className="flex gap-2">
                <Link href="/inquiry/accounts"
                  className="flex-1 py-2.5 text-[16px] font-bold text-white text-center hover:opacity-90 transition-opacity"
                  style={{ backgroundColor: '#3D3D3D' }}>
                  조회
                </Link>
                <Link href="/transfer/account"
                  className="flex-1 py-2.5 text-[16px] font-bold text-kb-text text-center bg-kb-yellow hover:bg-kb-yellow-dark transition-colors">
                  이체
                </Link>
              </div>
            </div>

            {/* 히어로 이미지 — 절대 위치, 상단 우측 */}
            <div className="absolute top-4 right-8 z-0 pointer-events-none">
              <img src="/images/personal-login-hero1.png" alt="" className="w-[432px] h-[384px] object-contain object-top" />
            </div>

          </div>

          {/* ===== 우측: 최근이체 내역 ===== */}
          <div className="w-[431px] flex-shrink-0 pl-8 pt-6">
            <h2 className="text-[24px] font-bold text-kb-text mb-4">
              <span style={{ color: '#6B4C35' }}>최근이체</span> 내역
            </h2>
            <div className="divide-y divide-kb-border">
              {RECENT_TRANSFERS.map((t, i) => (
                <div key={i} className="py-4">
                  <div className="flex items-center justify-between mb-1">
                    <p className="text-[13px] text-kb-text-muted leading-tight">{t.datetime}</p>
                    <Link href="/transfer/account"
                      className="text-[13px] text-kb-text-muted hover:text-kb-text flex items-center gap-0.5">
                      바로이체 <span>›</span>
                    </Link>
                  </div>
                  <div className="flex items-center justify-between">
                    <p className="text-[20px] font-bold text-kb-text">{t.name}</p>
                    <p className="text-[20px] font-bold text-kb-text">{formatNumber(t.amount)}원</p>
                  </div>
                  <p className="text-[13px] text-kb-text-muted mt-0.5">{t.bank} {t.account}</p>
                </div>
              ))}
            </div>
          </div>

        </div>
        </div>{/* max-w-kb-container */}

        <ProductShowcase />

        {/* 새소식 / 이벤트 + 고객센터 */}
        <section className="pt-8 pb-6">
          <div className="max-w-kb-container mx-auto px-6 grid gap-4" style={{ gridTemplateColumns: '1.97fr 1fr' }}>
            <div>
              <h2 className="text-2xl font-bold text-kb-text mb-3">새소식 / 이벤트</h2>
              <div className="border-b border-[#E0E0E0] mb-4" />
              <ul>
                {NEWS_ITEMS.map((item, idx) => (
                  <li key={idx}>
                    <Link href="#" className="flex items-center gap-3 py-1.5">
                      <span className="text-sm border px-2 py-0.5 flex-shrink-0 font-medium"
                        style={{ borderColor: item.type === '새소식' ? '#6B4C35' : '#2D6A4F', color: item.type === '새소식' ? '#6B4C35' : '#2D6A4F' }}>
                        {item.type}
                      </span>
                      <span className="text-base text-kb-text-body flex-1 truncate">{item.title}</span>
                      <span className="text-base text-kb-text-muted w-56 flex-shrink-0 whitespace-nowrap">⊙ {item.date}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
            <div className="border-l border-[#E0E0E0] pl-6 flex flex-col items-center text-center">
              <h2 className="text-2xl font-bold text-kb-text mb-6">고객센터</h2>
              <p className="text-[28px] font-bold mb-3" style={{ color: '#052e20' }}>1588·0000</p>
              <div className="space-y-1 text-sm text-kb-text-body mb-3">
                <p>해외 +82-2-0000-0000</p>
                <p>외국인상담 1599-0044</p>
                <p className="text-xs text-kb-text-muted">평일 09시 ~ 18시 (은행휴무일 제외)</p>
              </div>
            </div>
          </div>
        </section>

        {/* 챗봇 */}
        <section className="pt-6 pb-12">
          <div className="max-w-kb-container mx-auto px-6">
            <div className="flex items-center px-16 py-16" style={{ backgroundColor: '#F5F5F5' }}>
              <div>
                <h2 className="text-2xl font-bold text-kb-text mb-2">챗봇에게 물어봐~</h2>
                <p className="text-base text-kb-text-body">24시간 간편한 채팅상담 인터넷뱅킹 궁금증을 해결해드려요!</p>
              </div>
            </div>
          </div>
        </section>
      </main>
    )
  }

  /* ── 비로그인 홈 ── */
  return (
    <main className="pb-0">
      <HeroWithQuickMenu />
      <ProductShowcase />

      <section className="pt-8 pb-6">
        <div className="max-w-kb-container mx-auto px-6 grid gap-4" style={{ gridTemplateColumns: '1.97fr 1fr' }}>
          <div>
            <h2 className="text-2xl font-bold text-kb-text mb-3">새소식 / 이벤트</h2>
            <div className="border-b border-[#E0E0E0] mb-4" />
            <ul>
              {NEWS_ITEMS.map((item, idx) => (
                <li key={idx}>
                  <Link href="#" className="flex items-center gap-3 py-1.5">
                    <span className="text-sm border px-2 py-0.5 flex-shrink-0 font-medium"
                      style={{ borderColor: item.type === '새소식' ? '#6B4C35' : '#2D6A4F', color: item.type === '새소식' ? '#6B4C35' : '#2D6A4F' }}>
                      {item.type}
                    </span>
                    <span className="text-base text-kb-text-body flex-1 truncate">{item.title}</span>
                    <span className="text-base text-kb-text-muted w-56 flex-shrink-0 whitespace-nowrap">⊙ {item.date}</span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
          <div className="border-l border-[#E0E0E0] pl-6 flex flex-col items-center text-center">
            <h2 className="text-2xl font-bold text-kb-text mb-6">고객센터</h2>
            <p className="text-[28px] font-bold mb-3" style={{ color: '#052e20' }}>1588·0000</p>
            <div className="space-y-1 text-sm text-kb-text-body mb-3">
              <p>해외 +82-2-0000-0000</p>
              <p>외국인상담 1599-0044</p>
              <p className="text-xs text-kb-text-muted">평일 09시 ~ 18시 (은행휴무일 제외)</p>
            </div>
          </div>
        </div>
      </section>

      <section className="pt-6 pb-12">
        <div className="max-w-kb-container mx-auto px-6">
          <div className="flex items-center px-16 py-16" style={{ backgroundColor: '#F5F5F5' }}>
            <div>
              <h2 className="text-2xl font-bold text-kb-text mb-2">챗봇에게 물어봐~</h2>
              <p className="text-base text-kb-text-body">24시간 간편한 채팅상담 인터넷뱅킹 궁금증을 해결해드려요!</p>
            </div>
          </div>
        </div>
      </section>
    </main>
  )
}
