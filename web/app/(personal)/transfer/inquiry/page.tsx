'use client'

import Link from 'next/link'
import { useState, useEffect } from 'react'
import { formatNumber } from '@/lib/mock-data'
import { fetchDepositAccountViewModels, fetchTransactions, getCurrentDepositCustomerId } from '@/lib/deposit-api'
import TransferSidebar from '@/components/inquiry/TransferSidebar'

const TABS = ['즉시이체 결과조회', '예약이체 조회', '연락이체 조회', '지연이체 조회', '리브마니보내기 결과조회']

type ResultRow = { id: string; datetime: string; bank: string; account: string; receiver: string; amount: number; memo: string }

export default function TransferInquiryPage() {
  const [activeTab, setActiveTab] = useState('즉시이체 결과조회')
  const [fromAccount, setFromAccount] = useState('')
  const [startDate, setStartDate] = useState('20260519')
  const [endDate, setEndDate] = useState('20260525')
  const [counterAccount, setCounterAccount] = useState('')
  const [useCounter, setUseCounter] = useState(false)
  const [searched, setSearched] = useState(false)
  const [checkedRows, setCheckedRows] = useState<Set<string>>(new Set())
  const [localResults, setLocalResults] = useState<ResultRow[]>([])
  const [accounts, setAccounts] = useState<{ id: string; number: string; name: string }[]>([])
  const [apiResults, setApiResults] = useState<ResultRow[]>([])

  useEffect(() => {
    async function load() {
      try {
        const raw = localStorage.getItem('transferHistory')
        if (raw) setLocalResults(JSON.parse(raw))
      } catch {}
      try {
        const accs = await fetchDepositAccountViewModels(getCurrentDepositCustomerId())
        const mapped = accs.map(a => ({ id: a.id, number: a.number, name: a.name }))
        setAccounts(mapped)
        if (mapped.length > 0) setFromAccount(mapped[0].number)
      } catch {}
      try {
        const txs = await fetchTransactions({ customerId: getCurrentDepositCustomerId() })
        const rows: ResultRow[] = txs
          .filter(t => t.transactionType === 'TRANSFER' && t.directionType === 'OUT')
          .map(t => ({
            id: String(t.transactionId),
            datetime: t.transactionAt?.replace('T', '\n').slice(0, 19) || '',
            bank: t.counterpartyBankName || 'AXful',
            account: t.counterpartyAccountNo || '-',
            receiver: t.counterpartyName || t.transactionSummary || '-',
            amount: Number(t.amount),
            memo: t.transactionMemo || '',
          }))
        setApiResults(rows)
      } catch {}
    }
    load()
  }, [])

  const displayResults: ResultRow[] = apiResults.length > 0 ? apiResults : localResults.length > 0 ? localResults : []

  function toggleRow(id: string) {
    setCheckedRows(prev => {
      const next = new Set(prev)
      if (next.has(id)) { next.delete(id) } else { next.add(id) }
      return next
    })
  }

  function toggleAll(checked: boolean) {
    setCheckedRows(checked ? new Set(displayResults.map(r => r.id)) : new Set())
  }

  const allChecked = checkedRows.size === displayResults.length

  const displayFrom = startDate.length === 8
    ? `${startDate.slice(0,4)}.${startDate.slice(4,6)}.${startDate.slice(6,8)}`
    : startDate
  const displayTo = endDate.length === 8
    ? `${endDate.slice(0,4)}.${endDate.slice(4,6)}.${endDate.slice(6,8)}`
    : endDate

  function applyPeriod(days: number) {
    const to = new Date(2026, 4, 25)
    const from = new Date(to)
    from.setDate(to.getDate() - days)
    const fmt = (d: Date) =>
      `${d.getFullYear()}${String(d.getMonth()+1).padStart(2,'0')}${String(d.getDate()).padStart(2,'0')}`
    setStartDate(fmt(from))
    setEndDate(fmt(to))
  }

  function applyMonth(month: number) {
    setStartDate(`2026${String(month).padStart(2,'0')}01`)
    setEndDate(`2026${String(month).padStart(2,'0')}31`)
  }

  return (
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">
        <TransferSidebar />

        {/* ===== 본문 ===== */}
        <main className="flex-1 pl-8 pt-4 pb-12">
          {/* 브레드크럼 */}
          <div className="flex justify-end mb-2 text-[12px] text-kb-text-muted gap-1">
            <span>개인뱅킹</span><span>›</span>
            <span>이체</span><span>›</span>
            <span>이체결과 조회</span><span>›</span>
            <span>계좌이체결과 조회</span><span>›</span>
            <Link href="#" className="text-kb-blue hover:underline">? 도움말</Link>
          </div>

          <h1 className="text-[20px] font-bold text-kb-text mb-4">계좌이체결과 조회</h1>

          {/* 탭 */}
          <div className="flex border-b border-kb-border mb-5">
            {TABS.map(tab => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-5 py-2.5 text-[13px] border-b-2 -mb-px transition-colors ${
                  activeTab === tab
                    ? 'border-kb-text font-bold text-kb-text bg-white'
                    : 'border-transparent bg-kb-beige-light text-kb-text-muted hover:text-kb-text'
                }`}
              >
                {tab}
              </button>
            ))}
          </div>

          {/* 조회 폼 */}
          <div className="border border-kb-border mb-5">
            <table className="w-full text-[13px]">
              <tbody>
                {/* 출금계좌번호 */}
                <tr className="border-b border-kb-border">
                  <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[140px] whitespace-nowrap">출금계좌번호</td>
                  <td className="px-4 py-3">
                    <select
                      value={fromAccount}
                      onChange={e => setFromAccount(e.target.value)}
                      className="border border-kb-border px-3 py-1.5 text-[13px] w-[280px] outline-none bg-white"
                    >
                      {MOCK_ACCOUNTS.map(a => (
                        <option key={a.id} value={a.number}>{a.number}</option>
                      ))}
                    </select>
                  </td>
                </tr>

                {/* 조회기간 */}
                <tr className="border-b border-kb-border">
                  <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap">조회기간</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1 mb-2 flex-wrap">
                      {[['당일',0],['1주일',7],['1개월',30],['3개월',90],['6개월',180]].map(([label, days]) => (
                        <button key={label as string}
                          onClick={() => applyPeriod(days as number)}
                          className="border border-kb-border px-3 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                          {label}
                        </button>
                      ))}
                      <div className="w-px h-4 bg-kb-border mx-1" />
                      {[5, 4, 3].map(m => (
                        <button key={m}
                          onClick={() => applyMonth(m)}
                          className="border border-kb-border px-3 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                          {m.toString().padStart(2,'0')}월
                        </button>
                      ))}
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="flex items-center gap-1">
                        <input
                          type="text"
                          value={startDate}
                          onChange={e => setStartDate(e.target.value)}
                          maxLength={8}
                          className="border border-kb-border px-2 py-1.5 text-[13px] w-28 outline-none"
                          placeholder="YYYYMMDD"
                        />
                        <button className="border border-kb-border px-2 py-1.5 text-kb-text-muted hover:bg-kb-beige-light">
                          <svg viewBox="0 0 16 16" fill="none" className="w-4 h-4" stroke="currentColor" strokeWidth="1.5">
                            <rect x="1" y="2" width="14" height="13" rx="1"/><line x1="5" y1="1" x2="5" y2="4"/><line x1="11" y1="1" x2="11" y2="4"/><line x1="1" y1="7" x2="15" y2="7"/>
                          </svg>
                        </button>
                      </div>
                      <span className="text-kb-text-muted">~</span>
                      <div className="flex items-center gap-1">
                        <input
                          type="text"
                          value={endDate}
                          onChange={e => setEndDate(e.target.value)}
                          maxLength={8}
                          className="border border-kb-border px-2 py-1.5 text-[13px] w-28 outline-none"
                          placeholder="YYYYMMDD"
                        />
                        <button className="border border-kb-border px-2 py-1.5 text-kb-text-muted hover:bg-kb-beige-light">
                          <svg viewBox="0 0 16 16" fill="none" className="w-4 h-4" stroke="currentColor" strokeWidth="1.5">
                            <rect x="1" y="2" width="14" height="13" rx="1"/><line x1="5" y1="1" x2="5" y2="4"/><line x1="11" y1="1" x2="11" y2="4"/><line x1="1" y1="7" x2="15" y2="7"/>
                          </svg>
                        </button>
                      </div>
                    </div>
                  </td>
                </tr>

                {/* 상대 입금계좌 */}
                <tr>
                  <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap">상대 입금계좌</td>
                  <td className="px-4 py-3 flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={useCounter}
                      onChange={e => setUseCounter(e.target.checked)}
                      className="w-4 h-4"
                    />
                    <input
                      type="text"
                      value={counterAccount}
                      onChange={e => setCounterAccount(e.target.value)}
                      disabled={!useCounter}
                      className="border border-kb-border px-3 py-1.5 text-[13px] w-44 outline-none disabled:bg-kb-beige-light"
                    />
                  </td>
                </tr>
              </tbody>
            </table>
            <div className="flex justify-center py-4 border-t border-kb-border">
              <button
                onClick={() => setSearched(true)}
                className="bg-kb-yellow px-10 py-2 text-[13px] font-bold text-kb-text hover:brightness-95"
              >
                조회
              </button>
            </div>
          </div>

          {/* 결과 영역 */}
          {searched && (
            <>
              <div className="border border-kb-border px-4 py-3 mb-2 text-[13px]">
                <span className="text-kb-text-muted mr-1">*</span>
                <span className="text-kb-text-muted">계좌번호 : </span>
                <Link href="#" className="text-kb-blue underline">{fromAccount}</Link>
              </div>

              <div className="text-right text-[12px] text-kb-text-muted mb-1">
                조회기간 : {displayFrom} ~ {displayTo}
              </div>

              <div className="overflow-x-auto mb-1">
                <table className="w-full border-collapse text-[13px] border-t-2 border-kb-text">
                  <thead>
                    <tr className="bg-kb-beige-light">
                      <th className="border border-kb-border px-2 py-2 text-center w-8">
                        <input type="checkbox" checked={allChecked} onChange={e => toggleAll(e.target.checked)} className="w-4 h-4" />
                      </th>
                      <th className="border border-kb-border px-3 py-2 text-center font-semibold whitespace-nowrap">이체일시</th>
                      <th className="border border-kb-border px-3 py-2 text-center font-semibold whitespace-nowrap">입금은행</th>
                      <th className="border border-kb-border px-3 py-2 text-center font-semibold whitespace-nowrap">입금계좌번호</th>
                      <th className="border border-kb-border px-3 py-2 text-center font-semibold whitespace-nowrap">받는분</th>
                      <th className="border border-kb-border px-3 py-2 text-center font-semibold whitespace-nowrap">이체금액</th>
                      <th className="border border-kb-border px-3 py-2 text-center font-semibold whitespace-nowrap">출금통장표시내용</th>
                    </tr>
                  </thead>
                  <tbody>
                    {displayResults.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="border border-kb-border px-3 py-8 text-center text-[13px] text-kb-text-muted">
                          조회된 이체 내역이 없습니다.
                        </td>
                      </tr>
                    ) : displayResults.map(row => (
                      <tr key={row.id} className="hover:bg-kb-beige-light">
                        <td className="border border-kb-border px-2 py-3 text-center">
                          <input type="checkbox" checked={checkedRows.has(row.id)} onChange={() => toggleRow(row.id)} className="w-4 h-4" />
                        </td>
                        <td className="border border-kb-border px-3 py-3 text-center whitespace-pre-line text-[12px]">{row.datetime}</td>
                        <td className="border border-kb-border px-3 py-3 text-center">{row.bank}</td>
                        <td className="border border-kb-border px-3 py-3 text-center">{row.account}</td>
                        <td className="border border-kb-border px-3 py-3 text-center">{row.receiver}</td>
                        <td className="border border-kb-border px-3 py-3 text-right pr-4">{formatNumber(row.amount)}</td>
                        <td className="border border-kb-border px-3 py-3 text-center text-kb-text-muted">{row.memo}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* 페이지네이션 */}
              <div className="flex items-center justify-between mb-5">
                <div className="flex items-center gap-1">
                  {['|<', '<', '>', '>|'].map(btn => (
                    <button key={btn}
                      className="border border-kb-border px-2 py-1 text-[12px] text-kb-text-muted hover:bg-kb-beige-light">
                      {btn}
                    </button>
                  ))}
                  <span className="ml-2 text-[12px] text-kb-text-muted">
                    현재 1-{displayResults.length}건 / {displayResults.length}건
                  </span>
                </div>
              </div>

              {/* 하단 버튼 */}
              <div className="flex justify-center gap-2">
                <button className="border border-kb-border px-5 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light flex items-center gap-1">
                  <svg viewBox="0 0 16 16" fill="none" className="w-3.5 h-3.5" stroke="currentColor" strokeWidth="1.5">
                    <rect x="2" y="2" width="12" height="12" rx="1"/><line x1="8" y1="5" x2="8" y2="11"/><line x1="5" y1="8" x2="11" y2="8"/>
                  </svg>
                  저장
                </button>
                <button className="border border-kb-border px-5 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  이체확인증 건별 출력
                </button>
                <button className="border border-kb-border px-5 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light flex items-center gap-1">
                  이체확인증 일괄 출력
                  <svg viewBox="0 0 12 12" fill="none" className="w-3 h-3" stroke="currentColor" strokeWidth="1.5">
                    <path d="M2 10L10 2M10 2H5M10 2v5"/>
                  </svg>
                </button>
                <button className="border border-kb-border px-5 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  이체결과 전송
                </button>
              </div>
            </>
          )}
        </main>
      </div>
    </div>
  )
}
