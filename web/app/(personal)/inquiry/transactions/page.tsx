'use client'

import Link from 'next/link'
import { Fragment, useState, useEffect } from 'react'
import InquirySidebar from '@/components/inquiry/InquirySidebar'
import { formatNumber } from '@/lib/mock-data'
import { fetchDepositAccountViewModels, getCurrentDepositCustomerId, fetchTransactions, DepositViewAccount, DepositTransaction } from '@/lib/deposit-api'

const PERIOD_BUTTONS = ['당일', '1주일', '1개월', '3개월', '6개월', '1년']
const MONTH_BUTTONS = ['05월', '04월', '03월']
const YEARS = ['2026', '2025', '2024']
const MONTHS = ['01','02','03','04','05','06','07','08','09','10','11','12']

export default function TransactionsPage() {
  const [selectedAccount, setSelectedAccount] = useState('')
  const [calAccount, setCalAccount] = useState('')
  const [year, setYear] = useState('2026')
  const [month, setMonth] = useState('05')
  const [startDate, setStartDate] = useState('20260521')
  const [endDate, setEndDate] = useState('20260521')
  const [activeTab, setActiveTab] = useState('간편조회')
  const [searched, setSearched] = useState(false)
  const [calSearched, setCalSearched] = useState(false)
  const [expandedTxId, setExpandedTxId] = useState<number | null>(null)
  const [txType, setTxType] = useState<'전체' | '입금' | '출금'>('전체')
  const [sortOrder, setSortOrder] = useState<'최근' | '과거'>('최근')
  const [memoFilter, setMemoFilter] = useState('')
  const [minAmount, setMinAmount] = useState('')
  const [maxAmount, setMaxAmount] = useState('')
  const [senderFilter, setSenderFilter] = useState('')
  const [accounts, setAccounts] = useState<DepositViewAccount[]>([])
  const [allTransactions, setAllTransactions] = useState<DepositTransaction[]>([])
  const [loadingAccounts, setLoadingAccounts] = useState(true)

  useEffect(() => {
    const customerId = getCurrentDepositCustomerId()
    async function loadData() {
      try {
        const accs = await fetchDepositAccountViewModels(customerId)
        setAccounts(accs)
      } catch {
        setAccounts([])
      } finally {
        setLoadingAccounts(false)
      }
      try {
        const txs = await fetchTransactions({ customerId })
        setAllTransactions(txs)
      } catch {
        setAllTransactions([])
      }
    }
    loadData()
  }, [])

  const txs = selectedAccount
    ? allTransactions.filter(t => {
        const acc = accounts.find(a => a.id === selectedAccount)
        return acc?.number === t.accountNumber
      })
    : []

  const totalDeposit  = txs.filter(t => t.directionType === 'IN').reduce((s, t) => s + Number(t.amount), 0)
  const totalWithdraw = txs.filter(t => t.directionType === 'OUT').reduce((s, t) => s + Number(t.amount), 0)
  const selectedAcc   = accounts.find(a => a.id === selectedAccount)

  function handleSearch() { setSearched(true) }

  function setPeriod(p: string) {
    const today = new Date()
    const fmt = (d: Date) =>
      `${d.getFullYear()}${String(d.getMonth()+1).padStart(2,'0')}${String(d.getDate()).padStart(2,'0')}`
    const end = fmt(today)
    const s = new Date(today)
    if      (p === '당일')  { setStartDate(end); setEndDate(end); return }
    else if (p === '1주일') s.setDate(s.getDate()-7)
    else if (p === '1개월') s.setMonth(s.getMonth()-1)
    else if (p === '3개월') s.setMonth(s.getMonth()-3)
    else if (p === '6개월') s.setMonth(s.getMonth()-6)
    else if (p === '1년')   s.setFullYear(s.getFullYear()-1)
    setStartDate(fmt(s)); setEndDate(end)
  }

  /* 달력으로 보기 – 해당 월 거래 */
  const calTxs = (() => {
    if (!calSearched || !calAccount) return []
    const acc = accounts.find(a => a.id === calAccount)
    const prefix = `${year}-${month}`
    return allTransactions.filter(t => acc?.number === t.accountNumber && t.transactionAt.startsWith(prefix))
  })()

  /* 달력 그리드 계산 */
  function buildCalendar() {
    const y = parseInt(year), m = parseInt(month)
    const firstDay = new Date(y, m-1, 1).getDay()
    const daysInMonth = new Date(y, m, 0).getDate()
    return { firstDay, daysInMonth }
  }
  const { firstDay, daysInMonth } = buildCalendar()

  function txsOnDay(day: number) {
    const d = String(day).padStart(2,'0')
    const key = `${year}-${month}-${d}`
    return calTxs.filter(t => t.transactionAt.startsWith(key))
  }

  return (
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">

        {/* ===== 사이드바 ===== */}
        <InquirySidebar />

        {/* ===== 본문 ===== */}
        <main className="flex-1 pl-8 pt-4 pb-12">
          {/* 브레드크럼 */}
          <div className="flex justify-end mb-2 text-[12px] text-kb-text-muted gap-1">
            <span>개인뱅킹</span><span>&gt;</span>
            <span>조회</span><span>&gt;</span>
            <span>거래내역 조회</span><span>&gt;</span>
            <span className="font-semibold text-kb-text">거래내역 조회</span>
          </div>

          <h1 className="text-[20px] font-bold text-kb-text mb-4">거래내역 조회</h1>

          {/* 보안 공지 배너 */}
          <div className="flex items-center gap-4 border border-kb-border p-4 mb-4 bg-[#FAF8F5]">
            <div className="w-16 h-12 bg-orange-500 rounded flex items-center justify-center flex-shrink-0">
              <span className="text-white text-[10px] font-bold text-center leading-tight">보안카드<br/>이용고객</span>
            </div>
            <div>
              <p className="text-[13px] font-bold text-kb-text">
                전자금융 보안등급별 <span className="underline">이체한도</span> 변경안내
              </p>
              <p className="text-[12px] text-kb-text-muted">대상고객 : 보안카드 이용고객 (인터넷뱅킹, 스타뱅킹, 폰뱅킹)</p>
              <p className="text-[12px] text-kb-text-muted">
                시행일자 : 2014.4.15(화){'  '}
                <Link href="#" className="text-kb-blue underline">자세히보기</Link>
              </p>
            </div>
          </div>

          {/* ===== 조회 탭 ===== */}
          <div className="flex mb-4">
            {['간편조회', '상세조회', '달력으로 보기'].map((tab, i) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-5 py-2 text-[13px] border transition-colors ${
                  i > 0 ? '-ml-px' : ''
                } ${
                  activeTab === tab
                    ? 'bg-[#4D4D4D] text-white font-bold border-[#4D4D4D] z-10 relative'
                    : 'bg-[#F5F2EE] border-kb-border text-kb-text-muted hover:bg-kb-beige-light'
                }`}
              >
                {tab}
              </button>
            ))}
          </div>

          {/* ============================= 달력으로 보기 ============================= */}
          {activeTab === '달력으로 보기' && (
            <div>
              <p className="text-[12px] text-kb-text-muted mb-1">
                * 계속해서 다른 계좌의 거래내역을 조회하려면 계좌번호를 선택한 후 [조회] 버튼을 누르세요.
              </p>
              <p className="text-[12px] text-kb-text-muted mb-4">
                * 달력으로 보기는 한 달 단위만 선택할 수 있습니다.
              </p>

              <div className="border border-kb-border">
                <table className="w-full text-[13px]">
                  <tbody>
                    <tr className="border-b border-kb-border">
                      <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[120px] whitespace-nowrap">계좌번호</td>
                      <td className="px-4 py-3">
                        <select
                          value={calAccount}
                          onChange={e => { setCalAccount(e.target.value); setCalSearched(false) }}
                          className="border border-kb-border px-3 py-1.5 text-[13px] w-[340px]"
                        >
                          <option value="">－선택－</option>
                          {loadingAccounts ? (
                            <option disabled>loading...</option>
                          ) : accounts.map(a => (
                            <option key={a.id} value={a.id}>
                              {a.number} : {a.name}
                            </option>
                          ))}
                        </select>
                      </td>
                    </tr>
                    <tr>
                      <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap">조회기간</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <select value={year} onChange={e => { setYear(e.target.value); setCalSearched(false) }}
                            className="border border-kb-border px-2 py-1.5 text-[13px]">
                            {YEARS.map(y => <option key={y}>{y}</option>)}
                          </select>
                          <span className="text-[13px]">년</span>
                          <select value={month} onChange={e => { setMonth(e.target.value); setCalSearched(false) }}
                            className="border border-kb-border px-2 py-1.5 text-[13px]">
                            {MONTHS.map(m => <option key={m}>{m}</option>)}
                          </select>
                          <span className="text-[13px]">월</span>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div className="flex justify-center mt-4 mb-6">
                <button
                  onClick={() => setCalSearched(true)}
                  className="bg-[#555555] text-white px-12 py-2 text-[14px] font-bold hover:bg-[#444]"
                >
                  조회
                </button>
              </div>

              {/* 달력 결과 */}
              {calSearched && (
                <div>
                  <p className="text-[13px] font-bold text-kb-text mb-3">
                    {year}년 {month}월
                  </p>
                  <table className="w-full border-collapse text-[12px] mb-6">
                    <thead>
                      <tr>
                        {['일','월','화','수','목','금','토'].map((d, i) => (
                          <th key={d}
                            className={`border border-kb-border py-2 text-center font-semibold text-[12px] bg-kb-beige-light ${
                              i === 0 ? 'text-red-500' : i === 6 ? 'text-blue-500' : 'text-kb-text'
                            }`}>
                            {d}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {Array.from({ length: Math.ceil((firstDay + daysInMonth) / 7) }).map((_, weekIdx) => (
                        <tr key={weekIdx}>
                          {Array.from({ length: 7 }).map((_, dayIdx) => {
                            const day = weekIdx * 7 + dayIdx - firstDay + 1
                            const valid = day >= 1 && day <= daysInMonth
                            const dayTxs = valid ? txsOnDay(day) : []
                            const deposits  = dayTxs.filter(t => t.directionType === 'IN')
                            const withdraws = dayTxs.filter(t => t.directionType === 'OUT')
                            return (
                              <td key={dayIdx}
                                className={`border border-kb-border align-top p-1.5 h-20 text-[11px] ${
                                  dayIdx === 0 ? 'text-red-500' : dayIdx === 6 ? 'text-blue-500' : 'text-kb-text'
                                }`}>
                                {valid && (
                                  <>
                                    <p className="font-semibold mb-1">{day}</p>
                                    {deposits.map(t => (
                                      <p key={t.transactionId} className="text-blue-600 truncate">{formatNumber(Number(t.amount))}</p>
                                    ))}
                                    {withdraws.map(t => (
                                      <p key={t.transactionId} className="text-red-500 truncate">{formatNumber(Number(t.amount))}</p>
                                    ))}
                                  </>
                                )}
                              </td>
                            )
                          })}
                        </tr>
                      ))}
                    </tbody>
                  </table>

                  {calTxs.length === 0 && (
                    <p className="text-center text-[13px] text-kb-text-muted py-4">해당 월 거래 내역이 없습니다.</p>
                  )}
                </div>
              )}
            </div>
          )}

          {/* ============================= 간편조회 / 상세조회 ============================= */}
          {activeTab !== '달력으로 보기' && (
            <div>
              <p className="text-[12px] text-kb-text-muted mb-4">
                * 계속해서 다른 계좌의 거래내역을 조회하려면 계좌번호를 선택한 후 [조회] 버튼을 누르세요.
              </p>

              <div className="border border-kb-border rounded-xl overflow-hidden">
                <table className="w-full text-[13px]">
                  <tbody>
                    <tr className="border-b border-kb-border">
                      <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[120px] whitespace-nowrap">계좌번호</td>
                      <td className="px-4 py-3">
                        <select
                          value={selectedAccount}
                          onChange={e => { setSelectedAccount(e.target.value); setSearched(false) }}
                          className="border border-kb-border px-3 py-1.5 text-[13px] w-[340px]"
                        >
                          <option value="">－선택－</option>
                          {loadingAccounts ? (
                            <option disabled>loading...</option>
                          ) : accounts.map(a => (
                            <option key={a.id} value={a.id}>
                              {a.number} : {a.name}
                            </option>
                          ))}
                        </select>
                      </td>
                    </tr>
                    {activeTab === '상세조회' && (
                      <>
                        <tr className="border-b border-kb-border">
                          <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap">월별조회</td>
                          <td className="px-4 py-3 flex items-center gap-2">
                            <select value={year} onChange={e => setYear(e.target.value)}
                              className="border border-kb-border px-2 py-1 text-[13px]">
                              {YEARS.map(y => <option key={y}>{y}</option>)}
                            </select>
                            <span>년</span>
                            <select value={month} onChange={e => setMonth(e.target.value)}
                              className="border border-kb-border px-2 py-1 text-[13px]">
                              {MONTHS.map(m => <option key={m}>{m}</option>)}
                            </select>
                            <span>월</span>
                          </td>
                        </tr>
                      </>
                    )}
                    <tr className={activeTab === '상세조회' ? 'border-b border-kb-border' : ''}>
                      <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap align-top pt-4">조회기간</td>
                      <td className="px-4 py-3">
                        <div className="flex flex-wrap gap-1.5 mb-2">
                          {PERIOD_BUTTONS.map(p => (
                            <button key={p} onClick={() => setPeriod(p)}
                              className="border border-kb-border rounded-lg px-4 py-1.5 text-[12px] hover:bg-kb-beige-light">
                              {p}
                            </button>
                          ))}
                          <span className="mx-1 text-kb-border">|</span>
                          {MONTH_BUTTONS.map(m => (
                            <button key={m}
                              className="border border-kb-border rounded-lg px-4 py-1.5 text-[12px] hover:bg-kb-beige-light">
                              {m}
                            </button>
                          ))}
                        </div>
                        <div className="flex items-center gap-2">
                          <input type="text" value={startDate} onChange={e => setStartDate(e.target.value)}
                            className="border border-kb-border px-2 py-1 text-[13px] w-28" />
                          <span className="text-kb-text-muted">📅</span>
                          <span>~</span>
                          <input type="text" value={endDate} onChange={e => setEndDate(e.target.value)}
                            className="border border-kb-border px-2 py-1 text-[13px] w-28" />
                          <span className="text-kb-text-muted">📅</span>
                        </div>
                      </td>
                    </tr>

                    {activeTab === '상세조회' && (
                      <>
                        <tr className="border-b border-kb-border">
                          <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap">조회내용</td>
                          <td className="px-4 py-3 flex items-center gap-5">
                            {(['전체', '입금내용만', '출금내용만'] as const).map((v) => (
                              <label key={v} className="flex items-center gap-1.5 text-[13px] cursor-pointer">
                                <input type="radio" name="txType"
                                  checked={txType === (v === '전체' ? '전체' : v === '입금내용만' ? '입금' : '출금')}
                                  onChange={() => setTxType(v === '전체' ? '전체' : v === '입금내용만' ? '입금' : '출금')}
                                  className="accent-kb-yellow" />
                                {v}
                              </label>
                            ))}
                          </td>
                        </tr>
                        <tr className="border-b border-kb-border">
                          <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap">조회결과 순서</td>
                          <td className="px-4 py-3 flex items-center gap-5">
                            {[['최근', '최근 거래내역이 위로'], ['과거', '과거 거래내역이 위로']] .map(([val, label]) => (
                              <label key={val} className="flex items-center gap-1.5 text-[13px] cursor-pointer">
                                <input type="radio" name="sortOrder"
                                  checked={sortOrder === val}
                                  onChange={() => setSortOrder(val as '최근' | '과거')}
                                  className="accent-kb-yellow" />
                                {label}
                              </label>
                            ))}
                          </td>
                        </tr>
                        <tr className="border-b border-kb-border">
                          <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap">메모 선택</td>
                          <td className="px-4 py-3">
                            <select value={memoFilter} onChange={e => setMemoFilter(e.target.value)}
                              className="border border-kb-border px-3 py-1.5 text-[13px] w-36">
                              <option value="">선택</option>
                              <option value="있음">메모 있음</option>
                              <option value="없음">메모 없음</option>
                            </select>
                          </td>
                        </tr>
                        <tr>
                          <td className="bg-kb-beige-light px-4 py-4 font-semibold text-kb-text whitespace-nowrap align-top">추가검색조건</td>
                          <td className="px-4 py-3 space-y-2">
                            <div className="flex items-center gap-2">
                              <input type="checkbox" id="amtFilter" className="w-3.5 h-3.5" />
                              <label htmlFor="amtFilter" className="text-[13px]">조회대상금액</label>
                              <input type="text" value={minAmount} onChange={e => setMinAmount(e.target.value)}
                                placeholder="(최소)" className="border border-kb-border px-2 py-1 text-[13px] w-24 ml-2" />
                              <span className="text-[13px]">원 ~</span>
                              <input type="text" value={maxAmount} onChange={e => setMaxAmount(e.target.value)}
                                placeholder="(최대)" className="border border-kb-border px-2 py-1 text-[13px] w-24" />
                              <span className="text-[13px]">원</span>
                            </div>
                            <div className="flex items-center gap-2">
                              <input type="checkbox" id="senderFilter" className="w-3.5 h-3.5" />
                              <label htmlFor="senderFilter" className="text-[13px]">보낸분/받는분,송금메모</label>
                              <input type="text" value={senderFilter} onChange={e => setSenderFilter(e.target.value)}
                                className="border border-kb-border px-2 py-1 text-[13px] w-48 ml-2" />
                            </div>
                          </td>
                        </tr>
                      </>
                    )}
                  </tbody>
                </table>
              </div>

              <div className="flex gap-2 mt-4 mb-6">
                <button onClick={handleSearch}
                  className="bg-[#555555] text-white px-10 py-2 text-[14px] font-bold hover:bg-[#444]">
                  조회
                </button>
                <button className="flex items-center gap-1 border border-kb-border px-4 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  상세검색 ˅
                </button>
              </div>

              {/* 조회 결과 */}
              {searched && (
                <div>
                  <p className="text-[12px] text-kb-text-muted text-right mb-3">
                    조회기간 : {startDate.slice(0,4)}.{startDate.slice(4,6)}.{startDate.slice(6)} ~ {endDate.slice(0,4)}.{endDate.slice(4,6)}.{endDate.slice(6)}
                  </p>

                  {selectedAcc && (
                    <div className="border border-kb-border-dark rounded-xl p-6 mb-4">
                      <div className="flex items-start justify-between">
                        <div>
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-[14px] font-bold text-kb-text">{selectedAcc.number}</span>
                            {selectedAcc.badge && (
                              <span className="text-[11px] border border-gray-400 text-gray-500 px-1">{selectedAcc.badge}</span>
                            )}
                          </div>
                          <p className="text-[12px] text-kb-text-muted">{selectedAcc.name}</p>
                          <p className="text-[13px] text-kb-text mt-1">
                            잔액 <span className="font-bold">{formatNumber(selectedAcc.balance)}원</span>
                            <span className="text-kb-text-muted ml-2">(출금가능금액 {formatNumber(selectedAcc.availableBalance)}원)</span>
                          </p>
                          {selectedAcc.createdAt && (
                            <p className="text-[12px] text-kb-text-muted mt-1">* 신규일 : {selectedAcc.createdAt}</p>
                          )}
                        </div>
                        <div className="flex gap-1.5">
                          {['현금카드', '계좌상세정보'].map(btn => (
                            <button key={btn}
                              className="border border-kb-border rounded-lg px-4 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                              {btn}
                            </button>
                          ))}
                          <Link href="/transfer/account"
                            className="bg-kb-yellow px-4 py-1 text-[12px] font-bold text-kb-text hover:brightness-95">
                            이체
                          </Link>
                        </div>
                      </div>
                    </div>
                  )}

                  <div className="flex justify-between text-[13px] text-kb-text-muted mb-3">
                    <span>* 총 입금금액({txs.filter(t=>t.directionType==='IN').length}건) : <span className="text-kb-text font-semibold">{formatNumber(totalDeposit)}원</span></span>
                    <span>* 총 출금금액({txs.filter(t=>t.directionType==='OUT').length}건) : <span className="text-kb-text font-semibold">{formatNumber(totalWithdraw)}원</span></span>
                  </div>

                  <table className="w-full border-collapse text-[12px]">
                    <thead>
                      <tr className="bg-kb-beige-light">
                        {['선택','거래일시','적요','보낸분/받는분','출금액(원)','입금액(원)','잔액(원)','송금메모','거래점'].map(h => (
                          <th key={h} className="border border-kb-border px-2 py-2 text-center font-semibold">{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {txs.length === 0 ? (
                        <tr>
                          <td colSpan={9} className="border border-kb-border py-8 text-center text-kb-text-muted">
                            조회 내역이 없습니다.
                          </td>
                        </tr>
                      ) : txs.map(tx => {
                        const isExpanded = expandedTxId === tx.transactionId
                        const isIn = tx.directionType === 'IN'
                        const amt = Number(tx.amount)
                        return (
                          <Fragment key={tx.transactionId}>
                            <tr
                              onClick={() => setExpandedTxId(isExpanded ? null : tx.transactionId)}
                              className={`cursor-pointer transition-colors ${isExpanded ? 'bg-kb-beige-light' : 'hover:bg-kb-beige-light'}`}
                            >
                              <td className="border border-kb-border px-2 py-2 text-center" onClick={e => e.stopPropagation()}>
                                <input type="checkbox" />
                              </td>
                              <td className="border border-kb-border px-2 py-2 text-center">{tx.transactionAt}</td>
                              <td className="border border-kb-border px-2 py-2">{tx.transactionSummary || tx.transactionType}</td>
                              <td className="border border-kb-border px-2 py-2 text-center"></td>
                              <td className="border border-kb-border px-2 py-2 text-right text-kb-red">
                                {!isIn ? formatNumber(amt) : ''}
                              </td>
                              <td className="border border-kb-border px-2 py-2 text-right text-kb-blue">
                                {isIn ? formatNumber(amt) : ''}
                              </td>
                              <td className="border border-kb-border px-2 py-2 text-right">-</td>
                              <td className="border border-kb-border px-2 py-2 text-center">{tx.transactionMemo ?? ''}</td>
                              <td className="border border-kb-border px-2 py-2 text-center"></td>
                            </tr>
                            {isExpanded && (
                              <tr key={`${tx.transactionId}-detail`} className="bg-[#F5F8FF]">
                                <td colSpan={9} className="border border-kb-border px-6 py-4">
                                  <div className="flex gap-10 text-[12px]">
                                    <div className="space-y-1.5">
                                      <div className="flex gap-3">
                                        <span className="text-kb-text-muted w-20">거래일시</span>
                                        <span className="text-kb-text font-medium">{tx.transactionAt}</span>
                                      </div>
                                      <div className="flex gap-3">
                                        <span className="text-kb-text-muted w-20">거래구분</span>
                                        <span className={isIn ? 'text-kb-blue font-medium' : 'text-kb-red font-medium'}>
                                          {isIn ? '입금' : '출금'}
                                        </span>
                                      </div>
                                      <div className="flex gap-3">
                                        <span className="text-kb-text-muted w-20">거래금액</span>
                                        <span className="text-kb-text font-medium">
                                          {formatNumber(amt)}원
                                        </span>
                                      </div>
                                    </div>
                                    <div className="space-y-1.5">
                                      <div className="flex gap-3">
                                        <span className="text-kb-text-muted w-20">적요</span>
                                        <span className="text-kb-text">{tx.transactionSummary || tx.transactionType}</span>
                                      </div>
                                      {tx.transactionMemo && (
                                        <div className="flex gap-3">
                                          <span className="text-kb-text-muted w-20">송금메모</span>
                                          <span className="text-kb-text">{tx.transactionMemo}</span>
                                        </div>
                                      )}
                                      <div className="flex gap-3">
                                        <span className="text-kb-text-muted w-20">거래유형</span>
                                        <span className="text-kb-text">{tx.transactionType}</span>
                                      </div>
                                    </div>
                                    <div className="ml-auto flex items-start gap-2 pt-1">
                                      <button className="border border-kb-border px-3 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light rounded">
                                        거래영수증
                                      </button>
                                      <button className="border border-kb-border px-3 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light rounded">
                                        메모 수정
                                      </button>
                                    </div>
                                  </div>
                                </td>
                              </tr>
                            )}
                          </Fragment>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
