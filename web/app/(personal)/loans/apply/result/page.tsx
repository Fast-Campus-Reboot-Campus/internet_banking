'use client'

import Link from 'next/link'
import { useSearchParams } from 'next/navigation'
import { Suspense, useEffect, useState } from 'react'
import { bpsToRate, loanApplicationApi, loanMiscApi } from '@/lib/loan-api'

const STATUS_LABEL: Record<string, string> = {
  SUBMITTED: '접수완료', PRESCREENED: '가심사완료', REVIEWING: '심사중',
  APPROVED: '승인', REJECTED: '거절', CANCELLED: '취소', EXPIRED: '만료',
}

const AI_TRACK_LABEL: Record<string, string> = {
  TRACK_1: '빠른 심사', TRACK_2: '표준 심사', TRACK_3: '정밀 심사',
  FAST: '빠른 심사', STANDARD: '표준 심사', MANUAL: '수동 심사',
}

const REJECT_REASON_LABEL: Record<string, string> = {
  CREDIT_SCORE: '신용점수 기준 미충족',
  HIGH_DSR: '부채상환비율(DSR) 초과',
  INCOME_VERIFICATION: '소득 확인 불가',
  COLLATERAL_INSUFFICIENT: '담보 가치 부족',
  POLICY_REJECT: '내부 심사 기준 미충족',
  OTHER: '기타 심사 기준 미충족',
}

function LoanResultContent() {
  const searchParams = useSearchParams()
  const applId  = searchParams.get('applId')
  const amount  = parseInt(searchParams.get('amount') ?? '0', 10)
  const period  = parseInt(searchParams.get('period') ?? '12', 10)
  const purpose = searchParams.get('purpose') ?? '-'

  const [journey,  setJourney]  = useState<any>(null)
  const [history,  setHistory]  = useState<any[]>([])
  const [review,   setReview]   = useState<any>(null)
  const [loading,  setLoading]  = useState(!!applId)
  const [error,    setError]    = useState('')

  useEffect(() => {
    if (!applId) return
    loanApplicationApi.journey(parseInt(applId, 10))
      .then(({ data: res }) => setJourney(res.data))
      .catch(() => setError('신청 정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
    loanMiscApi.getStatusHistory('LOAN_APPLICATION', parseInt(applId, 10))
      .then(({ data: res }) => setHistory(res.data?.items ?? []))
      .catch(() => {})
    loanApplicationApi.getReview(parseInt(applId, 10))
      .then(({ data: res }) => setReview(res.data))
      .catch(() => {})
  }, [applId])

  if (loading) return <div className="py-20 text-center text-kb-text-muted">처리 중...</div>
  if (error)   return <div className="py-20 text-center text-kb-red">{error}</div>

  const application = journey?.application
  const statusCd    = application?.applStatusCd ?? ''
  const isApproved  = statusCd === 'APPROVED'
  const isRejected  = statusCd === 'REJECTED'
  const prodId      = application?.prodId

  const displayAmount  = application?.requestedAmount ?? amount
  const displayPeriod  = application?.requestedPeriodMo ?? period
  const monthlyRate    = 0.055 / 12
  const monthly = displayAmount > 0
    ? Math.round((displayAmount * monthlyRate * Math.pow(1 + monthlyRate, displayPeriod)) /
        (Math.pow(1 + monthlyRate, displayPeriod) - 1))
    : 0

  return (
    <div className="max-w-kb-container mx-auto px-6 py-4 pb-16">
      <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
        <span>개인뱅킹</span><span>&gt;</span>
        <span>대출</span><span>&gt;</span>
        <Link href="/loans/apply" className="hover:underline">대출 신청</Link>
        <span>&gt;</span>
        <span className="font-semibold text-kb-text">신청 결과</span>
      </div>

      <h1 className="text-[22px] font-bold text-kb-text mb-6 pb-2 border-b-2 border-kb-primary">대출 신청 결과</h1>

      {/* 상태 배너 */}
      <div className={`p-8 mb-6 text-center border rounded-xl ${
        isApproved ? 'border-kb-text bg-kb-primary/10'
        : isRejected ? 'border-red-300 bg-red-50'
        : 'border-kb-primary-border bg-kb-primary-bg'}`}>
        {isApproved ? (
          <>
            <div className="w-16 h-16 rounded-full bg-kb-primary flex items-center justify-center mx-auto mb-3">
              <svg viewBox="0 0 40 40" fill="none" className="w-9 h-9">
                <path d="M8 20l8 8 16-16" stroke="#333" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            <p className="text-[20px] font-bold text-kb-text mb-1">대출 승인</p>
            <p className="text-[14px] text-kb-text-muted">대출 신청이 승인되었습니다.</p>
          </>
        ) : isRejected ? (
          <>
            <div className="w-16 h-16 rounded-full bg-red-200 flex items-center justify-center mx-auto mb-3">
              <svg viewBox="0 0 40 40" fill="none" className="w-9 h-9">
                <path d="M12 12l16 16M28 12L12 28" stroke="#888" strokeWidth="3" strokeLinecap="round" />
              </svg>
            </div>
            <p className="text-[20px] font-bold text-kb-text mb-1">대출 거절</p>
            <p className="text-[14px] text-kb-text-muted">심사 결과 대출이 거절되었습니다.</p>
          </>
        ) : (
          <>
            <div className="w-16 h-16 rounded-full bg-kb-primary/50 flex items-center justify-center mx-auto mb-3">
              <svg viewBox="0 0 40 40" fill="none" className="w-9 h-9">
                <circle cx="20" cy="20" r="14" stroke="#333" strokeWidth="2.5"/>
                <path d="M20 13v9l5 3" stroke="#333" strokeWidth="2.5" strokeLinecap="round"/>
              </svg>
            </div>
            <p className="text-[20px] font-bold text-kb-text mb-1">
              {statusCd ? STATUS_LABEL[statusCd] ?? statusCd : '신청 접수완료'}
            </p>
            <p className="text-[14px] text-kb-text-muted">신청이 접수되었습니다. 심사 결과를 기다려 주세요.</p>
          </>
        )}
      </div>

      {/* AI 평가 트랙 배지 */}
      {review?.revAiTrackCd && (
        <div className="flex items-center gap-2 mb-4 px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-lg text-[13px]">
          <span className="text-gray-500">AI 평가 트랙</span>
          <span className={`px-2 py-0.5 rounded text-[11px] font-bold border ${
            review.revAiTrackCd === 'TRACK_1' || review.revAiTrackCd === 'FAST'
              ? 'bg-green-100 text-green-700 border-green-300'
              : review.revAiTrackCd === 'TRACK_2' || review.revAiTrackCd === 'STANDARD'
              ? 'bg-blue-100 text-blue-700 border-blue-300'
              : 'bg-orange-100 text-orange-700 border-orange-300'
          }`}>
            {AI_TRACK_LABEL[review.revAiTrackCd] ?? review.revAiTrackCd}
          </span>
        </div>
      )}

      {/* 거절 사유 안내 */}
      {isRejected && review?.rejectReasonCd && (
        <div className="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-[13px] text-red-700">
          <span className="font-semibold">거절 사유: </span>
          {REJECT_REASON_LABEL[review.rejectReasonCd] ?? '심사 기준 미충족'}
        </div>
      )}

      {/* 신청 상세 */}
      <section className="mb-6">
        <h2 className="text-lg font-bold text-kb-text mb-5 pb-2 border-b border-kb-primary-border">신청 상세 내용</h2>
        <table className="w-full border-collapse text-[13px]">
          <tbody>
            {applId && (
              <tr>
                <td className="border border-kb-primary-border bg-kb-primary-bg px-4 py-3 font-semibold text-kb-text w-[160px]">신청번호</td>
                <td className="border border-kb-primary-border px-4 py-3 text-kb-text-body">{application?.applNo ?? applId}</td>
                <td className="border border-kb-primary-border bg-kb-primary-bg px-4 py-3 font-semibold text-kb-text w-[120px]">진행상태</td>
                <td className="border border-kb-primary-border px-4 py-3 font-bold text-kb-text">{STATUS_LABEL[statusCd] ?? statusCd}</td>
              </tr>
            )}
            <tr>
              <td className="border border-kb-primary-border bg-kb-primary-bg px-4 py-3 font-semibold text-kb-text">신청 금액</td>
              <td className="border border-kb-primary-border px-4 py-3 font-bold text-kb-text text-[15px]">{displayAmount.toLocaleString('ko-KR')}원</td>
              <td className="border border-kb-primary-border bg-kb-primary-bg px-4 py-3 font-semibold text-kb-text">대출 기간</td>
              <td className="border border-kb-primary-border px-4 py-3 text-kb-text-body">{displayPeriod}개월</td>
            </tr>
            <tr>
              <td className="border border-kb-primary-border bg-kb-primary-bg px-4 py-3 font-semibold text-kb-text">대출 목적</td>
              <td className="border border-kb-primary-border px-4 py-3 text-kb-text-body">{purpose}</td>
              <td className="border border-kb-primary-border bg-kb-primary-bg px-4 py-3 font-semibold text-kb-text">월 납부액(예상)</td>
              <td className="border border-kb-primary-border px-4 py-3 font-bold text-kb-text">{monthly > 0 ? `${monthly.toLocaleString('ko-KR')}원` : '-'}</td>
            </tr>
          </tbody>
        </table>
      </section>

      {/* 심사 단계별 결과 */}
      {journey && (journey.prescreening || journey.creditEvaluation || journey.dsr) && (
        <section className="mb-6">
          <h2 className="text-lg font-bold text-kb-text mb-5 pb-2 border-b border-kb-primary-border">심사 진행 현황</h2>
          <div className="grid grid-cols-3 gap-4">
            {journey.prescreening && (
              <div className={`border rounded-xl p-5 ${journey.prescreening.resultCd === 'PASS' ? 'border-green-300 bg-green-50' : 'border-red-300 bg-red-50'}`}>
                <p className="text-[12px] text-kb-text-muted font-medium mb-2">1단계 · 가심사</p>
                <p className={`text-[16px] font-bold mb-2 ${journey.prescreening.resultCd === 'PASS' ? 'text-green-700' : 'text-red-700'}`}>
                  {journey.prescreening.resultCd === 'PASS' ? '통과' : '미통과'}
                </p>
                {journey.prescreening.maxAmount > 0 && (
                  <p className="text-[12px] text-kb-text-body">한도 {journey.prescreening.maxAmount.toLocaleString('ko-KR')}원</p>
                )}
                {journey.prescreening.rateBps > 0 && (
                  <p className="text-[12px] text-kb-text-body">예상금리 연 {bpsToRate(journey.prescreening.rateBps)}%</p>
                )}
              </div>
            )}
            {journey.creditEvaluation && (
              <div className={`border rounded-xl p-5 ${journey.creditEvaluation.resultCd === 'PASS' ? 'border-green-300 bg-green-50' : 'border-red-300 bg-red-50'}`}>
                <p className="text-[12px] text-kb-text-muted font-medium mb-2">2단계 · 신용평가</p>
                <p className={`text-[16px] font-bold mb-2 ${journey.creditEvaluation.resultCd === 'PASS' ? 'text-green-700' : 'text-red-700'}`}>
                  {journey.creditEvaluation.resultCd === 'PASS' ? '통과' : '미통과'}
                </p>
                {journey.creditEvaluation.creditScore > 0 && (
                  <p className="text-[12px] text-kb-text-body">신용점수 {journey.creditEvaluation.creditScore}점</p>
                )}
                {journey.creditEvaluation.rateBps > 0 && (
                  <p className="text-[12px] text-kb-text-body">적용금리 연 {bpsToRate(journey.creditEvaluation.rateBps)}%</p>
                )}
              </div>
            )}
            {journey.dsr && (
              <div className={`border rounded-xl p-5 ${journey.dsr.resultCd === 'PASS' ? 'border-green-300 bg-green-50' : 'border-red-300 bg-red-50'}`}>
                <p className="text-[12px] text-kb-text-muted font-medium mb-2">3단계 · DSR 산정</p>
                <p className={`text-[16px] font-bold mb-2 ${journey.dsr.resultCd === 'PASS' ? 'text-green-700' : 'text-red-700'}`}>
                  {journey.dsr.resultCd === 'PASS' ? '통과' : '미통과'}
                </p>
                {journey.dsr.dsrRatio > 0 && (
                  <p className="text-[12px] text-kb-text-body">DSR {(journey.dsr.dsrRatio * 100).toFixed(1)}%</p>
                )}
              </div>
            )}
          </div>
        </section>
      )}

      {/* 상태 변경 이력 */}
      {history.length > 0 && (
        <section className="mb-6">
          <h2 className="text-lg font-bold text-kb-text mb-5 pb-2 border-b border-kb-primary-border">신청 상태 변경 이력</h2>
          <table className="w-full border-collapse text-[13px]">
            <thead>
              <tr className="bg-kb-primary-bg">
                <th className="border border-kb-primary-border px-4 py-3 text-center font-semibold">변경 일시</th>
                <th className="border border-kb-primary-border px-4 py-3 text-center font-semibold">이전 상태</th>
                <th className="border border-kb-primary-border px-4 py-3 text-center font-semibold">변경 상태</th>
                <th className="border border-kb-primary-border px-4 py-3 text-center font-semibold">사유</th>
              </tr>
            </thead>
            <tbody>
              {history.map((h: any, i: number) => (
                <tr key={i} className="hover:bg-kb-primary-bg">
                  <td className="border border-kb-primary-border px-4 py-3 text-center text-kb-text-muted">
                    {h.changedAt ? h.changedAt.slice(0, 19).replace('T', ' ') : '-'}
                  </td>
                  <td className="border border-kb-primary-border px-4 py-3 text-center text-kb-text-muted">
                    {h.beforeStatusCd ?? '-'}
                  </td>
                  <td className="border border-kb-primary-border px-4 py-3 text-center font-bold text-kb-text">
                    {h.afterStatusCd ?? '-'}
                  </td>
                  <td className="border border-kb-primary-border px-4 py-3 text-center text-kb-text-muted">
                    {h.changeReasonCd ?? '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      <div className="border border-[#b3cce8] bg-[#f0f6ff] p-4 mb-6 space-y-1">
        <p className="text-[13px] text-kb-text-body leading-relaxed">· 실제 대출 실행은 영업점 방문 또는 전화 확인 후 진행됩니다.</p>
        <p className="text-[13px] text-kb-text-body leading-relaxed">· 승인 결과는 7일간 유효하며, 이후에는 재신청이 필요합니다.</p>
      </div>

      <div className="flex justify-center gap-3 flex-wrap">
        <Link href="/loans/apply"
          className="px-10 py-3 border border-kb-primary-border text-[14px] text-kb-text hover:bg-kb-primary-bg transition-colors">
          다시 신청
        </Link>
        {prodId && (
          <Link href={`/products/loan/credit/${prodId}`}
            className="px-10 py-3 border border-kb-primary-border text-[14px] text-kb-text hover:bg-kb-primary-bg transition-colors">
            상품 상세
          </Link>
        )}
        {applId && !isRejected && (
          <Link href={`/loans/apply/${applId}/collateral`}
            className="px-10 py-3 border border-kb-primary-border text-[14px] text-kb-text hover:bg-kb-primary-bg transition-colors">
            담보 등록
          </Link>
        )}
        {applId && !isRejected && (
          <Link href={`/loans/apply/${applId}/guarantor`}
            className="px-10 py-3 border border-kb-primary-border text-[14px] text-kb-text hover:bg-kb-primary-bg transition-colors">
            보증인 동의
          </Link>
        )}
        {applId && !isRejected && (
          <Link href={`/loans/apply/${applId}/documents`}
            className="px-10 py-3 border border-kb-text text-[14px] font-medium text-kb-text hover:bg-kb-primary-bg transition-colors">
            서류 제출
          </Link>
        )}
        <Link href="/dashboard"
          className="px-10 py-3 bg-kb-primary text-[14px] font-bold text-kb-text hover:brightness-95 transition-all">
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
