'use client'

import Link from 'next/link'
import { useState } from 'react'

const CATEGORIES = [
  { no: '01', desc: '열심히 모은 종자돈을 더 크게', label: '예금 상품',     href: '/products/deposit/list?tab=예금',      key: '예금' },
  { no: '02', desc: '당신의 노력과 꿈을 모아모아',  label: '적금 상품',     href: '/products/deposit/list?tab=자유적금',   key: '적금' },
  { no: '03', desc: '입금과 출금을 내 마음대로',    label: '입출금자유 상품', href: '/products/deposit/list?tab=입출금자유', key: '입출금자유' },
  { no: '04', desc: '내 집 마련의 꿈을 위한',      label: '주택청약 상품', href: '/products/deposit/list?tab=주택청약',  key: '주택청약' },
]

const CALC_TABS = [
  '열심히 모은 목돈을 예치할 때',
  '매월 일정금액을 저축할 때',
  '목표금액을 모을 때',
]

const SLIDES = [
  {
    badge: '적금', category: '적금',
    sub: '어린이/청년 무료 보험가입',
    title: 'AXful Young Youth 적금',
    period: '1년', amount: '3백만원 이내',
    rate: '연 2.1% ~ 3.4%', rateNote: '2026.05.25 기준, 세금공제전, 우대금리포함',
    href: '/products/deposit/list?tab=자유적금',
  },
  {
    badge: '입출금자유', category: '입출금자유',
    sub: '저금통 기능과 수수료면제 서비스 제공',
    title: 'AXful Young Youth 통장',
    period: '제한없음', amount: '제한없음',
    rate: '연 2%', rateNote: '2026.05.25 기준, 세금공제전, 우대금리포함',
    href: '/products/deposit/list?tab=입출금자유',
  },
  {
    badge: '적금', category: '적금',
    sub: '누구나 쉽게 우대받는 DIY',
    title: 'AXful 내맘대로적금',
    period: '6~36개월', amount: '1만원 이상',
    rate: '연 2.95% ~ 3.55%', rateNote: '2026.05.25 기준, 세금공제전, 우대금리포함',
    href: '/products/deposit/list?tab=자유적금',
  },
  {
    badge: '예금', category: '예금',
    sub: 'Digital AXful의 대표 정기예금',
    title: 'AXful 정기예금',
    period: '1~36개월', amount: '제한없음',
    rate: '연 2.4% ~ 2.9%', rateNote: '2026.05.25 기준, 세금공제전',
    href: '/products/deposit/list?tab=예금',
  },
  {
    badge: '주택청약', category: '주택청약',
    sub: '내 집 마련의 꿈을 응원합니다',
    title: 'AXful 주택청약종합저축',
    period: '24개월 기준', amount: '제한없음',
    rate: '연 3.1%', rateNote: '2026.05.25 기준, 세금공제전',
    href: '/products/deposit/list?tab=주택청약',
  },
]

export default function DepositMainPage() {
  const [slide, setSlide] = useState(0)
  const [calcTab, setCalcTab] = useState(0)
  const [amount, setAmount] = useState('')
  const [monthly, setMonthly] = useState('')
  const [target, setTarget] = useState('')
  const [months, setMonths] = useState('')
  const [rate, setRate] = useState('')
  const [calcResult, setCalcResult] = useState<string | null>(null)

  const current = SLIDES[slide]

  function handleCalc() {
    const m = parseInt(months) || 0
    const r = (parseFloat(rate) || 2.4) / 100
    if (calcTab === 0) {
      const a = parseFloat(amount.replace(/,/g, '')) || 0
      const interest = Math.floor(a * r * (m / 12))
      setCalcResult(`예상 이자: ${interest.toLocaleString('ko-KR')}원 / 세후: ${Math.floor(interest * 0.846).toLocaleString('ko-KR')}원`)
    } else if (calcTab === 1) {
      const mo = parseFloat(monthly.replace(/,/g, '')) || 0
      const total = mo * m
      const interest = Math.floor(total * r * 0.5)
      setCalcResult(`만기수령액: ${(total + interest).toLocaleString('ko-KR')}원 (이자: ${interest.toLocaleString('ko-KR')}원)`)
    } else {
      const tgt = parseFloat(target.replace(/,/g, '')) || 0
      const mo = m > 0 ? Math.ceil(tgt / m) : 0
      setCalcResult(`월 저축 필요금액: ${mo.toLocaleString('ko-KR')}원`)
    }
  }

  return (
    <div className="max-w-kb-container mx-auto">
      {/* ===== 히어로 영역 ===== */}
      <div className="flex">

        {/* 좌: 슬라이드 배너 — GNB px-6 + 4/6 비율과 정렬: calc(66.667% - 8px) */}
        <div className="bg-white px-12 pt-9 pb-7 relative min-h-[340px] flex flex-col justify-between"
          style={{ width: 'calc(66.667% - 8px)', flexShrink: 0 }}>
          <div>
            {/* 뱃지 */}
            <span className="inline-block text-white text-[12px] font-bold px-3 py-0.5 rounded-sm mb-3"
              style={{ backgroundColor: '#2D5A3D' }}>
              {current.badge}
            </span>

            {/* 부제목 */}
            <p className="text-[13px] text-kb-text-muted mb-2">{current.sub}</p>

            {/* 상품명 — 매우 크게 */}
            <h2 className="text-[38px] font-bold text-kb-text leading-tight mb-5">{current.title}</h2>

            <hr className="border-t-4 border-kb-border mb-5" />

            {/* 기간 + 금액 (나란히) */}
            <div className="flex gap-14 mb-5">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0"
                  style={{ backgroundColor: '#C09B3A' }}>
                  <svg viewBox="0 0 24 24" fill="none" className="w-6 h-6" stroke="white" strokeWidth="1.8">
                    <rect x="3" y="4" width="18" height="17" rx="2"/>
                    <line x1="3" y1="9" x2="21" y2="9"/>
                    <line x1="8" y1="2" x2="8" y2="6"/>
                    <line x1="16" y1="2" x2="16" y2="6"/>
                  </svg>
                </div>
                <div>
                  <p className="text-[12px] text-kb-text-muted">기간</p>
                  <p className="text-[19px] font-bold text-kb-text">{current.period}</p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0"
                  style={{ backgroundColor: '#C09B3A' }}>
                  <svg viewBox="0 0 24 24" fill="none" className="w-6 h-6" stroke="white" strokeWidth="1.8">
                    <circle cx="12" cy="12" r="9"/>
                    <text x="12" y="16" textAnchor="middle" fontSize="11" fill="white" stroke="none" fontWeight="bold">₩</text>
                  </svg>
                </div>
                <div>
                  <p className="text-[12px] text-kb-text-muted">금액</p>
                  <p className="text-[19px] font-bold text-kb-text">{current.amount}</p>
                </div>
              </div>
            </div>

            {/* 금리 */}
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0"
                style={{ backgroundColor: '#C09B3A' }}>
                <svg viewBox="0 0 24 24" fill="none" className="w-6 h-6" stroke="white" strokeWidth="2">
                  <circle cx="12" cy="12" r="9"/>
                  <line x1="12" y1="7" x2="12" y2="17"/>
                  <line x1="7" y1="12" x2="17" y2="12"/>
                </svg>
              </div>
              <div>
                <p className="text-[22px] font-bold text-kb-text leading-none">
                  {current.rate}
                  <span className="text-[11px] text-kb-text-muted font-normal ml-2">{current.rateNote}</span>
                </p>
              </div>
            </div>
          </div>

          {/* 하단: 캐러셀 + 바로가기 */}
          <div className="flex items-center justify-between mt-5">
            <div className="flex items-center gap-2">
              <button onClick={() => setSlide(s => (s - 1 + SLIDES.length) % SLIDES.length)}
                className="text-kb-text-muted hover:text-kb-text text-xl leading-none">‹</button>
              <div className="flex gap-1.5">
                {SLIDES.map((_, i) => (
                  <button key={i} onClick={() => setSlide(i)}
                    className={`w-2 h-2 rounded-full transition-colors ${
                      i === slide ? 'bg-kb-text' : 'bg-kb-border'
                    }`} />
                ))}
              </div>
              <button onClick={() => setSlide(s => (s + 1) % SLIDES.length)}
                className="text-kb-text-muted hover:text-kb-text text-xl leading-none">›</button>
              <span className="ml-2 text-[11px] text-kb-text-muted border border-kb-border px-1.5 py-0.5">II</span>
            </div>
            <Link href={current.href}
              className="text-[14px] font-bold text-kb-text underline hover:opacity-70">
              바로가기
            </Link>
          </div>
        </div>

        {/* 우: 카테고리 4개 */}
        <div className="flex-1 bg-[#F2F0E8] flex flex-col gap-2 p-3">
          {CATEGORIES.map(cat => {
            const isActive = cat.key === current.category
            return (
              <Link key={cat.no} href={cat.href}
                className={`flex items-center justify-between px-5 py-4 flex-1 transition-colors ${
                  isActive
                    ? 'bg-[#2D5A3D]'
                    : 'bg-white hover:bg-kb-beige-light'
                }`}>
                <div className="flex items-start gap-4">
                  <span className={`text-[22px] font-bold leading-none mt-0.5 ${
                    isActive ? 'text-white/70' : 'text-[#C09B3A]'
                  }`}>
                    {cat.no}
                  </span>
                  <div>
                    <p className={`text-[11px] leading-relaxed ${isActive ? 'text-white/80' : 'text-kb-text-muted'}`}>
                      {cat.desc}
                    </p>
                    <p className={`text-[14px] font-bold ${isActive ? 'text-white' : 'text-kb-text'}`}>
                      {cat.label}
                    </p>
                  </div>
                </div>
                {isActive && (
                  <span className="text-white text-lg">›</span>
                )}
              </Link>
            )
          })}
        </div>
      </div>

      {/* ===== 상품 검색 ===== */}
      <div className="bg-[#ede0d4] px-10 py-5 flex items-center gap-4">
        <p className="text-[20px] font-bold text-kb-text whitespace-nowrap">원하시는 상품을 찾아보세요.</p>
        <div className="flex-1 flex items-center border border-kb-border bg-white rounded-full overflow-hidden">
          <input type="text" placeholder="상품명을 입력하세요." className="flex-1 px-5 py-2.5 text-[20px] outline-none bg-transparent" />
          <button className="px-5 py-2.5 text-kb-text-muted hover:text-kb-text">
            <svg viewBox="0 0 20 20" fill="none" className="w-5 h-5" stroke="currentColor" strokeWidth="2">
              <circle cx="9" cy="9" r="6"/><line x1="14" y1="14" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
      </div>

      <div className="px-10 py-8">
        {/* ===== 주요 서비스 ===== */}
        <div className="flex items-center gap-14 mb-8 pb-7 border-b border-kb-border">
          <h3 className="text-[18px] font-bold text-kb-text whitespace-nowrap">주요 서비스</h3>
          <div className="flex flex-1 justify-evenly">
            {[
              {
                label: '예금조회', href: '/products/deposit/inquiry/new',
                icon: (
                  <svg viewBox="0 0 48 48" fill="none" className="w-12 h-12" stroke="currentColor" strokeWidth="1.5">
                    <rect x="5" y="5" width="22" height="28" rx="2"/>
                    <line x1="10" y1="13" x2="22" y2="13"/>
                    <line x1="10" y1="19" x2="22" y2="19"/>
                    <line x1="10" y1="25" x2="17" y2="25"/>
                    <circle cx="33" cy="31" r="10" fill="white" stroke="#C09B3A" strokeWidth="1.8"/>
                    <line x1="27" y1="31" x2="39" y2="31" stroke="#C09B3A" strokeWidth="1.5" strokeLinecap="round"/>
                    <line x1="33" y1="25" x2="33" y2="37" stroke="#C09B3A" strokeWidth="1.5" strokeLinecap="round"/>
                    <line x1="40" y1="38" x2="44" y2="43" stroke="#C09B3A" strokeWidth="2.2" strokeLinecap="round"/>
                  </svg>
                ),
              },
              {
                label: '예금해지', href: '/products/deposit/inquiry/terminate',
                icon: (
                  <svg viewBox="0 0 48 48" fill="none" className="w-12 h-12" stroke="currentColor" strokeWidth="1.5">
                    <rect x="6" y="5" width="22" height="28" rx="2"/>
                    <line x1="11" y1="13" x2="23" y2="13"/>
                    <line x1="11" y1="19" x2="23" y2="19"/>
                    <line x1="11" y1="25" x2="18" y2="25"/>
                    <circle cx="34" cy="34" r="10" fill="white" stroke="#C09B3A" strokeWidth="1.8"/>
                    <line x1="29" y1="29" x2="39" y2="39" stroke="#C09B3A" strokeWidth="2" strokeLinecap="round"/>
                    <line x1="39" y1="29" x2="29" y2="39" stroke="#C09B3A" strokeWidth="2" strokeLinecap="round"/>
                  </svg>
                ),
              },
              {
                label: '예금전환', href: '/products/deposit/manage/convert',
                icon: (
                  <svg viewBox="0 0 48 48" fill="none" className="w-12 h-12" stroke="currentColor" strokeWidth="1.5">
                    <rect x="2" y="13" width="16" height="22" rx="2"/>
                    <line x1="6" y1="20" x2="14" y2="20"/>
                    <line x1="6" y1="26" x2="11" y2="26"/>
                    <rect x="30" y="13" width="16" height="22" rx="2"/>
                    <line x1="34" y1="20" x2="42" y2="20"/>
                    <line x1="34" y1="26" x2="39" y2="26"/>
                    <path d="M19 21 L29 21" stroke="#C09B3A" strokeWidth="2" strokeLinecap="round"/>
                    <polyline points="26,18 29,21 26,24" stroke="#C09B3A" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
                    <path d="M29 27 L19 27" stroke="#C09B3A" strokeWidth="2" strokeLinecap="round"/>
                    <polyline points="22,24 19,27 22,30" stroke="#C09B3A" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
                  </svg>
                ),
              },
              {
                label: '만기해지방법 변경', href: '#',
                icon: (
                  <svg viewBox="0 0 48 48" fill="none" className="w-12 h-12" stroke="currentColor" strokeWidth="1.5">
                    <rect x="4" y="10" width="30" height="27" rx="2"/>
                    <line x1="4" y1="18" x2="34" y2="18"/>
                    <line x1="13" y1="6" x2="13" y2="14" strokeWidth="2" strokeLinecap="round"/>
                    <line x1="25" y1="6" x2="25" y2="14" strokeWidth="2" strokeLinecap="round"/>
                    <circle cx="11" cy="25" r="1.5" fill="currentColor" stroke="none"/>
                    <circle cx="19" cy="25" r="1.5" fill="currentColor" stroke="none"/>
                    <circle cx="27" cy="25" r="1.5" fill="currentColor" stroke="none"/>
                    <circle cx="11" cy="32" r="1.5" fill="currentColor" stroke="none"/>
                    <circle cx="19" cy="32" r="1.5" fill="currentColor" stroke="none"/>
                    <path d="M36 34 L44 26 L41 23 L33 31 Z" fill="white" stroke="#C09B3A" strokeWidth="1.8" strokeLinejoin="round"/>
                    <line x1="41" y1="23" x2="44" y2="26" stroke="#C09B3A" strokeWidth="1.5"/>
                    <path d="M33 31 L31 37 L37 35 Z" fill="white" stroke="#C09B3A" strokeWidth="1.5" strokeLinejoin="round"/>
                  </svg>
                ),
              },
            ].map(s => (
              <Link key={s.label} href={s.href}
                className="flex flex-col items-center gap-2 hover:opacity-75 transition-opacity"
                style={{ color: '#333333' }}>
                {s.icon}
                <span className="text-[12px] text-kb-text-body text-center leading-tight">{s.label}</span>
              </Link>
            ))}
          </div>
        </div>

        {/* ===== 예금 계산기 ===== */}
        <div className="mb-8">
          <h3 className="text-[17px] font-bold text-kb-text mb-4">예금 계산기</h3>
          <div className="border border-kb-border">
            <div className="flex">
              {/* 계산기 탭 (좌) */}
              <div className="w-[300px] flex-shrink-0 border-r border-kb-border bg-[#F5F3F0] divide-y divide-kb-border">
                {[
                  { icon: <svg viewBox="0 0 20 20" fill="none" className="w-5 h-5 flex-shrink-0" stroke="currentColor" strokeWidth="1.5"><ellipse cx="10" cy="11" rx="7" ry="5"/><path d="M3 11V9c0-2.8 3.1-5 7-5s7 2.2 7 5v2"/><line x1="10" y1="4" x2="10" y2="2"/></svg> },
                  { icon: <svg viewBox="0 0 20 20" fill="none" className="w-5 h-5 flex-shrink-0" stroke="currentColor" strokeWidth="1.5"><rect x="3" y="2" width="14" height="16" rx="1"/><line x1="6" y1="7" x2="14" y2="7"/><line x1="6" y1="10" x2="14" y2="10"/><line x1="6" y1="13" x2="10" y2="13"/></svg> },
                  { icon: <svg viewBox="0 0 20 20" fill="none" className="w-5 h-5 flex-shrink-0" stroke="currentColor" strokeWidth="1.5"><circle cx="10" cy="10" r="7"/><circle cx="10" cy="10" r="3"/><line x1="10" y1="1" x2="10" y2="3"/><line x1="10" y1="17" x2="10" y2="19"/><line x1="1" y1="10" x2="3" y2="10"/><line x1="17" y1="10" x2="19" y2="10"/></svg> },
                ].map(({ icon }, i) => (
                  <button key={i} onClick={() => { setCalcTab(i); setCalcResult(null) }}
                    className={`flex items-center gap-3 w-full px-5 py-5 text-left text-[13px] leading-snug transition-colors ${
                      calcTab === i
                        ? 'bg-white font-bold text-kb-text'
                        : 'text-kb-text-muted hover:text-kb-text'
                    }`}
                    style={calcTab === i ? { borderLeft: '3px solid #C09B3A' } : { borderLeft: '3px solid transparent' }}>
                    <span style={{ color: calcTab === i ? '#C09B3A' : undefined }}>{icon}</span>
                    <span>{CALC_TABS[i]}</span>
                  </button>
                ))}
              </div>

              {/* 계산기 내용 (우) */}
              <div className="flex-1 px-8 py-6 flex gap-4 items-stretch">
                <div className="flex-1">
                  <p className="text-[14px] text-kb-text mb-6">{CALC_TABS[calcTab]},</p>
                  {calcTab === 0 && (
                    <div className="space-y-4">
                      <div className="flex items-center gap-2 text-[13px]">
                        <input type="text" value={amount} onChange={e => setAmount(e.target.value)}
                          placeholder="예치금액"
                          className="border border-kb-border px-3 py-2 w-32 outline-none text-right text-[13px]" />
                        <span className="text-kb-text whitespace-nowrap">원을</span>
                        <input type="text" value={months} onChange={e => setMonths(e.target.value)}
                          placeholder="기간"
                          className="border border-kb-border px-3 py-2 w-16 outline-none text-right text-[13px]" />
                        <span className="text-kb-text whitespace-nowrap">개월 간</span>
                      </div>
                      <div className="flex items-center gap-2 text-[13px]">
                        <input type="text" value={rate} onChange={e => setRate(e.target.value)}
                          className="border border-kb-border px-3 py-2 w-16 outline-none text-right text-[13px]" placeholder="금리" />
                        <span className="text-kb-text">%의 예금상품에 저축하면?</span>
                      </div>
                      {calcResult && (
                        <p className="text-[13px] font-bold text-kb-text">{calcResult}</p>
                      )}
                    </div>
                  )}
                  {calcTab === 1 && (
                    <div className="space-y-4">
                      <div className="flex items-center gap-2 text-[13px]">
                        <input type="text" value={monthly} onChange={e => setMonthly(e.target.value)}
                          placeholder="월저축액"
                          className="border border-kb-border px-3 py-2 w-32 outline-none text-right text-[13px]" />
                        <span className="text-kb-text whitespace-nowrap">원씩</span>
                        <input type="text" value={months} onChange={e => setMonths(e.target.value)}
                          placeholder="기간"
                          className="border border-kb-border px-3 py-2 w-16 outline-none text-right text-[13px]" />
                        <span className="text-kb-text whitespace-nowrap">개월 간</span>
                      </div>
                      <div className="flex items-center gap-2 text-[13px]">
                        <input type="text" value={rate} onChange={e => setRate(e.target.value)}
                          placeholder="금리"
                          className="border border-kb-border px-3 py-2 w-16 outline-none text-right text-[13px]" />
                        <span className="text-kb-text">%의 적금상품에 저축하면?</span>
                      </div>
                      {calcResult && (
                        <p className="text-[13px] font-bold text-kb-text">{calcResult}</p>
                      )}
                    </div>
                  )}
                  {calcTab === 2 && (
                    <div className="space-y-4">
                      <div className="flex items-center gap-2 text-[13px]">
                        <input type="text" value={target} onChange={e => setTarget(e.target.value)}
                          placeholder="목표금액"
                          className="border border-kb-border px-3 py-2 w-32 outline-none text-right text-[13px]" />
                        <span className="text-kb-text whitespace-nowrap">원을 목표로</span>
                        <input type="text" value={months} onChange={e => setMonths(e.target.value)}
                          placeholder="기간"
                          className="border border-kb-border px-3 py-2 w-16 outline-none text-right text-[13px]" />
                        <span className="text-kb-text whitespace-nowrap">개월 간</span>
                      </div>
                      <div className="flex items-center gap-2 text-[13px]">
                        <input type="text" value={rate} onChange={e => setRate(e.target.value)}
                          placeholder="금리"
                          className="border border-kb-border px-3 py-2 w-16 outline-none text-right text-[13px]" />
                        <span className="text-kb-text">%로 저축한다면?</span>
                      </div>
                      {calcResult && (
                        <p className="text-[13px] font-bold text-kb-text">{calcResult}</p>
                      )}
                    </div>
                  )}
                  <p className="text-[12px] text-kb-text-muted mt-8 flex items-center gap-1.5">
                    <span className="border border-kb-border rounded-full w-4 h-4 flex items-center justify-center text-[8px] flex-shrink-0">i</span>
                    원하시는 정보를 입력하신 후, 예상계산결과를 확인하세요.
                  </p>
                </div>

                {/* 결과보기 버튼 */}
                <div className="flex-shrink-0 self-stretch flex">
                  <button onClick={handleCalc}
                    className="self-stretch px-8 text-white text-[15px] font-bold hover:opacity-90 transition-opacity min-w-[90px]"
                    style={{ backgroundColor: '#5A504A' }}>
                    결과보기
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* ===== 하단 안내 ===== */}
        <div className="flex gap-4">
          <div className="flex-[3] border border-kb-border p-6">
            <p className="text-[15px] font-bold text-kb-text mb-4">이용시간 안내</p>
            <div className="text-[13px] space-y-2">
              <p className="flex items-baseline gap-6">
                <span className="text-kb-text-muted w-20 flex-shrink-0">· 신규/조회</span>
                <span className="text-kb-text">24시간, 365일</span>
              </p>
              <p className="flex items-baseline gap-6">
                <span className="text-kb-text-muted w-20 flex-shrink-0">· 해지</span>
                <span className="text-kb-text">08:00 ~ 24:00&nbsp;
                  <span style={{ color: '#C05050' }}>(토요일 및 공휴일 제외)</span>
                </span>
              </p>
            </div>
          </div>
          <div className="flex-[2] border border-kb-border p-6">
            <p className="text-[15px] font-bold text-kb-text mb-4">예금가이드</p>
            <div className="flex gap-0 text-[13px]">
              <div className="flex-1 space-y-2">
                <Link href="#" className="block text-kb-text-body hover:text-kb-text">· 예금금리 안내</Link>
                <Link href="#" className="block text-kb-text-body hover:text-kb-text">· 금융상품 공시</Link>
              </div>
              <div className="flex-1 border-l border-kb-border pl-6 space-y-2">
                <Link href="#" className="block text-kb-text-body hover:text-kb-text">· 예금수수료 안내</Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
