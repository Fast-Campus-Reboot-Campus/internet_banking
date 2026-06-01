'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { formatNumber } from '@/lib/mock-data'
import { fetchDepositAccountViewModels, getCurrentDepositCustomerId } from '@/lib/deposit-api'
import TransferSidebar from '@/components/inquiry/TransferSidebar'

type PendingTransfer = {
  fromNumber: string
  fromName: string
  toBank: string
  toBankCode?: string
  toAccount: string
  amount: number
  receiverName: string
  fee: number
}

type PaymentResult = {
  status: 'COMPLETED' | 'FAILED' | 'CLEARING' | 'ERROR'
  piId?: string
  txNo?: string
  failureCategory?: string | null
  message?: string
}

const FAILURE_MESSAGES: Record<string, string> = {
  INSUFFICIENT_BALANCE: '잔액 부족',
  ACCOUNT_CLOSED: '수취계좌 폐쇄',
  ACCOUNT_RESTRICTED: '수취계좌 사고신고',
  OWNER_INQUIRY_FAILED: '예금주 불일치',
  LIMIT_EXCEEDED: '한도 초과',
}

export default function TransferResultPage() {
  const router = useRouter()
  const [data, setData] = useState<PendingTransfer | null>(null)
  const [accounts, setAccounts] = useState<{ number: string; balance: number }[]>([])
  const [paymentResult, setPaymentResult] = useState<PaymentResult | null>(null)

  useEffect(() => {
    fetchDepositAccountViewModels(getCurrentDepositCustomerId())
      .then(accs => setAccounts(accs.map(a => ({ number: a.number, balance: Number(a.balance) }))))
      .catch(() => {})
  }, [])

  useEffect(() => {
    const raw = sessionStorage.getItem('pendingTransfer')
    if (!raw) { router.push('/transfer/account'); return }
    const transfer = JSON.parse(raw)
    setData(transfer)

    const resultRaw = sessionStorage.getItem('paymentResult')
    const result: PaymentResult | null = resultRaw ? JSON.parse(resultRaw) : null
    setPaymentResult(result)

    // COMPLETED 일 때만 이체 기록을 localStorage 히스토리에 저장
    if (!result || result.status === 'COMPLETED' || result.status === 'CLEARING') {
      try {
        const prev = JSON.parse(localStorage.getItem('transferHistory') || '[]')
        const now = new Date()
        const datetime =
          `${now.getFullYear()}.${String(now.getMonth()+1).padStart(2,'0')}.${String(now.getDate()).padStart(2,'0')}\n` +
          `${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}:${String(now.getSeconds()).padStart(2,'0')}`
        prev.unshift({
          id: String(now.getTime()),
          datetime,
          bank: transfer.toBank,
          account: transfer.toAccount,
          receiver: transfer.receiverName,
          amount: transfer.amount,
          memo: '',
        })
        localStorage.setItem('transferHistory', JSON.stringify(prev.slice(0, 50)))
      } catch {}
    }

    sessionStorage.removeItem('pendingTransfer')
    sessionStorage.removeItem('paymentResult')
  }, [router])

  if (!data) return null

  const fromAcc = accounts.find(a => a.number === data?.fromNumber)
  const remainingBalance = (fromAcc?.balance ?? 0) - (data?.amount ?? 0)

  return (
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">
        <TransferSidebar />

        {/* ===== 본문 ===== */}
        <main className="flex-1 pl-8 pt-4 pb-12">
          <h1 className="text-[20px] font-bold text-kb-text mb-5">계좌이체</h1>

          {/* 완료/실패 메시지 */}
          {(!paymentResult || paymentResult.status === 'COMPLETED' || paymentResult.status === 'CLEARING') ? (
            <div className="border border-kb-border p-5 mb-6 flex items-center gap-5">
              <div className="flex-shrink-0 relative w-16 h-16">
                <svg viewBox="0 0 64 64" fill="none" className="w-16 h-16">
                  <rect x="6" y="10" width="40" height="28" rx="2" fill="#D8D8D8" stroke="#BBBBBB" strokeWidth="1.5"/>
                  <rect x="18" y="38" width="16" height="5" rx="1" fill="#BBBBBB"/>
                  <rect x="12" y="43" width="28" height="3" rx="1" fill="#BBBBBB"/>
                  <circle cx="48" cy="14" r="10" fill="#FFCC00" stroke="white" strokeWidth="2"/>
                  <polyline points="43,14 47,18 54,10" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </div>
              <div>
                <p className="text-[15px] font-bold mb-1" style={{ color: '#2563EB' }}>
                  {paymentResult?.status === 'CLEARING' ? '이체 요청이 접수되었습니다.' : '즉시이체가 완료되었습니다.'}
                </p>
                <p className="text-[12px] text-kb-text-muted">* 타행계좌로의 이체는 해당 은행의 사정에 따라 입금이 다소 지연될 수 있습니다.</p>
                {paymentResult?.txNo && (
                  <p className="text-[12px] text-kb-text-muted">거래번호: {paymentResult.txNo}</p>
                )}
              </div>
            </div>
          ) : (
            <div className="border border-red-300 bg-red-50 p-5 mb-6 flex items-center gap-5">
              <div className="flex-shrink-0 w-16 h-16 flex items-center justify-center">
                <svg viewBox="0 0 64 64" fill="none" className="w-16 h-16">
                  <circle cx="32" cy="32" r="26" fill="#FEE2E2" stroke="#FCA5A5" strokeWidth="2"/>
                  <line x1="22" y1="22" x2="42" y2="42" stroke="#DC2626" strokeWidth="3" strokeLinecap="round"/>
                  <line x1="42" y1="22" x2="22" y2="42" stroke="#DC2626" strokeWidth="3" strokeLinecap="round"/>
                </svg>
              </div>
              <div>
                <p className="text-[15px] font-bold mb-1 text-red-600">
                  {paymentResult.status === 'ERROR' ? '이체 처리 중 오류가 발생했습니다.' : '이체가 실패하였습니다.'}
                </p>
                <p className="text-[13px] text-red-500">
                  {paymentResult.status === 'ERROR'
                    ? (paymentResult.message ?? '잠시 후 다시 시도해 주세요.')
                    : (paymentResult.failureCategory
                        ? (FAILURE_MESSAGES[paymentResult.failureCategory] ?? paymentResult.failureCategory)
                        : '이체를 처리할 수 없습니다.')}
                </p>
              </div>
            </div>
          )}

          {/* 이체결과 확인 */}
          <div className="mb-5">
            <div className="flex justify-between items-center mb-2">
              <p className="text-[14px] font-bold text-kb-text">이체결과 확인</p>
              <div className="flex gap-2">
                <button className="border border-kb-border px-4 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">자주쓰는계좌 등록</button>
                <button className="border border-kb-border px-4 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">단축이체 등록</button>
              </div>
            </div>
            <table className="w-full border-collapse text-[13px]">
              <thead>
                <tr className="bg-kb-beige-light">
                  <th className="border border-kb-border px-3 py-2 text-center font-semibold">출금계좌번호</th>
                  <th className="border border-kb-border px-3 py-2 text-center font-semibold">입금계좌번호</th>
                  <th className="border border-kb-border px-3 py-2 text-center font-semibold">
                    이체금액(원)<br/>
                    <span className="font-normal text-kb-text-muted">수수료(원)</span>
                  </th>
                  <th className="border border-kb-border px-3 py-2 text-center font-semibold">
                    받는분 예금주명<br/>
                    <span className="font-normal text-kb-text-muted">(실제 예금주명)</span>
                  </th>
                  <th className="border border-kb-border px-3 py-2 text-center font-semibold">결과</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td className="border border-kb-border px-3 py-3 text-center">{data.fromNumber}</td>
                  <td className="border border-kb-border px-3 py-3 text-center">
                    <p>{data.toBank}</p>
                    <p>{data.toAccount}</p>
                    <button className="text-[11px] border border-kb-border px-1.5 py-0.5 mt-1 text-kb-text-muted hover:bg-kb-beige-light">
                      <svg viewBox="0 0 16 16" fill="none" className="w-3.5 h-3.5 inline" stroke="currentColor" strokeWidth="1.5">
                        <rect x="4" y="4" width="9" height="10" rx="1"/><rect x="2" y="2" width="9" height="10" rx="1" fill="white"/>
                      </svg>
                    </button>
                  </td>
                  <td className="border border-kb-border px-3 py-3 text-right">
                    <p className="font-semibold">{formatNumber(data.amount)}</p>
                    <p className="text-kb-text-muted">{data.fee === 0 ? '면제' : formatNumber(data.fee)}</p>
                  </td>
                  <td className="border border-kb-border px-3 py-3 text-center">{data.receiverName}</td>
                  <td className="border border-kb-border px-3 py-3 text-center">
                    {(!paymentResult || paymentResult.status === 'COMPLETED' || paymentResult.status === 'CLEARING') ? (
                      <>
                        <p className="font-semibold text-kb-text">
                          {paymentResult?.status === 'CLEARING' ? '처리중' : '정상'}
                        </p>
                        <button className="text-[11px] border border-kb-border px-2 py-0.5 mt-1 text-kb-text-muted hover:bg-kb-beige-light flex items-center gap-0.5 mx-auto">
                          알림
                          <svg viewBox="0 0 12 12" fill="none" className="w-3 h-3" stroke="currentColor" strokeWidth="1.5">
                            <path d="M2 10L10 2M10 2H5M10 2v5"/>
                          </svg>
                        </button>
                      </>
                    ) : (
                      <p className="font-semibold text-red-600">실패</p>
                    )}
                  </td>
                </tr>
              </tbody>
            </table>
            <p className="text-right text-[12px] text-kb-text-muted mt-2">
              이체 후 잔액 : {formatNumber(remainingBalance)}원
            </p>
          </div>

          {/* 버튼 그룹 */}
          <div className="flex justify-center gap-2 mb-6">
            <Link href="/transfer/inquiry"
              className="bg-kb-yellow px-8 py-2.5 text-[14px] font-bold text-kb-text hover:brightness-95">
              이체결과 조회
            </Link>
            <button className="border border-kb-border px-8 py-2.5 text-[14px] text-kb-text-body hover:bg-kb-beige-light">
              전자확인증
            </button>
            <Link href="/transfer/account"
              className="border border-kb-border px-8 py-2.5 text-[14px] text-kb-text-body hover:bg-kb-beige-light">
              이체 다시하기
            </Link>
          </div>

          {/* 안내 박스 */}
          <div className="border border-kb-border p-4 text-[12px] space-y-1.5">
            <p className="text-kb-text-muted">* 인터넷뱅킹 종료 시 안전한 금융거래를 위하여 반드시 [로그아웃] 버튼을 눌러 종료하시기 바랍니다.</p>
            <p>
              <span className="text-kb-text-muted">· 고객님의 소중한 </span>
              <span className="underline text-kb-text-body cursor-pointer">금융자산</span>
              <span className="text-kb-text-muted"> 보호를 위해 이체한도 조회 후 평소 이체금액을 감안하여 이체한도를 조정할 수 있습니다.</span>
            </p>
            <Link href="/banking/transfer-limit"
              className="inline-block border border-kb-border px-4 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light mt-1">
              이체한도 조회/감액하기
            </Link>
          </div>
        </main>
      </div>
    </div>
  )
}
