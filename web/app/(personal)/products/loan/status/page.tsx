'use client'

import Link from 'next/link'
import { useState } from 'react'
import LoanSidebar from '@/components/inquiry/LoanSidebar'

const MOCK_APPLICATIONS = [
  {
    id: 'APP-2026-001',
    appliedAt: '2026.05.20',
    productName: 'AXful 직장인 신용대출',
    status: '심사중',
    statusColor: '#C09B3A',
    validUntil: '2026.06.20',
    amount: '3,000만원',
  },
  {
    id: 'APP-2026-002',
    appliedAt: '2026.04.10',
    productName: 'AXful 아파트담보대출',
    status: '서류제출 필요',
    statusColor: '#DC2626',
    validUntil: '2026.05.10',
    amount: '2억원',
  },
  {
    id: 'APP-2026-003',
    appliedAt: '2026.03.05',
    productName: 'AXful 비상금대출',
    status: '실행완료',
    statusColor: '#0D5C47',
    validUntil: '-',
    amount: '200만원',
  },
]

const STATUS_ACTIONS = [
  { label: '사후서류제출',        href: '/products/loan/status/docs' },
  { label: '배우자정보제공동의',   href: '/products/loan/status/spouse' },
  { label: '세대원정보제공동의',   href: '/products/loan/status/household' },
  { label: '제3자담보제공동의',    href: '/products/loan/status/collateral' },
  { label: '부동산담보대출 전자서명', href: '/products/loan/status/sign' },
]

export default function LoanStatusPage() {
  const [searched, setSearched] = useState(false)

  return (
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6">
        <nav className="text-[12px] text-kb-text-muted mb-4 flex items-center gap-1">
          <Link href="/" className="hover:underline">개인뱅킹</Link><span>›</span>
          <Link href="/products/deposit" className="hover:underline">금융상품</Link><span>›</span>
          <Link href="/products/loan" className="hover:underline">대출</Link><span>›</span>
          <span className="text-kb-text font-medium">대출진행현황</span><span>›</span>
          <span className="text-kb-text font-medium">진행현황조회/실행/예약</span>
        </nav>

        <div className="flex gap-8">
          <LoanSidebar />
          <div className="flex-1 min-w-0">
            <h1 className="text-[22px] font-bold text-kb-text mb-5">진행현황조회/실행/예약</h1>

            {/* 안내 박스 */}
            <div className="rounded-xl px-5 py-4 mb-6 text-[13px] text-kb-text-body space-y-1"
              style={{ border: '1px solid #E2F5EF', backgroundColor: '#F8FFFE' }}>
              <p>· 대출 신청 후 진행 중인 현황을 조회하고, 승인된 대출을 실행하거나 예약할 수 있습니다.</p>
              <p>· 신청 유효기간 내에만 실행·예약이 가능합니다.</p>
              <p>· 서류 제출이 필요한 경우 <Link href="/products/loan/status/docs" className="underline font-medium" style={{ color: '#0D5C47' }}>사후서류제출</Link> 메뉴를 이용하세요.</p>
            </div>

            {/* 조회 폼 */}
            <div className="rounded-xl p-5 mb-6" style={{ border: '1px solid #E2F5EF', backgroundColor: '#F8FFFE' }}>
              <div className="flex items-center gap-4 mb-5">
                <span className="w-24 text-[13px] font-medium text-kb-text flex-shrink-0">조회 구분</span>
                <div className="flex gap-5">
                  {['진행중', '완료', '전체'].map(v => (
                    <label key={v} className="flex items-center gap-1.5 cursor-pointer">
                      <input type="radio" name="statusFilter" defaultChecked={v === '진행중'}
                        style={{ accentColor: '#0D5C47' }} />
                      <span className="text-[13px] text-kb-text-body">{v}</span>
                    </label>
                  ))}
                </div>
              </div>
              <div className="flex justify-center">
                <button onClick={() => setSearched(true)}
                  className="px-16 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity"
                  style={{ backgroundColor: '#0D5C47' }}>조회</button>
              </div>
            </div>

            {/* 결과 테이블 */}
            {searched && (
              <>
                <div className="rounded-xl overflow-hidden mb-6" style={{ border: '1px solid #E2F5EF' }}>
                  <table className="w-full text-[13px]">
                    <thead>
                      <tr style={{ backgroundColor: '#F0FAF7', borderBottom: '2px solid #0D5C47' }}>
                        <th className="px-4 py-3 text-center font-semibold" style={{ color: '#0D5C47' }}>신청일자</th>
                        <th className="px-4 py-3 text-left font-semibold" style={{ color: '#0D5C47' }}>상품명</th>
                        <th className="px-4 py-3 text-center font-semibold" style={{ color: '#0D5C47' }}>신청금액</th>
                        <th className="px-4 py-3 text-center font-semibold" style={{ color: '#0D5C47' }}>진행상태</th>
                        <th className="px-4 py-3 text-center font-semibold" style={{ color: '#0D5C47' }}>신청 유효기간</th>
                        <th className="px-4 py-3 text-center font-semibold" style={{ color: '#0D5C47' }}>처리</th>
                      </tr>
                    </thead>
                    <tbody>
                      {MOCK_APPLICATIONS.map((app, i) => (
                        <tr key={app.id} className="hover:bg-[#F8FFFE] transition-colors"
                          style={{ borderBottom: i < MOCK_APPLICATIONS.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
                          <td className="px-4 py-4 text-center text-kb-text-muted">{app.appliedAt}</td>
                          <td className="px-4 py-4 font-medium text-kb-text">{app.productName}</td>
                          <td className="px-4 py-4 text-center font-semibold" style={{ color: '#0D5C47' }}>{app.amount}</td>
                          <td className="px-4 py-4 text-center">
                            <span className="text-[12px] font-bold px-2 py-1 rounded-lg"
                              style={{ backgroundColor: app.statusColor + '18', color: app.statusColor }}>
                              {app.status}
                            </span>
                          </td>
                          <td className="px-4 py-4 text-center text-kb-text-muted">{app.validUntil}</td>
                          <td className="px-4 py-4 text-center">
                            {app.status === '심사중' && (
                              <button className="text-[12px] border rounded-lg px-3 py-1 hover:bg-[#F0FAF7] transition-colors"
                                style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>상세보기</button>
                            )}
                            {app.status === '서류제출 필요' && (
                              <Link href="/products/loan/status/docs"
                                className="text-[12px] border rounded-lg px-3 py-1 hover:bg-[#F0FAF7] transition-colors"
                                style={{ borderColor: '#DC2626', color: '#DC2626' }}>서류제출</Link>
                            )}
                            {app.status === '실행완료' && (
                              <span className="text-[12px] text-kb-text-muted">-</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* 바로가기 버튼 */}
                <div className="flex flex-wrap gap-2">
                  {STATUS_ACTIONS.map(action => (
                    <Link key={action.href} href={action.href}
                      className="border rounded-xl px-4 py-2 text-[13px] font-medium hover:bg-[#F0FAF7] transition-colors"
                      style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
                      {action.label}
                    </Link>
                  ))}
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </main>
  )
}
