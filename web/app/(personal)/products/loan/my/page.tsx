'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import LoanSidebar from '@/components/inquiry/LoanSidebar'
import { api } from '@/lib/api'

type LoanContract = {
  cntrId: number
  cntrNo: string
  contractedAmount: number
  contractedPeriodMo: number
  totalRateBps: number
  rateTypeCd: string
  repaymentMethodCd: string
  cntrStatusCd: string
  cntrStartDate: string
  cntrEndDate: string
}

const STATUS_LABEL: Record<string, { label: string; color: string }> = {
  SIGNED:    { label: '약정완료', color: '#1A56DB' },
  ACTIVE:    { label: '대출중',   color: '#0D5C47' },
  CLOSED:    { label: '상환완료', color: '#6B7280' },
  CANCELLED: { label: '취소',     color: '#9CA3AF' },
}

const REPAY_LABEL: Record<string, string> = {
  EQUAL_PRINCIPAL_INTEREST: '원리금균등',
  EQUAL_PRINCIPAL: '원금균등',
  BULLET: '만기일시',
  REVOLVING: '한도대출',
}

const RATE_TYPE_LABEL: Record<string, string> = {
  FIXED: '고정금리', VARIABLE: '변동금리', MIXED: '혼합금리',
}

export default function MyLoanPage() {
  const [contracts, setContracts] = useState<LoanContract[]>([])
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState('')

  useEffect(() => {
    api.get('/api/loan-contracts')
      .then(res => setContracts(res.data.data?.items ?? []))
      .catch(() => setError('대출 계약 정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [])

  const active    = contracts.filter(c => c.cntrStatusCd === 'ACTIVE')
  const totalDebt = active.reduce((s, c) => s + (c.contractedAmount ?? 0), 0)

  return (
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6">
        <nav className="text-[12px] text-kb-text-muted mb-4 flex items-center gap-1">
          <Link href="/" className="hover:underline">개인뱅킹</Link><span>›</span>
          <Link href="/products/loan" className="hover:underline">대출</Link><span>›</span>
          <span className="text-kb-text font-medium">대출관리</span><span>›</span>
          <span className="text-kb-text font-medium">내 대출 현황</span>
        </nav>

        <div className="flex gap-8">
          <LoanSidebar />
          <div className="flex-1 min-w-0">
            <h1 className="text-[22px] font-bold text-kb-text mb-5">내 대출 현황</h1>

            {!loading && !error && (
              <div className="flex items-center justify-between mb-5 rounded-xl px-6 py-4"
                style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
                <p className="text-[14px] font-bold text-kb-text">
                  총 대출 잔액{' '}
                  <span className="text-[20px]" style={{ color: '#E05555' }}>{totalDebt.toLocaleString('ko-KR')}</span>원
                  <span className="text-kb-text-muted font-normal text-[13px] ml-1">({active.length}건)</span>
                </p>
              </div>
            )}

            {loading && <p className="text-[13px] text-kb-text-muted py-10 text-center">불러오는 중...</p>}
            {error   && <p className="text-[13px] py-10 text-center" style={{ color: '#E05555' }}>{error}</p>}

            {!loading && !error && contracts.map(c => {
              const st = STATUS_LABEL[c.cntrStatusCd] ?? { label: c.cntrStatusCd, color: '#6B7280' }
              return (
                <div key={c.cntrId} className="rounded-xl p-5 mb-3 shadow-sm" style={{ border: '1px solid #E2F5EF' }}>
                  <div className="flex items-start justify-between mb-3">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-[14px] font-bold" style={{ color: '#0D5C47' }}>{c.cntrNo}</span>
                        <span className="text-[11px] font-bold px-2 py-0.5 rounded-full"
                          style={{ color: st.color, backgroundColor: `${st.color}18` }}>
                          {st.label}
                        </span>
                      </div>
                      <p className="text-[12px] text-kb-text-muted">
                        {REPAY_LABEL[c.repaymentMethodCd] ?? c.repaymentMethodCd} · {RATE_TYPE_LABEL[c.rateTypeCd] ?? c.rateTypeCd}
                      </p>
                    </div>
                    <p className="text-[20px] font-bold" style={{ color: '#E05555' }}>
                      {(c.contractedAmount ?? 0).toLocaleString('ko-KR')}원
                    </p>
                  </div>
                  <div className="grid grid-cols-3 gap-3 text-[12px]">
                    {[
                      { label: '적용금리', value: `연 ${(c.totalRateBps / 100).toFixed(2)}%` },
                      { label: '대출기간', value: `${c.contractedPeriodMo}개월` },
                      { label: '만기일',   value: c.cntrEndDate?.replace(/-/g, '.') ?? '-' },
                    ].map(({ label, value }) => (
                      <div key={label}>
                        <p className="text-kb-text-muted mb-0.5">{label}</p>
                        <p className="font-semibold text-kb-text">{value}</p>
                      </div>
                    ))}
                  </div>
                  {c.cntrStatusCd === 'ACTIVE' && (
                    <div className="flex gap-2 mt-4 pt-3" style={{ borderTop: '1px solid #E2F5EF' }}>
                      {[
                        { label: '이자납입',   href: '/products/loan/manage/payment' },
                        { label: '대출금상환', href: '/products/loan/manage/repay' },
                        { label: '금리조회',   href: '/products/loan/manage/rate' },
                      ].map(({ label, href }) => (
                        <Link key={label} href={`${href}?cntrId=${c.cntrId}`}
                          className="px-4 py-1.5 text-[12px] font-medium rounded-lg border transition-colors hover:bg-[#F0FAF7]"
                          style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
                          {label}
                        </Link>
                      ))}
                    </div>
                  )}
                </div>
              )
            })}

            {!loading && !error && contracts.length === 0 && (
              <div className="rounded-xl px-6 py-12 text-center" style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
                <p className="text-[14px] text-kb-text-muted mb-3">보유 중인 대출이 없습니다.</p>
                <Link href="/products/loan/credit"
                  className="inline-block px-8 py-2 text-[13px] font-bold text-white rounded-xl hover:opacity-85"
                  style={{ backgroundColor: '#0D5C47' }}>대출 상품 보기</Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </main>
  )
}
