'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import LoanSidebar from '@/components/inquiry/LoanSidebar'
import { api } from '@/lib/api'

type LoanApplication = {
  applId: number
  applNo: string
  requestedAmount: number
  requestedPeriodMo: number
  applStatusCd: string
  appliedAt: string
  repaymentMethodCd: string
}

const STATUS_LABEL: Record<string, { label: string; color: string }> = {
  SUBMITTED:   { label: '접수',       color: '#6B7280' },
  PRESCREENED: { label: '가심사완료', color: '#1A56DB' },
  REVIEWING:   { label: '심사중',     color: '#D97706' },
  APPROVED:    { label: '승인',       color: '#0D5C47' },
  REJECTED:    { label: '거절',       color: '#E05555' },
  CANCELLED:   { label: '취소',       color: '#9CA3AF' },
}

const REPAY_LABEL: Record<string, string> = {
  EQUAL_PRINCIPAL_INTEREST: '원리금균등',
  EQUAL_PRINCIPAL: '원금균등',
  BULLET: '만기일시',
  REVOLVING: '한도대출',
}

export default function LoanStatusPage() {
  const [applications, setApplications] = useState<LoanApplication[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/api/loan-applications')
      .then(res => setApplications(res.data.data?.items ?? []))
      .catch(() => setError('대출 신청 내역을 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6">
        <nav className="text-[12px] text-kb-text-muted mb-4 flex items-center gap-1">
          <Link href="/" className="hover:underline">개인뱅킹</Link><span>›</span>
          <Link href="/products/loan" className="hover:underline">대출</Link><span>›</span>
          <span className="text-kb-text font-medium">대출진행현황</span>
        </nav>

        <div className="flex gap-8">
          <LoanSidebar />
          <div className="flex-1 min-w-0">
            <h1 className="text-[22px] font-bold text-kb-text mb-5">진행현황조회/실행/예약</h1>

            <div className="rounded-xl px-5 py-4 mb-5 text-[12px] space-y-1.5"
              style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
              <p className="text-kb-text-muted">· 대출 신청 후 심사 진행 현황을 조회하고, 승인된 대출을 실행할 수 있습니다.</p>
              <p className="text-kb-text-muted">· 접수·가심사완료·심사중 단계에서는 신청을 취소할 수 있습니다.</p>
            </div>

            {loading && <p className="text-[13px] text-kb-text-muted py-10 text-center">불러오는 중...</p>}
            {error   && <p className="text-[13px] py-10 text-center" style={{ color: '#E05555' }}>{error}</p>}

            {!loading && !error && (
              <div className="rounded-xl overflow-hidden" style={{ border: '1px solid #E2F5EF' }}>
                <table className="w-full border-collapse text-[13px]">
                  <thead>
                    <tr style={{ backgroundColor: '#F0FAF7' }}>
                      {['신청번호', '신청금액', '기간', '상환방법', '신청일', '상태', ''].map(h => (
                        <th key={h} className="px-4 py-3 text-center font-semibold text-[12px]"
                          style={{ borderBottom: '2px solid #E2F5EF', color: '#0D5C47' }}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {applications.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="px-4 py-12 text-center text-[13px] text-kb-text-muted">
                          진행 중인 대출 신청이 없습니다.
                        </td>
                      </tr>
                    ) : applications.map(app => {
                      const st = STATUS_LABEL[app.applStatusCd] ?? { label: app.applStatusCd, color: '#6B7280' }
                      return (
                        <tr key={app.applId} className="border-b hover:bg-[#F8FFFE] transition-colors"
                          style={{ borderColor: '#E2F5EF' }}>
                          <td className="px-4 py-3.5 text-center font-medium" style={{ color: '#0D5C47' }}>{app.applNo}</td>
                          <td className="px-4 py-3.5 text-right pr-5 font-semibold">{Number(app.requestedAmount).toLocaleString('ko-KR')}원</td>
                          <td className="px-4 py-3.5 text-center">{app.requestedPeriodMo}개월</td>
                          <td className="px-4 py-3.5 text-center text-kb-text-muted">{REPAY_LABEL[app.repaymentMethodCd] ?? app.repaymentMethodCd}</td>
                          <td className="px-4 py-3.5 text-center text-kb-text-muted">{app.appliedAt?.slice(0, 10).replace(/-/g, '.') ?? '-'}</td>
                          <td className="px-4 py-3.5 text-center">
                            <span className="text-[12px] font-bold px-2 py-0.5 rounded-full"
                              style={{ color: st.color, backgroundColor: `${st.color}18` }}>
                              {st.label}
                            </span>
                          </td>
                          <td className="px-4 py-3.5 text-center">
                            <div className="flex gap-1.5 justify-center">
                              {app.applStatusCd === 'APPROVED' && (
                                <Link href={`/products/loan/status/execute?applId=${app.applId}`}
                                  className="px-3 py-1 text-[12px] font-bold text-white rounded-lg hover:opacity-85"
                                  style={{ backgroundColor: '#0D5C47' }}>
                                  대출실행
                                </Link>
                              )}
                              {['SUBMITTED','PRESCREENED','REVIEWING','APPROVED'].includes(app.applStatusCd) && (
                                <Link href={`/products/loan/status/docs?applId=${app.applId}`}
                                  className="px-3 py-1 text-[12px] font-medium rounded-lg border hover:bg-[#F0FAF7]"
                                  style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
                                  서류제출
                                </Link>
                              )}
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </main>
  )
}
