'use client'

import Link from 'next/link'
import { useSearchParams } from 'next/navigation'
import { Suspense } from 'react'

function LoanResultContent() {
  const searchParams = useSearchParams()
  const product = searchParams.get('product') ?? 'AXful 대출'
  const rate = searchParams.get('rate') ?? '연 5.0%'
  const amount = parseInt(searchParams.get('amount') ?? '0', 10)
  const period = parseInt(searchParams.get('period') ?? '12', 10)
  const purpose = searchParams.get('purpose') ?? '-'

  const monthlyRate = 0.055 / 12
  const monthly =
    amount > 0
      ? Math.round((amount * monthlyRate * Math.pow(1 + monthlyRate, period)) /
          (Math.pow(1 + monthlyRate, period) - 1))
      : 0

  const isApproved = amount <= 30_000_000

  return (
    <div className="max-w-kb-container mx-auto px-6 py-4 pb-16">
      {/* 브레드크럼 */}
      <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
        <span>개인뱅킹</span><span>&gt;</span>
        <span>대출</span><span>&gt;</span>
        <Link href="/loans/apply" className="hover:underline">대출 신청</Link>
        <span>&gt;</span>
        <span className="font-semibold text-kb-text">신청 결과</span>
      </div>

      <h1 className="text-[22px] font-bold text-kb-text mb-6 pb-2 border-b-2 border-kb-text">대출 신청 결과</h1>

      {/* 결과 배너 */}
      <div className={`p-8 mb-6 text-center border rounded-xl ${
        isApproved
          ? 'border-kb-taupe bg-kb-yellow/10'
          : 'border-gray-300 bg-gray-50'
      }`}>
        {isApproved ? (
          <>
            <div className="w-16 h-16 rounded-full bg-kb-yellow flex items-center justify-center mx-auto mb-3">
              <svg viewBox="0 0 40 40" fill="none" className="w-9 h-9">
                <path d="M8 20l8 8 16-16" stroke="#333" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            <p className="text-[20px] font-bold text-kb-text mb-1">대출 승인</p>
            <p className="text-[14px] text-kb-text-muted">대출 신청이 승인되었습니다. 아래 내용을 확인해 주세요.</p>
          </>
        ) : (
          <>
            <div className="w-16 h-16 rounded-full bg-gray-200 flex items-center justify-center mx-auto mb-3">
              <svg viewBox="0 0 40 40" fill="none" className="w-9 h-9">
                <path d="M12 12l16 16M28 12L12 28" stroke="#888" strokeWidth="3" strokeLinecap="round" />
              </svg>
            </div>
            <p className="text-[20px] font-bold text-kb-text mb-1">대출 한도 초과</p>
            <p className="text-[14px] text-kb-text-muted">신청 금액이 승인 한도를 초과하였습니다. 금액을 조정하여 다시 신청해 주세요.</p>
          </>
        )}
      </div>

      {isApproved && (
        <>
          {/* 대출 상세 */}
          <section className="mb-6">
            <h2 className="text-lg font-bold text-kb-text mb-5 pb-2 border-b border-kb-border">대출 상세 내용</h2>
            <table className="w-full border-collapse text-[13px]">
              <tbody>
                <tr>
                  <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[160px]">대출 상품명</td>
                  <td className="border border-kb-border px-4 py-3 text-kb-text-body">{product}</td>
                  <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[120px]">대출 목적</td>
                  <td className="border border-kb-border px-4 py-3 text-kb-text-body">{purpose}</td>
                </tr>
                <tr>
                  <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text">대출 금액</td>
                  <td className="border border-kb-border px-4 py-3 font-bold text-kb-text text-[15px]">
                    {amount.toLocaleString('ko-KR')}원
                  </td>
                  <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text">대출 기간</td>
                  <td className="border border-kb-border px-4 py-3 text-kb-text-body">{period}개월</td>
                </tr>
                <tr>
                  <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text">적용 금리</td>
                  <td className="border border-kb-border px-4 py-3 text-kb-blue font-bold">{rate}</td>
                  <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text">월 납부액(예상)</td>
                  <td className="border border-kb-border px-4 py-3 font-bold text-kb-text">
                    {monthly > 0 ? `${monthly.toLocaleString('ko-KR')}원` : '-'}
                  </td>
                </tr>
              </tbody>
            </table>
          </section>

          {/* 안내 */}
          <div className="border border-[#b3cce8] bg-[#f0f6ff] p-4 mb-6 space-y-1">
            <p className="text-[13px] text-kb-text-body leading-relaxed">· 실제 대출 실행은 영업점 방문 또는 전화 확인 후 진행됩니다.</p>
            <p className="text-[13px] text-kb-text-body leading-relaxed">· 승인 결과는 7일간 유효하며, 이후에는 재신청이 필요합니다.</p>
            <p className="text-[13px] text-kb-text-body leading-relaxed">· 실제 금리는 심사 결과에 따라 달라질 수 있습니다.</p>
          </div>
        </>
      )}

      {/* 버튼 */}
      <div className="flex justify-center gap-3">
        <Link
          href="/loans/apply"
          className="px-10 py-3 border border-kb-border text-[14px] text-kb-text hover:bg-kb-beige-light transition-colors"
        >
          {isApproved ? '다시 신청' : '금액 조정 후 재신청'}
        </Link>
        {isApproved && (
          <button className="px-10 py-3 bg-kb-yellow text-[14px] font-bold text-kb-text hover:brightness-95 transition-all">
            대출 실행하기
          </button>
        )}
        <Link
          href="/dashboard"
          className="px-10 py-3 border border-kb-border text-[14px] text-kb-text hover:bg-kb-beige-light transition-colors"
        >
          홈으로
        </Link>
      </div>
    </div>
  )
}

export default function LoanApplyResultPage() {
  return (
    <Suspense fallback={<div className="max-w-kb-container mx-auto px-6 py-16 text-center text-kb-text-muted">로딩 중...</div>}>
      <LoanResultContent />
    </Suspense>
  )
}
