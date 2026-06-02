'use client'

import { useEffect, useState } from 'react'
import DepositSidebar from '@/components/products/DepositSidebar'
import { fetchDepositAccountViewModels, getCurrentDepositCustomerId, type DepositViewAccount } from '@/lib/deposit-api'

const STEP_LABELS = ['보유계좌조회', '전환정보 확인', '약관동의', '인증', '최종확인', '완료']

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
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">
        <DepositSidebar />

        <main className="flex-1 pl-8 pt-6 pb-12">
          <h1 className="text-[22px] font-bold text-kb-text mb-5">예금전환</h1>

          {/* 6단계 표시 */}
          <div className="flex items-center gap-1 mb-6 flex-wrap">
            {STEP_LABELS.map((label, i) => (
              <div key={i} className="flex items-center gap-1">
                {i > 0 && <span className="text-[12px] text-kb-text-muted mx-0.5">›</span>}
                <div
                  className="flex items-center gap-1.5 px-3 py-1 rounded-full text-[12px] font-medium"
                  style={i + 1 === step
                    ? { backgroundColor: '#0D5C47', color: 'white' }
                    : i + 1 < step
                    ? { backgroundColor: '#E2F5EF', color: '#5BC9A8' }
                    : { backgroundColor: '#F3F4F6', color: '#9CA3AF' }}>
                  <span className="font-bold">{i + 1}</span>
                  {i + 1 === step && <span>{label}</span>}
                </div>
              </div>
            ))}
          </div>

          {/* STEP 1 */}
          {step === 1 && (
            <>
              <div className="rounded-xl px-5 py-4 mb-5 text-[12px] space-y-1.5"
                style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
                <p className="flex gap-1.5 text-kb-text-muted">
                  <span className="flex-shrink-0">·</span>
                  <span>입출금이 자유로운 예금을 제외한 타 상품으로 전환하는 서비스입니다. (단, MMDA예금 등 일부 상품은 전환이 불가합니다.)</span>
                </p>
                <p className="flex gap-1.5 text-kb-text-muted">
                  <span className="flex-shrink-0">·</span>
                  <span>전환 후에는 계좌번호 및 각종 자동이체 등이 변경되지 않습니다.</span>
                </p>
                <p className="flex gap-1.5 text-kb-text-muted">
                  <span className="flex-shrink-0">·</span>
                  <span>전환할 계좌를 확인한 후 전환 버튼을 눌러주시기 바랍니다.</span>
                </p>
              </div>

              <div className="rounded-xl overflow-hidden" style={{ border: '1px solid #E2F5EF' }}>
                <table className="w-full border-collapse text-[13px]">
                  <thead>
                    <tr style={{ backgroundColor: '#F0FAF7' }}>
                      {['계좌번호', '상품명', '잔액', '상품전환'].map(h => (
                        <th key={h} className="px-4 py-3 text-center font-semibold text-[12px]"
                          style={{ borderBottom: '2px solid #E2F5EF', color: '#0D5C47' }}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {accounts.length === 0 ? (
                      <tr>
                        <td colSpan={4} className="px-4 py-10 text-center text-[13px] text-kb-text-muted">
                          보유 계좌가 없습니다.
                        </td>
                      </tr>
                    ) : accounts.map(acc => (
                      <tr key={acc.id} className="border-b hover:bg-[#F8FFFE] transition-colors"
                        style={{ borderColor: '#E2F5EF' }}>
                        <td className="px-4 py-3.5 text-center font-medium" style={{ color: '#0D5C47' }}>{acc.number}</td>
                        <td className="px-4 py-3.5 text-center text-kb-text">{acc.name}</td>
                        <td className="px-4 py-3.5 text-right font-semibold pr-5" style={{ color: '#0D5C47' }}>
                          {Number(acc.balance).toLocaleString('ko-KR')}원
                        </td>
                        <td className="px-4 py-3.5 text-center">
                          <button
                            onClick={() => handleConvert(acc.number)}
                            className="px-5 py-1.5 text-[12px] font-bold text-white rounded-lg hover:opacity-85 transition-opacity"
                            style={{ backgroundColor: '#0D5C47' }}>
                            전환
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}

          {/* STEP 2 */}
          {step === 2 && converting && (
            <>
              <div className="rounded-xl p-6 mb-6 flex items-center gap-5"
                style={{ backgroundColor: '#F0FAF7', border: '1px solid #E2F5EF' }}>
                <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0"
                  style={{ backgroundColor: '#0D5C47' }}>
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                </div>
                <div>
                  <p className="text-[16px] font-bold mb-1" style={{ color: '#0D5C47' }}>전환 신청이 접수되었습니다.</p>
                  <p className="text-[12px] text-kb-text-muted">계좌번호 {converting}</p>
                </div>
              </div>

              <div className="flex justify-center">
                <button
                  onClick={() => { setStep(1); setConverting(null) }}
                  className="border rounded-xl px-10 py-2.5 text-[14px] font-medium transition-colors hover:bg-[#F0FAF7]"
                  style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
                  돌아가기
                </button>
              </div>
            </>
          )}
        </main>
      </div>
    </div>
  )
}
