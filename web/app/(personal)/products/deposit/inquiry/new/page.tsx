'use client'

import Link from 'next/link'
import DepositSidebar from '@/components/products/DepositSidebar'

const MOCK_ROWS = [
  {
    date: '2024.10.16',
    accountNo: '557315-2623671',
    type: 'AXful 정기예금',
    withdrawNo: '531089-04-274618',
    amount: '1,000,000',
  },
  {
    date: '2024.02.26',
    accountNo: '557315-2588965',
    type: 'AXful 정기예금',
    withdrawNo: '531089-04-274618',
    amount: '1,000,000',
  },
]

export default function DepositNewHistoryPage() {
  return (
    <div className="max-w-kb-container mx-auto px-6 py-6">
      {/* 브레드크럼 */}
      <div className="flex justify-end mb-3 text-[12px] text-kb-text-muted gap-1 items-center">
        <span>개인뱅킹</span><span>›</span>
        <span>금융상품</span><span>›</span>
        <span>예금</span><span>›</span>
        <Link href="/products/deposit/inquiry/new" className="hover:underline">신규결과/내역 조회</Link>
        <span>›</span>
        <Link href="#" className="text-kb-blue hover:underline">도움말</Link>
      </div>

      <div className="flex gap-6">
        <DepositSidebar />

        <main className="flex-1 min-w-0">
          <h1 className="text-[20px] font-bold text-kb-text mb-5">신규결과/내역 조회</h1>

          {/* 안내 */}
          <div className="border border-kb-border bg-[#FAFAFA] px-4 py-3 mb-5 text-[12px] text-kb-text-body space-y-1">
            <p className="flex gap-1.5"><span className="flex-shrink-0">-</span><span>예금 신규결과/내역입니다.</span></p>
            <p className="flex gap-1.5"><span className="flex-shrink-0">-</span><span>AX풀뱅크 계좌형자주 가입현황은 '재청·저주신규조회' 화면에서 확인가능합니다.</span></p>
          </div>

          {/* 테이블 */}
          <table className="w-full border-collapse text-[13px] border-t-2 border-kb-text">
            <thead>
              <tr className="bg-kb-beige-light">
                <th className="border border-kb-border px-3 py-3 font-semibold text-kb-text text-center">신규일자</th>
                <th className="border border-kb-border px-3 py-3 font-semibold text-kb-text text-center">신규계좌번호</th>
                <th className="border border-kb-border px-3 py-3 font-semibold text-kb-text text-center">신규계좌종류</th>
                <th className="border border-kb-border px-3 py-3 font-semibold text-kb-text text-center">출금계좌번호</th>
                <th className="border border-kb-border px-3 py-3 font-semibold text-kb-text text-center">신규금액</th>
              </tr>
            </thead>
            <tbody>
              {MOCK_ROWS.map((row, i) => (
                <tr key={i} className="hover:bg-[#fafafa]">
                  <td className="border border-kb-border px-3 py-3 text-center text-kb-text-body">{row.date}</td>
                  <td className="border border-kb-border px-3 py-3 text-center text-kb-text-body">{row.accountNo}</td>
                  <td className="border border-kb-border px-3 py-3 text-center text-kb-text-body">{row.type}</td>
                  <td className="border border-kb-border px-3 py-3 text-center text-kb-text-body">{row.withdrawNo}</td>
                  <td className="border border-kb-border px-3 py-3 text-right text-kb-text-body pr-4">{row.amount}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* 페이지네이션 */}
          <div className="flex justify-center mt-4">
            <button className="w-7 h-7 text-[12px] font-bold text-white flex items-center justify-center" style={{ backgroundColor: '#5BC9A8' }}>1</button>
          </div>
        </main>
      </div>
    </div>
  )
}
