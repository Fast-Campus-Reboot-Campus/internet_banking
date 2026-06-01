'use client'

import Link from 'next/link'
import { useEffect, useRef, useState } from 'react'
import { useRouter } from 'next/navigation'
import { formatNumber } from '@/lib/mock-data'
import { fetchDepositAccountViewModels, getCurrentDepositCustomerId } from '@/lib/deposit-api'
import TransferSidebar from '@/components/inquiry/TransferSidebar'

type PendingTransfer = {
  fromNumber: string
  fromName: string
  toBank: string
  toAccount: string
  amount: number
  receiverName: string
  fee: number
}

export default function TransferResultPage() {
  const router = useRouter()
  const [data, setData] = useState<PendingTransfer | null>(null)
  const [remainingBalance, setRemainingBalance] = useState<number | null>(null)
  const initialized = useRef(false)

  useEffect(() => {
    if (initialized.current) return
    initialized.current = true

    const raw = sessionStorage.getItem('pendingTransfer')
    if (!raw) {
      router.replace('/transfer/account')
      return
    }

    const transfer: PendingTransfer = JSON.parse(raw)
    setData(transfer)

    // 이체 이력 저장
    try {
      const prev = JSON.parse(localStorage.getItem('transferHistory') || '[]')
      const now = new Date()
      prev.unshift({
        id: String(now.getTime()),
        datetime: `${now.getFullYear()}.${String(now.getMonth()+1).padStart(2,'0')}.${String(now.getDate()).padStart(2,'0')} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`,
        bank: transfer.toBank,
        account: transfer.toAccount,
        receiver: transfer.receiverName,
        amount: transfer.amount,
        memo: '',
      })
      localStorage.setItem('transferHistory', JSON.stringify(prev.slice(0, 50)))
    } catch {}

    sessionStorage.removeItem('pendingTransfer')

    // 잔액 조회
    fetchDepositAccountViewModels(getCurrentDepositCustomerId())
      .then(accs => {
        const acc = accs.find(a => a.number === transfer.fromNumber)
        if (acc) setRemainingBalance(Number(acc.balance) - Number(transfer.amount))
      })
      .catch(() => {})
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (!data) return null

  return (
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">
        <TransferSidebar />

        <main className="flex-1 pl-8 pt-6 pb-12">
          <h1 className="text-[22px] font-bold text-kb-text mb-6">계좌이체</h1>

          {/* 완료 배너 */}
          <div className="rounded-xl p-6 mb-6 flex items-center gap-5" style={{ backgroundColor: '#F0FAF7', border: '1px solid #E2F5EF' }}>
            <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: '#0D5C47' }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <div>
              <p className="text-[16px] font-bold mb-1" style={{ color: '#0D5C47' }}>즉시이체가 완료되었습니다.</p>
              <p className="text-[12px] text-kb-text-muted">타행계좌로의 이체는 해당 은행 사정에 따라 입금이 다소 지연될 수 있습니다.</p>
            </div>
          </div>

          {/* 이체결과 테이블 */}
          <div className="mb-6">
            <p className="text-[15px] font-bold text-kb-text mb-3">이체결과</p>
            <div className="rounded-xl overflow-hidden" style={{ border: '1px solid #E2F5EF' }}>
              <table className="w-full border-collapse text-[13px]">
                <thead>
                  <tr style={{ backgroundColor: '#F0FAF7' }}>
                    {['출금계좌번호', '입금계좌번호', '이체금액', '수수료', '받는분', '결과'].map(h => (
                      <th key={h} className="px-4 py-3 text-center font-semibold text-[12px]"
                        style={{ borderBottom: '2px solid #E2F5EF', color: '#0D5C47' }}>
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="px-4 py-4 text-center text-kb-text">{data.fromNumber}</td>
                    <td className="px-4 py-4 text-center">
                      <p className="font-medium text-kb-text">{data.toBank}</p>
                      <p className="text-kb-text-muted text-[12px]">{data.toAccount}</p>
                    </td>
                    <td className="px-4 py-4 text-right font-bold text-[15px]" style={{ color: '#0D5C47' }}>
                      {formatNumber(Number(data.amount))}원
                    </td>
                    <td className="px-4 py-4 text-center text-kb-text-muted">
                      {data.fee === 0 ? '면제' : `${formatNumber(data.fee)}원`}
                    </td>
                    <td className="px-4 py-4 text-center text-kb-text">{data.receiverName}</td>
                    <td className="px-4 py-4 text-center font-bold" style={{ color: '#5BC9A8' }}>완료</td>
                  </tr>
                </tbody>
              </table>
            </div>
            {remainingBalance !== null && (
              <p className="text-right text-[12px] text-kb-text-muted mt-2">
                이체 후 잔액 : <span className="font-semibold" style={{ color: '#0D5C47' }}>{formatNumber(remainingBalance)}원</span>
              </p>
            )}
          </div>

          {/* 버튼 그룹 */}
          <div className="flex justify-center gap-3 mb-8">
            <Link href="/transfer/inquiry"
              className="px-8 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity"
              style={{ backgroundColor: '#0D5C47' }}>
              이체결과 조회
            </Link>
            <Link href="/transfer/account"
              className="border rounded-xl px-8 py-2.5 text-[14px] font-medium transition-colors hover:bg-[#F0FAF7]"
              style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
              이체 다시하기
            </Link>
            <Link href="/"
              className="border rounded-xl px-8 py-2.5 text-[14px] font-medium transition-colors hover:bg-gray-50"
              style={{ borderColor: '#D1D5DB', color: '#6B7280' }}>
              홈으로
            </Link>
          </div>

          {/* 안내 */}
          <div className="rounded-xl px-5 py-4 text-[12px] text-kb-text-muted space-y-1" style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
            <p>· 인터넷뱅킹 종료 시 반드시 [로그아웃] 버튼을 눌러 종료하시기 바랍니다.</p>
            <p>· 평소 이체금액을 감안하여 이체한도를 조정하실 수 있습니다.{' '}
              <Link href="/banking/transfer-limit" className="underline font-medium" style={{ color: '#0D5C47' }}>
                이체한도 조회/변경
              </Link>
            </p>
          </div>
        </main>
      </div>
    </div>
  )
}
