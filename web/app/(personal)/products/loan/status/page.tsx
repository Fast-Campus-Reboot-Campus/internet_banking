import Link from 'next/link'
import LoanSidebar from '@/components/inquiry/LoanSidebar'

export default function LoanStatusPage() {
  return (
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">
        <LoanSidebar />

        {/* ===== 본문 ===== */}
        <main className="flex-1 pl-8 pt-4 pb-12">
          {/* 브레드크럼 */}
          <div className="flex justify-end mb-2 text-[12px] text-kb-text-muted gap-1">
            <span>개인뱅킹</span><span>&gt;</span>
            <span>금융상품</span><span>&gt;</span>
            <span>대출</span><span>&gt;</span>
            <span>대출진행현황</span><span>&gt;</span>
            <span className="font-semibold text-kb-text">진행현황조회/실행/예약</span>
          </div>

          <h1 className="text-[20px] font-bold text-kb-text mb-6">진행현황조회/실행/예약</h1>

          <table className="w-full border-collapse text-[13px]">
            <thead>
              <tr className="bg-kb-beige-light">
                <th className="border border-kb-border px-4 py-3 text-center font-semibold">신청일자</th>
                <th className="border border-kb-border px-4 py-3 text-center font-semibold">상품명</th>
                <th className="border border-kb-border px-4 py-3 text-center font-semibold">진행상태</th>
                <th className="border border-kb-border px-4 py-3 text-center font-semibold">신청유효기간</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td colSpan={4} className="border border-kb-border py-12 text-center text-[13px] text-kb-text-muted">
                  조회 가능한 대출이 없습니다.
                </td>
              </tr>
            </tbody>
          </table>
        </main>
      </div>
    </div>
  )
}
