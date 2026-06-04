'use client'
import { KB_MINT,KB_PRIMARY,KB_PRIMARY_BG } from '@/lib/theme'

import Link from 'next/link'
import { useState } from 'react'

const DAILY_LIMIT  = 1_000_000
const ONCE_LIMIT   = 1_000_000
const USED_TODAY   = 50
const REMAIN_TODAY = DAILY_LIMIT - USED_TODAY

function formatN(n: number) { return n.toLocaleString('ko-KR') }

const NOTICES_INQUIRY = [
  '이체한도 증액은 본인이 직접 신분증 지참 후 영업점에 방문하시거나, AXful뱅킹 로그인 후 \'이체한도 조회/변경\' 메뉴에서 본인인증을 통해 처리하실 수 있습니다.',
  '이체한도는 AXful인증서 또는 공동인증서를 통해 변경하실 수 있습니다.',
  '금융거래한도계좌는 인터넷뱅킹 이체한도와 한도계좌 중 낮은 이체한도가 적용됩니다. (2016.3.1까지 개설 계좌: 한도제한 70만원, 2016.3.2이후 개설 계좌: 한도제한 100만원)',
]

const NOTICES_REDUCE = [
  '1일 및 1회 이체한도를 확인하시고 감액할 1일 및 1회 이체한도를 만원 단위로 입력하세요.',
  '1회 이체한도는 1일 이체한도를 초과할 수 없습니다.',
  '이체한도 감액하면 다시 증액하실 때는 지정 방문 또는 AXful뱅킹을 통해 가능합니다.',
  '※ AXful뱅킹 경로: [AXful뱅킹 전체메뉴 > 이체 > 이체한도 조회/변경]',
]

const NOTICES_BOTTOM = [
  '이체한도 설정 시 인증서 인증이 필요합니다.',
  '보안콤 1종을 이체한도 1일 5억원/1회 1억원을 초과하여 설정하시려면 지점을 방문하여 이체한도 초과 약정 거래를 신청해 주시기 바랍니다.',
]

function NoticeBox({ items }: { items: string[] }) {
  return (
    <div className="bg-kb-primary-bg border border-kb-border rounded-xl px-5 py-4 space-y-2">
      {items.map((text, i) => (
        <div key={i} className="flex items-start gap-2 text-[13px] text-kb-text-body">
          <span className="flex-shrink-0 mt-1.5 w-1.5 h-1.5 rounded-full" style={{ backgroundColor: KB_MINT }} />
          {text}
        </div>
      ))}
    </div>
  )
}

export default function TransferLimitPage() {
  const [view, setView] = useState<'inquiry' | 'reduce'>('inquiry')
  const [dailyInput, setDailyInput] = useState('')
  const [onceInput, setOnceInput]   = useState('')
  const [done, setDone]             = useState(false)

  const now = new Date(2026, 4, 25, 1, 45, 45)
  const datetime = `${now.getFullYear()}. ${String(now.getMonth()+1).padStart(2,'0')}. ${String(now.getDate()).padStart(2,'0')} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}:${String(now.getSeconds()).padStart(2,'0')}`

  function handleReduce() {
    if (!dailyInput && !onceInput) return
    setDone(true)
  }

  return (
    <div className="max-w-kb-container mx-auto px-6 py-8">

      {/* 페이지 제목 */}
      <h1 className="text-[24px] font-bold text-kb-text mb-2">이체한도 조회/변경</h1>
      <p className="text-[14px] text-kb-text-muted mb-8">
        현재 설정된 이체한도를 조회하고 감액할 수 있습니다.
      </p>

      {/* ── 조회 화면 ── */}
      {view === 'inquiry' && (
        <div className="space-y-5">
          <NoticeBox items={NOTICES_INQUIRY} />

          <p className="text-right text-[12px] text-kb-text-muted">조회기준일시 : {datetime}</p>

          {/* 한도 테이블 */}
          <div className="border border-kb-border rounded-xl overflow-hidden">
            <table className="w-full text-[14px]">
              <tbody>
                {[
                  { label: '1일 이체한도',      value: `${formatN(DAILY_LIMIT)}원` },
                  { label: '1회 이체한도',      value: `${formatN(ONCE_LIMIT)}원` },
                  { label: '당일 이체금액 합계', value: `${formatN(USED_TODAY)}원` },
                  { label: '당일 이체 잔여한도', value: `${formatN(REMAIN_TODAY)}원` },
                ].map((row, i, arr) => (
                  <tr key={row.label} className={i < arr.length - 1 ? 'border-b border-kb-border' : ''}>
                    <td className="px-5 py-3.5 font-semibold text-kb-text w-48 whitespace-nowrap"
                      style={{ backgroundColor: KB_PRIMARY_BG }}>
                      {row.label}
                    </td>
                    <td className="px-5 py-3.5 text-kb-text-body">{row.value}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="flex gap-3">
            <Link href="/transfer/account"
              className="px-6 py-2.5 text-[14px] font-semibold rounded-lg border-2 transition-colors hover:bg-kb-primary-bg"
              style={{ borderColor: KB_PRIMARY, color: KB_PRIMARY }}>
              당행/타행이체
            </Link>
            <button
              onClick={() => { setView('reduce'); setDone(false); setDailyInput(''); setOnceInput('') }}
              className="px-6 py-2.5 text-[14px] font-bold text-white rounded-lg transition-opacity hover:opacity-85"
              style={{ backgroundColor: KB_PRIMARY }}>
              이체한도 감액
            </button>
          </div>
        </div>
      )}

      {/* ── 감액 화면 ── */}
      {view === 'reduce' && (
        <div className="space-y-5">
          <NoticeBox items={NOTICES_REDUCE} />

          {/* 감액 테이블 */}
          <div className="border border-kb-border rounded-xl overflow-hidden">
            <table className="w-full text-[14px] border-collapse">
              <thead>
                <tr style={{ backgroundColor: KB_PRIMARY }}>
                  <th className="px-5 py-3 font-semibold text-white text-left w-44"></th>
                  <th className="px-5 py-3 font-semibold text-white text-center border-l border-white/20">감액 전</th>
                  <th className="px-5 py-3 font-semibold text-white text-center border-l border-white/20">감액 후</th>
                </tr>
              </thead>
              <tbody>
                {[
                  { label: '1일 이체한도', current: DAILY_LIMIT, input: dailyInput, onChange: (v: string) => setDailyInput(v.replace(/[^0-9]/g, '')) },
                  { label: '1회 이체한도', current: ONCE_LIMIT,  input: onceInput,  onChange: (v: string) => setOnceInput(v.replace(/[^0-9]/g, '')) },
                ].map((row, i, arr) => (
                  <tr key={row.label} className={i < arr.length - 1 ? 'border-b border-kb-border' : ''}>
                    <td className="px-5 py-3.5 font-semibold text-kb-text whitespace-nowrap"
                      style={{ backgroundColor: KB_PRIMARY_BG }}>
                      {row.label}
                    </td>
                    <td className="px-5 py-3.5 text-center text-kb-text-body">{formatN(row.current)}원</td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-2">
                        <input
                          type="text"
                          value={row.input}
                          onChange={e => row.onChange(e.target.value)}
                          placeholder="0"
                          className="border border-kb-border rounded-lg px-3 py-1.5 text-[14px] w-36 outline-none text-right focus:border-kb-primary transition-colors"
                        />
                        <span className="text-kb-text-body">원</span>
                        <span className="text-[12px] text-kb-text-muted">1만원 단위</span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {done ? (
            <div className="text-center py-4 space-y-3">
              <p className="text-[15px] font-bold" style={{ color: KB_PRIMARY }}>이체한도 감액이 완료되었습니다.</p>
              <button onClick={() => setView('inquiry')}
                className="px-8 py-2.5 text-[14px] font-bold text-white rounded-lg transition-opacity hover:opacity-85"
                style={{ backgroundColor: KB_PRIMARY }}>
                확인
              </button>
            </div>
          ) : (
            <div className="flex gap-3">
              <button onClick={() => setView('inquiry')}
                className="px-6 py-2.5 text-[14px] font-semibold rounded-lg border-2 transition-colors hover:bg-kb-primary-bg"
                style={{ borderColor: KB_PRIMARY, color: KB_PRIMARY }}>
                이체한도 조회로 돌아가기
              </button>
              <button onClick={handleReduce}
                className="px-8 py-2.5 text-[14px] font-bold text-white rounded-lg transition-opacity hover:opacity-85"
                style={{ backgroundColor: KB_PRIMARY }}>
                변경
              </button>
            </div>
          )}

          <NoticeBox items={NOTICES_BOTTOM} />
        </div>
      )}
    </div>
  )
}
