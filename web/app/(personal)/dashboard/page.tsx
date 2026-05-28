'use client'

import Link from 'next/link'
import { useState, useEffect } from 'react'
import { formatNumber } from '@/lib/mock-data'
import { fetchDepositAccountViewModels, getCurrentDepositCustomerId, fetchTransactions, DepositTransaction, DepositViewAccount } from '@/lib/deposit-api'

const QUICK_ACTIONS = [
  { label: '계좌이체', href: '/transfer/account', icon: '💸' },
  { label: '계좌조회', href: '/inquiry/accounts', icon: '📋' },
  { label: '대출신청', href: '/loans/apply', icon: '🏦' },
  { label: '설정', href: '/settings', icon: '⚙️' },
]

export default function DashboardPage() {
  const [userName, setUserName] = useState<string | null>(null)
  const [balanceVisible, setBalanceVisible] = useState(true)
  const [accounts, setAccounts] = useState<DepositViewAccount[]>([])
  const [recentTx, setRecentTx] = useState<DepositTransaction[]>([])

  useEffect(() => {
    try {
      const stored = localStorage.getItem('user')
      if (stored) setUserName(JSON.parse(stored).name)
    } catch {}

    const customerId = getCurrentDepositCustomerId()
    async function loadData() {
      try {
        const accs = await fetchDepositAccountViewModels(customerId)
        setAccounts(accs)
      } catch {
        setAccounts([])
      }
      try {
        const txs = await fetchTransactions({ customerId })
        setRecentTx(txs.slice(0, 5))
      } catch {
        setRecentTx([])
      }
    }
    loadData()
  }, [])

  const totalBalance = accounts.reduce((s, a) => s + a.balance, 0)

  const now = new Date()
  const dateStr = `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, '0')}.${String(now.getDate()).padStart(2, '0')}`

  return (
    <div className="max-w-kb-container mx-auto px-6 py-10 pb-16">
      {/* 브레드크럼 */}
      <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
        <span>개인뱅킹</span><span>&gt;</span>
        <span className="font-semibold text-kb-text">대시보드</span>
      </div>

      {/* 헤더 */}
      <div className="flex items-end justify-between mb-6">
        <div>
          <h1 className="text-[22px] font-bold text-kb-text">
            {userName ? `${userName} 고객님, 안녕하세요.` : '안녕하세요.'}
          </h1>
          <p className="text-[13px] text-kb-text-muted mt-1">{dateStr} 기준</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-[12px] text-kb-text-muted">잔액보기</span>
          <button
            onClick={() => setBalanceVisible(v => !v)}
            className={`relative w-10 h-5 rounded-full transition-colors ${balanceVisible ? 'bg-kb-blue' : 'bg-gray-300'}`}
          >
            <span className={`absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${balanceVisible ? 'translate-x-5' : 'translate-x-0.5'}`} />
            <span className={`absolute text-[9px] font-bold text-white ${balanceVisible ? 'left-1.5 top-0.5' : 'right-1 top-0.5'}`}>
              {balanceVisible ? 'ON' : 'OFF'}
            </span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-6 mb-6">
        {/* 총 자산 카드 */}
        <div className="col-span-1 border border-kb-border-dark rounded-xl p-6 bg-kb-beige-light">
          <p className="text-[12px] text-kb-text-muted mb-1">총 보유 자산</p>
          <p className="text-[24px] font-bold text-kb-text mb-3">
            {balanceVisible ? formatNumber(totalBalance) : '●●●●●●●'}원
          </p>
          <div className="border-t border-kb-border pt-3 space-y-2">
            {accounts.map(acc => (
              <div key={acc.id} className="flex justify-between text-[13px]">
                <span className="text-kb-text-muted truncate max-w-[130px]">{acc.name}</span>
                <span className="font-medium text-kb-text">
                  {balanceVisible ? formatNumber(acc.balance) : '●●●●●'}원
                </span>
              </div>
            ))}
          </div>
          <Link href="/inquiry/accounts" className="mt-3 block text-[12px] text-kb-blue hover:underline text-right">
            전체계좌 보기 &gt;
          </Link>
        </div>

        {/* 퀵메뉴 + 안내 */}
        <div className="col-span-2 space-y-4">
          {/* 퀵 액션 4개 */}
          <div className="grid grid-cols-4 border border-kb-border-dark rounded-xl divide-x divide-kb-border overflow-hidden">
            {QUICK_ACTIONS.map(action => (
              <Link
                key={action.href}
                href={action.href}
                className="flex flex-col items-center gap-2 py-5 hover:bg-kb-beige-light transition-colors"
              >
                <span className="text-2xl">{action.icon}</span>
                <span className="text-[13px] font-medium text-kb-text">{action.label}</span>
              </Link>
            ))}
          </div>

          {/* 계좌 요약 */}
          <div className="border border-kb-border-dark rounded-xl divide-y divide-kb-border overflow-hidden">
            {accounts.map(acc => (
              <div key={acc.id} className="flex items-center justify-between px-5 py-4">
                <div>
                  <p className="text-[13px] font-bold text-kb-text">{acc.name}</p>
                  <p className="text-[12px] text-kb-text-muted">{acc.number}</p>
                </div>
                <div className="text-right">
                  <p className="text-[15px] font-bold text-kb-text">
                    {balanceVisible ? formatNumber(acc.balance) : '●●●●●'}원
                  </p>
                  <div className="flex gap-1.5 mt-1 justify-end">
                    <Link href={`/accounts/${acc.id}`} className="text-[12px] border border-kb-border px-3 py-0.5 text-kb-text-body hover:bg-kb-beige-light">
                      조회
                    </Link>
                    <Link href="/transfer/account" className="text-[12px] border border-kb-border px-3 py-0.5 text-kb-text-body hover:bg-kb-beige-light">
                      이체
                    </Link>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 최근 거래내역 */}
      <section>
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-bold text-kb-text">최근 거래내역</h2>
          <Link href="/inquiry/transactions" className="text-[12px] text-kb-blue hover:underline">
            전체보기 &gt;
          </Link>
        </div>
        <table className="w-full border-collapse text-[13px]">
          <thead>
            <tr className="bg-kb-beige-light border-t-2 border-kb-text">
              <th className="border border-kb-border px-4 py-2 text-left font-semibold text-kb-text">날짜/시간</th>
              <th className="border border-kb-border px-4 py-2 text-left font-semibold text-kb-text">내용</th>
              <th className="border border-kb-border px-4 py-2 text-right font-semibold text-kb-text">출금금액</th>
              <th className="border border-kb-border px-4 py-2 text-right font-semibold text-kb-text">입금금액</th>
              <th className="border border-kb-border px-4 py-2 text-right font-semibold text-kb-text">잔액</th>
            </tr>
          </thead>
          <tbody>
            {recentTx.length === 0 ? (
              <tr>
                <td colSpan={5} className="border border-kb-border px-4 py-6 text-center text-kb-text-muted">
                  거래내역이 없습니다.
                </td>
              </tr>
            ) : recentTx.map(tx => {
              const amt = Number(tx.amount)
              const isOut = tx.directionType === 'OUT'
              return (
                <tr key={tx.transactionId} className="hover:bg-kb-beige-light transition-colors">
                  <td className="border border-kb-border px-4 py-2 text-kb-text-muted whitespace-nowrap">{tx.transactionAt}</td>
                  <td className="border border-kb-border px-4 py-2 text-kb-text-body">
                    {tx.transactionSummary || tx.transactionType}
                    {tx.transactionMemo && <span className="text-[11px] text-kb-text-muted ml-2">[{tx.transactionMemo}]</span>}
                  </td>
                  <td className="border border-kb-border px-4 py-2 text-right text-kb-red font-medium">
                    {isOut ? formatNumber(amt) : ''}
                  </td>
                  <td className="border border-kb-border px-4 py-2 text-right text-kb-blue font-medium">
                    {!isOut ? formatNumber(amt) : ''}
                  </td>
                  <td className="border border-kb-border px-4 py-2 text-right text-kb-text">
                    -
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </section>
    </div>
  )
}
