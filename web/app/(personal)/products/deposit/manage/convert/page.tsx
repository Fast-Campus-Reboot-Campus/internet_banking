'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import DepositSidebar from '@/components/products/DepositSidebar'
import { fetchDepositAccountViewModels, getCurrentDepositCustomerId, type DepositViewAccount } from '@/lib/deposit-api'

const STEPS = ['1. 보유계좌조회', '2', '3', '4', '5', '6']

export default function DepositConvertPage() {
  const [step, setStep] = useState(1)
  const [converting, setConverting] = useState<string | null>(null)
  const [accounts, setAccounts] = useState<DepositViewAccount[]>([])

  useEffect(() => {
    fetchDepositAccountViewModels(getCurrentDepositCustomerId())
      .then(data => setAccounts(data))
      .catch(() => setAccounts([]))
  }, [])

  function handleConvert(no: string) {
    setConverting(no)
    setStep(2)
  }

  return (
    <div className="max-w-kb-container mx-auto px-6 py-6">
      <div className="flex justify-end mb-3 text-[12px] text-kb-text-muted gap-1 items-center">
        <span>개인뱅킹</span><span>›</span>
        <span>금융상품</span><span>›</span>
        <span>예금</span><span>›</span>
        <span>예금 관리</span><span>›</span>
        <Link href="/products/deposit/manage/convert" className="text-kb-blue hover:underline">예금전환</Link>
      </div>

      <div className="flex gap-6">
        <DepositSidebar />

        <main className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-5">
            <h1 className="text-[20px] font-bold text-kb-text">예금전환</h1>
            <div className="flex gap-1">
              {STEPS.map((s, i) => (
                <button key={s}
                  className={`px-4 py-1.5 text-[12px] ${
                    i + 1 === step
                      ? 'font-bold text-white'
                      : 'text-kb-text-body border border-kb-border bg-white hover:bg-kb-beige-light'
                  }`}
                  style={i + 1 === step ? { backgroundColor: '#5BC9A8' } : {}}>
                  {s}
                </button>
              ))}
            </div>
          </div>

          {step === 1 && (
            <>
              <div className="border border-kb-border bg-[#FAFAFA] px-4 py-3 mb-5 text-[12px] text-kb-text-body space-y-1">
                {[
                  '입출금이자유로운 예금을 제외하는 타 상품으로 전환하는 서비스입니다. (단, MMDA예금 등 상품에서 제외사항이 있는 경우 전환이 불가합니다.)',
                  '전환 후에는 계좌번호 및 각종 자동이체 등이 변경되지 않습니다.',
                  '전환할 계좌를 확인한 후 전환 버튼을 눌러주시기 바랍니다.',
                ].map((n, i) => (
                  <p key={i} className="flex gap-1.5"><span className="flex-shrink-0">-</span><span>{n}</span></p>
                ))}
              </div>

              <table className="w-full border-collapse text-[13px] border-t-2 border-kb-text">
                <thead>
                  <tr className="bg-kb-beige-light">
                    {['계좌번호', '상품명', '잔액', '상품전환'].map(h => (
                      <th key={h} className="border border-kb-border px-4 py-3 font-semibold text-kb-text text-center">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {accounts.length === 0 ? (
                    <tr><td colSpan={4} className="border border-kb-border px-4 py-6 text-center text-kb-text-muted text-[13px]">보유 계좌가 없습니다.</td></tr>
                  ) : accounts.map(acc => (
                    <tr key={acc.id}>
                      <td className="border border-kb-border px-4 py-3 text-center text-kb-text-body">{acc.number}</td>
                      <td className="border border-kb-border px-4 py-3 text-center text-kb-text-body">{acc.name}</td>
                      <td className="border border-kb-border px-4 py-3 text-right text-kb-text-body pr-6">{Number(acc.balance).toLocaleString('ko-KR')}</td>
                      <td className="border border-kb-border px-4 py-3 text-center">
                        <button
                          onClick={() => handleConvert(acc.number)}
                          className="border border-[#5BC9A8] px-5 py-1.5 text-[12px] font-bold hover:bg-[#5BC9A8] hover:text-white transition-colors"
                          style={{ color: '#5BC9A8' }}>
                          전환
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}

          {step === 2 && converting && (
            <div className="border border-kb-border py-16 text-center">
              <p className="text-[16px] font-bold text-kb-text mb-2">전환 신청이 접수되었습니다.</p>
              <p className="text-[13px] text-kb-text-muted mb-6">계좌번호 {converting}</p>
              <button
                onClick={() => { setStep(1); setConverting(null) }}
                className="border border-kb-border px-8 py-2.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                돌아가기
              </button>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
