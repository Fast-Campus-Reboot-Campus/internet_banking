'use client'

import Link from 'next/link'
import { useState } from 'react'
import ManageSidebar from '@/components/inquiry/ManageSidebar'

const DAILY_LIMIT   = 1_000_000
const ONCE_LIMIT    = 1_000_000
const USED_TODAY    = 50
const REMAIN_TODAY  = DAILY_LIMIT - USED_TODAY

function formatN(n: number) { return n.toLocaleString('ko-KR') }

export default function TransferLimitPage() {
  const [view, setView] = useState<'inquiry' | 'reduce'>('inquiry')
  const [dailyInput, setDailyInput] = useState('')
  const [onceInput, setOnceInput] = useState('')
  const [done, setDone] = useState(false)

  const now = new Date(2026, 4, 25, 1, 45, 45)
  const datetime = `${now.getFullYear()}. ${String(now.getMonth()+1).padStart(2,'0')}. ${String(now.getDate()).padStart(2,'0')} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}:${String(now.getSeconds()).padStart(2,'0')}`

  function handleReduce() {
    if (!dailyInput && !onceInput) return
    setDone(true)
  }

  return (
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">
        <ManageSidebar />

        {/* ===== 본문 ===== */}
        <main className="flex-1 pl-8 pt-4 pb-12">
          {/* 브레드크럼 */}
          <div className="flex justify-end mb-2 text-[12px] text-kb-text-muted gap-1">
            <span>개인뱅킹</span><span>›</span>
            <span>뱅킹관리</span><span>›</span>
            <span>계좌관리</span><span>›</span>
            <span className="text-kb-text font-semibold">이체한도 조회/감액</span>
            <span className="ml-2">
              <Link href="#" className="text-kb-blue hover:underline">? 도움말</Link>
            </span>
          </div>

          {/* ==================== 조회 화면 ==================== */}
          {view === 'inquiry' && (
            <>
              <h1 className="text-[20px] font-bold text-kb-text mb-4">이체한도 조회/감액</h1>

              {/* 안내 박스 */}
              <div className="border border-kb-border bg-[#FAFAFA] px-4 py-4 mb-5 text-[12px] text-kb-text-body space-y-2">
                <p className="flex gap-1.5">
                  <span className="flex-shrink-0">·</span>
                  <span>이체한도 증액은 본인이 직접 신분증 지참 후 영업점에 방문하시거나, AXful뱅킹 로그인 후 &apos;이체한도 조회/변경&apos; 메뉴에서 본인인증을 통해 처리하실 수 있습니다.</span>
                </p>
                <p className="flex gap-1.5">
                  <span className="flex-shrink-0">·</span>
                  <span>단, 기존 보안카드/OTP 이용 고객님은 각 보안매체별 최대한도까지 이체한도를 변경하실 수 있습니다.</span>
                </p>
                <p className="flex gap-1.5">
                  <span className="flex-shrink-0">·</span>
                  <span>금융거래한도계좌는 인터넷뱅킹 이체한도와 한도계좌 중 낮은 이체한도가 적용됩니다.<br />
                    (2016.3.1까지 개설 계좌: 한도제한 70만원, 2016.3.2이후 개설 계좌: 한도제한 100만원(단, 한도유지 신청 계좌: 한도제한 30만원))</span>
                </p>
                <p className="flex gap-1.5 items-center">
                  <span className="flex-shrink-0">·</span>
                  <span>보안매체별 상세한도를 확인하시기 바랍니다.</span>
                  <button className="border border-kb-border px-3 py-0.5 text-[11px] text-kb-text-body hover:bg-kb-beige-light ml-1">자세히보기</button>
                </p>
              </div>

              {/* 조회기준일시 */}
              <p className="text-right text-[12px] text-kb-text-muted mb-3">조회기준일시 : {datetime}</p>

              {/* 한도 정보 */}
              <div className="border border-kb-border mb-5">
                <table className="w-full text-[13px]">
                  <tbody>
                    {[
                      { label: '1일이체한도', value: `${formatN(DAILY_LIMIT)}원` },
                      { label: '1회이체한도', value: `${formatN(ONCE_LIMIT)}원` },
                      { label: '당일이체금액합계', value: `${formatN(USED_TODAY)}원` },
                      { label: '당일이체잔여한도', value: `${formatN(REMAIN_TODAY)}원` },
                    ].map(row => (
                      <tr key={row.label} className="border-b border-kb-border last:border-b-0">
                        <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[180px] whitespace-nowrap">{row.label}</td>
                        <td className="px-4 py-3 text-kb-text-body">{row.value}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* 버튼 */}
              <div className="flex justify-center gap-2">
                <button
                  onClick={() => alert('당행/타행이체 페이지로 이동합니다.')}
                  className="border border-kb-border px-8 py-2.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  당행/타행이체
                </button>
                <button
                  onClick={() => { setView('reduce'); setDone(false); setDailyInput(''); setOnceInput('') }}
                  className="border border-kb-border px-8 py-2.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  이체한도감액
                </button>
              </div>
            </>
          )}

          {/* ==================== 감액 화면 ==================== */}
          {view === 'reduce' && (
            <>
              <h1 className="text-[20px] font-bold text-kb-text mb-4">이체한도 감액</h1>

              {/* 안내 박스 */}
              <div className="border border-kb-border bg-[#FAFAFA] px-4 py-4 mb-5 text-[12px] text-kb-text-body space-y-2">
                <p className="flex gap-1.5">
                  <span className="flex-shrink-0">·</span>
                  <span>1일 및 1회 이체한도를 확인하시고 감액할 1일 및 1회 이체한도를 만원 단위로 입력하세요.</span>
                </p>
                <p className="flex gap-1.5">
                  <span className="flex-shrink-0">·</span>
                  <span>1회 이체한도는 1일 이체한도를 초과할 수 없습니다.</span>
                </p>
                <p className="flex gap-1.5">
                  <span className="flex-shrink-0">·</span>
                  <span>이체한도 감액하면 다시 증액하실 때는 지정 방문 또는 AXful뱅킹을 통해 가능합니다.</span>
                </p>
                <p className="flex gap-1.5">
                  <span className="flex-shrink-0">※</span>
                  <span>AXful뱅킹 경로: [AXful뱅킹 전체메뉴 &gt; 이체 &gt; 이체한도 조회/변경]</span>
                </p>
              </div>

              {/* 감액 테이블 */}
              <div className="border border-kb-border mb-5">
                <table className="w-full text-[13px] border-collapse">
                  <thead>
                    <tr className="bg-kb-beige-light">
                      <th className="border border-kb-border px-4 py-2.5 font-semibold text-kb-text w-[160px]"></th>
                      <th className="border border-kb-border px-4 py-2.5 font-semibold text-kb-text text-center">감액 전</th>
                      <th className="border border-kb-border px-4 py-2.5 font-semibold text-kb-text text-center">감액 후</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-b border-kb-border">
                      <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap">1일 이체한도</td>
                      <td className="px-4 py-3 text-center text-kb-text-body">{formatN(DAILY_LIMIT)}원</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <input
                            type="text"
                            value={dailyInput}
                            onChange={e => setDailyInput(e.target.value.replace(/[^0-9]/g, ''))}
                            placeholder=""
                            className="border border-kb-border px-3 py-1.5 text-[13px] w-36 outline-none text-right"
                          />
                          <span className="text-kb-text-body">원</span>
                          <span className="text-[11px] text-kb-text-muted flex items-center gap-1">
                            <span className="border border-kb-border rounded-full w-4 h-4 flex items-center justify-center text-[9px]">i</span>
                            1만원 단위로 입력해 주세요
                          </span>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap">1회 이체한도</td>
                      <td className="px-4 py-3 text-center text-kb-text-body">{formatN(ONCE_LIMIT)}원</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <input
                            type="text"
                            value={onceInput}
                            onChange={e => setOnceInput(e.target.value.replace(/[^0-9]/g, ''))}
                            placeholder=""
                            className="border border-kb-border px-3 py-1.5 text-[13px] w-36 outline-none text-right"
                          />
                          <span className="text-kb-text-body">원</span>
                          <span className="text-[11px] text-kb-text-muted flex items-center gap-1">
                            <span className="border border-kb-border rounded-full w-4 h-4 flex items-center justify-center text-[9px]">i</span>
                            1만원 단위로 입력해 주세요
                          </span>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              {/* 변경 버튼 */}
              <div className="flex justify-center mb-6">
                {done ? (
                  <div className="text-center">
                    <p className="text-[14px] font-bold text-kb-text mb-3">이체한도 감액이 완료되었습니다.</p>
                    <button
                      onClick={() => setView('inquiry')}
                      className="border border-kb-border px-8 py-2.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                      확인
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={handleReduce}
                    className="bg-kb-yellow px-12 py-2.5 text-[14px] font-bold text-kb-text hover:brightness-95">
                    변경
                  </button>
                )}
              </div>

              {/* 하단 주의사항 */}
              <div className="border border-kb-border bg-[#FAFAFA] px-4 py-4 text-[12px] text-kb-text-body space-y-2">
                <p className="flex gap-1.5">
                  <span className="flex-shrink-0">·</span>
                  <span>보안카드 이용하시는 고객님께서는 1일/1회 이체한도를 1만원 이하로만 설정하실 수 있습니다.</span>
                </p>
                <p className="flex gap-1.5">
                  <span className="flex-shrink-0">·</span>
                  <span>보안콤 1종을 이체한도 1일 5억원/1회 1억원을 초과하여 설정하시려면 지점을 방문하여 이체한도 초과 약정 거래를 신청해 주시기 바랍니다.</span>
                </p>
              </div>

              {/* 뒤로가기 */}
              <div className="flex justify-center mt-5">
                <button
                  onClick={() => setView('inquiry')}
                  className="border border-kb-border px-8 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  이체한도 조회로 돌아가기
                </button>
              </div>
            </>
          )}
        </main>
      </div>
    </div>
  )
}
