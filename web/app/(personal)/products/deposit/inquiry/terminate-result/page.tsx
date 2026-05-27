'use client'

import Link from 'next/link'
import { useState } from 'react'
import DepositSidebar from '@/components/products/DepositSidebar'

const PERIOD_BTNS = ['당일', '6개월', '6개월', '1년']

export default function TerminateResultPage() {
  const [startYear, setStartYear] = useState('2026')
  const [startMonth, setStartMonth] = useState('05')
  const [startDay, setStartDay] = useState('25')
  const [endYear, setEndYear] = useState('2026')
  const [endMonth, setEndMonth] = useState('05')
  const [endDay, setEndDay] = useState('25')
  const [searched, setSearched] = useState(false)

  const YEARS = ['2026', '2025', '2024', '2023', '2022']
  const MONTHS = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))
  const DAYS = Array.from({ length: 31 }, (_, i) => String(i + 1).padStart(2, '0'))

  return (
    <div className="max-w-kb-container mx-auto px-6 py-6">
      <div className="flex justify-end mb-3 text-[12px] text-kb-text-muted gap-1 items-center">
        <span>개인뱅킹</span><span>›</span>
        <span>금융상품</span><span>›</span>
        <span>예금</span><span>›</span>
        <Link href="/products/deposit/inquiry/terminate-result" className="hover:underline">해지결과/내역 조회</Link>
        <span>›</span>
        <Link href="#" className="text-kb-blue hover:underline">도움말</Link>
      </div>

      <div className="flex gap-6">
        <DepositSidebar />

        <main className="flex-1 min-w-0">
          <h1 className="text-[20px] font-bold text-kb-text mb-5">해지결과/내역 조회</h1>

          <div className="border border-kb-border bg-[#FAFAFA] px-4 py-3 mb-5 text-[12px] text-kb-text-body space-y-1">
            <p className="flex gap-1.5"><span>-</span><span>조회시작일은 당일 기준으로 5년 이내에서 선택 가능합니다. 단, 1회 조회시 최대기간은 1년입니다.</span></p>
            <p className="flex gap-1.5"><span>-</span><span>예시) 포함일 : 2015.8.20 조회시작일 : 2010.8.20부터 선택가능</span></p>
          </div>

          {/* 필터 */}
          <div className="border border-kb-border px-4 py-4 mb-4">
            <div className="flex items-center gap-3 flex-wrap">
              <span className="text-[13px] font-semibold text-kb-text">조회기간</span>
              <div className="flex gap-1">
                {PERIOD_BTNS.map((b, i) => (
                  <button key={i}
                    onClick={() => setSearched(false)}
                    className="border border-kb-border px-3 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                    {b}
                  </button>
                ))}
              </div>
              <div className="flex items-center gap-1 text-[13px]">
                <select value={startYear} onChange={e => setStartYear(e.target.value)}
                  className="border border-kb-border px-2 py-1 text-[12px] outline-none bg-white">
                  {YEARS.map(y => <option key={y}>{y}</option>)}
                </select>
                <span>년</span>
                <select value={startMonth} onChange={e => setStartMonth(e.target.value)}
                  className="border border-kb-border px-2 py-1 text-[12px] outline-none bg-white w-14">
                  {MONTHS.map(m => <option key={m}>{m}</option>)}
                </select>
                <span>월</span>
                <select value={startDay} onChange={e => setStartDay(e.target.value)}
                  className="border border-kb-border px-2 py-1 text-[12px] outline-none bg-white w-14">
                  {DAYS.map(d => <option key={d}>{d}</option>)}
                </select>
                <span>일</span>
                <span className="mx-1">
                  <svg viewBox="0 0 16 16" fill="none" className="w-4 h-4" stroke="currentColor" strokeWidth="1.5">
                    <rect x="1" y="2" width="14" height="13" rx="1"/><line x1="5" y1="1" x2="5" y2="4"/><line x1="11" y1="1" x2="11" y2="4"/><line x1="1" y1="7" x2="15" y2="7"/>
                  </svg>
                </span>
              </div>
              <span className="text-[13px]">~</span>
              <div className="flex items-center gap-1 text-[13px]">
                <select value={endYear} onChange={e => setEndYear(e.target.value)}
                  className="border border-kb-border px-2 py-1 text-[12px] outline-none bg-white">
                  {YEARS.map(y => <option key={y}>{y}</option>)}
                </select>
                <span>년</span>
                <select value={endMonth} onChange={e => setEndMonth(e.target.value)}
                  className="border border-kb-border px-2 py-1 text-[12px] outline-none bg-white w-14">
                  {MONTHS.map(m => <option key={m}>{m}</option>)}
                </select>
                <span>월</span>
                <select value={endDay} onChange={e => setEndDay(e.target.value)}
                  className="border border-kb-border px-2 py-1 text-[12px] outline-none bg-white w-14">
                  {DAYS.map(d => <option key={d}>{d}</option>)}
                </select>
                <span>일</span>
                <span className="mx-1">
                  <svg viewBox="0 0 16 16" fill="none" className="w-4 h-4" stroke="currentColor" strokeWidth="1.5">
                    <rect x="1" y="2" width="14" height="13" rx="1"/><line x1="5" y1="1" x2="5" y2="4"/><line x1="11" y1="1" x2="11" y2="4"/><line x1="1" y1="7" x2="15" y2="7"/>
                  </svg>
                </span>
              </div>
            </div>
            <div className="flex justify-center mt-3">
              <button
                onClick={() => setSearched(true)}
                className="px-10 py-2 text-[13px] font-bold text-white hover:opacity-90"
                style={{ backgroundColor: '#5BC9A8' }}>
                조회
              </button>
            </div>
          </div>

          {/* 결과 테이블 */}
          <table className="w-full border-collapse text-[13px] border-t-2 border-kb-text">
            <thead>
              <tr className="bg-kb-beige-light">
                {['해지계좌번호', '해지일자', '해지계좌종류', '해지금액', '바로가기'].map(h => (
                  <th key={h} className="border border-kb-border px-4 py-3 font-semibold text-kb-text text-center">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              <tr>
                <td colSpan={5} className="border border-kb-border px-4 py-8 text-center text-[13px] text-kb-text-muted">
                  조회하실 내역이 없습니다.
                </td>
              </tr>
            </tbody>
          </table>
        </main>
      </div>
    </div>
  )
}
