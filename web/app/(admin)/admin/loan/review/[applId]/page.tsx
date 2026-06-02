/* eslint-disable @typescript-eslint/no-explicit-any */
'use client'

import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'next/navigation'
import Link from 'next/link'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { adminReviewApi, loanApplicationApi } from '@/lib/loan-api'

const REV_STATUS: Record<string, { text: string; cls: string }> = {
  PENDING_APPROVAL:  { text: '확정 대기',   cls: 'bg-yellow-100 text-yellow-700 border-yellow-300' },
  PENDING_APPROVER:  { text: '승인자 대기', cls: 'bg-blue-100   text-blue-700   border-blue-300' },
  COMPLETED:         { text: '완료',         cls: 'bg-green-100  text-green-700  border-green-300' },
  EXPIRED:           { text: '만료',         cls: 'bg-gray-100   text-gray-500   border-gray-300' },
  BIAS_REVIEWING:    { text: '편향검토중',   cls: 'bg-red-100    text-red-700    border-red-300' },
}

const REJECT_REASONS = ['CREDIT_SCORE', 'HIGH_DSR', 'INCOME_VERIFICATION', 'COLLATERAL_INSUFFICIENT', 'POLICY_REJECT', 'OTHER']

export default function LoanReviewDetailPage() {
  const { applId } = useParams<{ applId: string }>()
  const numApplId = parseInt(applId, 10)

  // pre-review data
  const [prescreening, setPrescreening] = useState<any>(null)
  const [creditEval, setCreditEval]     = useState<any>(null)
  const [dsr, setDsr]                   = useState<any>(null)

  // review data
  const [review, setReview]             = useState<any>(null)
  const [advices, setAdvices]           = useState<any[]>([])
  const [checks, setChecks]             = useState<any[]>([])
  const [advisoryReports, setAdvisory]  = useState<any[]>([])

  const [loading, setLoading] = useState(true)
  const [busy, setBusy]       = useState(false)
  const [msg, setMsg]         = useState('')
  const [err, setErr]         = useState('')

  // review form state
  const [revType, setRevType]               = useState('MANUAL')
  const [reviewerId, setReviewerId]         = useState('1')
  const [approverId, setApproverId]         = useState('2')
  const [confirmRemark, setConfirmRemark]   = useState('')
  const [ackRemark, setAckRemark]           = useState('')
  const [overrideReason, setOverrideReason] = useState('')
  const [reviseDecision, setReviseDecision] = useState('APPROVED')
  const [reviseAmount, setReviseAmount]     = useState('')
  const [reviseRate, setReviseRate]         = useState('')
  const [reviseReject, setReviseReject]     = useState('CREDIT_SCORE')
  const [checkItem, setCheckItem]           = useState('')
  const [checkResult, setCheckResult]       = useState('PASS')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [psRes, ceRes, dsrRes, revRes] = await Promise.allSettled([
        loanApplicationApi.getPrescreening(numApplId),
        loanApplicationApi.getCreditEvaluation(numApplId),
        loanApplicationApi.getDsr(numApplId),
        adminReviewApi.get(numApplId),
      ])
      if (psRes.status  === 'fulfilled') setPrescreening(psRes.value.data?.data ?? null)
      if (ceRes.status  === 'fulfilled') setCreditEval(ceRes.value.data?.data ?? null)
      if (dsrRes.status === 'fulfilled') setDsr(dsrRes.value.data?.data ?? null)
      if (revRes.status === 'fulfilled') {
        const rev = revRes.value.data?.data ?? null
        setReview(rev)
        if (rev?.revId) {
          const [adv, chk, adr] = await Promise.all([
            adminReviewApi.getAdvices(rev.revId),
            adminReviewApi.getChecks(rev.revId),
            adminReviewApi.getAdvisoryReports(rev.revId),
          ])
          setAdvices(adv.data?.data ?? [])
          setChecks(chk.data?.data ?? [])
          setAdvisory(adr.data?.data ?? [])
        }
      }
    } catch {}
    finally { setLoading(false) }
  }, [numApplId])

  useEffect(() => { load() }, [load])

  function notify(m: string) { setMsg(m); setTimeout(() => setMsg(''), 3000) }
  function fail(m: string)   { setErr(m); setTimeout(() => setErr(''), 4000) }

  async function act(fn: () => Promise<any>, successMsg: string) {
    setBusy(true); setErr(''); setMsg('')
    try { await fn(); notify(successMsg); await load() }
    catch (e: any) { fail(e?.response?.data?.message ?? '처리 실패') }
    finally { setBusy(false) }
  }

  const status = review?.revStatusCd
  const revId  = review?.revId

  // 가심사 통과 여부
  const psPass  = prescreening?.prescResultCd === 'PASS'
  const psDone  = !!prescreening
  const ceDone  = !!creditEval
  const dsrDone = !!dsr

  return (
    <div className="flex min-h-screen bg-gray-50">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-gray-200 px-6 py-3 text-xs text-gray-500">
          대출 &gt; <Link href="/admin/loan/review" className="hover:underline">본심사 목록</Link> &gt;{' '}
          <span className="text-gray-800 font-medium">신청 #{applId} 심사</span>
        </div>

        <div className="px-6 py-5 max-w-5xl">
          <h1 className="text-lg font-bold text-gray-800 mb-5">신청 #{applId} · 심사 상세</h1>

          {msg && <div className="mb-4 px-4 py-2 bg-green-50 border border-green-300 text-green-700 text-sm rounded">{msg}</div>}
          {err && <div className="mb-4 px-4 py-2 bg-red-50 border border-red-300 text-red-700 text-sm rounded">{err}</div>}

          {loading ? (
            <p className="py-10 text-center text-sm text-gray-400">불러오는 중...</p>
          ) : (
            <div className="space-y-6">

              {/* ── 1단계: 가심사 ─────────────────────────────── */}
              <Section title="1단계 · 가심사 (Prescreening)">
                {psDone ? (
                  <div className="grid grid-cols-4 gap-4 text-[13px] mb-3">
                    <KV k="결과" v={
                      <span className={`font-bold ${prescreening.prescResultCd === 'PASS' ? 'text-green-600' : 'text-red-500'}`}>
                        {prescreening.prescResultCd}
                      </span>
                    } />
                    <KV k="예상 한도" v={prescreening.estimatedLimit != null ? `${(prescreening.estimatedLimit / 10000).toLocaleString('ko-KR')}만원` : '-'} />
                    <KV k="예상 금리" v={prescreening.estimatedRateBps != null ? `${(prescreening.estimatedRateBps / 100).toFixed(2)}%` : '-'} />
                    <KV k="거절 사유" v={prescreening.rejectReasonCd ?? '-'} />
                  </div>
                ) : (
                  <p className="text-sm text-gray-400 mb-3">가심사 미실행</p>
                )}
                {!psDone && (
                  <Btn label="가심사 실행" disabled={busy}
                    onClick={() => act(() => loanApplicationApi.runPrescreening(numApplId), '가심사가 완료되었습니다.')} />
                )}
                {psDone && !psPass && (
                  <p className="text-[12px] text-red-600 mt-1">가심사 거절 — 이후 단계를 진행할 수 없습니다.</p>
                )}
              </Section>

              {/* ── 2단계: 신용평가 ───────────────────────────── */}
              <Section title="2단계 · 신용평가 (Credit Evaluation)">
                {ceDone ? (
                  <div className="grid grid-cols-4 gap-4 text-[13px] mb-3">
                    <KV k="결정" v={
                      <span className={`font-bold ${creditEval.cevalDecisionCd === 'APPROVE' ? 'text-green-600' : 'text-red-500'}`}>
                        {creditEval.cevalDecisionCd}
                      </span>
                    } />
                    <KV k="신용점수" v={creditEval.creditScore != null ? `${creditEval.creditScore}점` : '-'} />
                    <KV k="신용등급" v={creditEval.creditGrade ?? '-'} />
                    <KV k="PD" v={creditEval.pdValue != null ? `${(creditEval.pdValue * 100).toFixed(2)}%` : '-'} />
                  </div>
                ) : (
                  <p className="text-sm text-gray-400 mb-3">신용평가 미실행</p>
                )}
                {!ceDone && psPass && (
                  <Btn label="신용평가 실행" disabled={busy}
                    onClick={() => act(() => loanApplicationApi.runCreditEvaluation(numApplId), '신용평가가 완료되었습니다.')} />
                )}
                {!ceDone && !psPass && (
                  <p className="text-[12px] text-gray-400">가심사 통과 후 실행 가능</p>
                )}
              </Section>

              {/* ── 3단계: DSR ────────────────────────────────── */}
              <Section title="3단계 · DSR 산정">
                {dsrDone ? (
                  <div className="grid grid-cols-3 gap-4 text-[13px] mb-3">
                    <KV k="결과" v={
                      <span className={`font-bold ${dsr.dsrStatusCd === 'PASS' ? 'text-green-600' : 'text-red-500'}`}>
                        {dsr.dsrStatusCd}
                      </span>
                    } />
                    <KV k="DSR 비율" v={dsr.ratioBps != null ? `${(dsr.ratioBps / 100).toFixed(1)}%` : '-'} />
                    <KV k="한도 비율" v={dsr.limitBps != null ? `${(dsr.limitBps / 100).toFixed(1)}%` : '-'} />
                  </div>
                ) : (
                  <p className="text-sm text-gray-400 mb-3">DSR 미산정</p>
                )}
                {!dsrDone && ceDone && (
                  <Btn label="DSR 실행" disabled={busy}
                    onClick={() => act(() => loanApplicationApi.runDsr(numApplId), 'DSR 산정이 완료되었습니다.')} />
                )}
                {!dsrDone && !ceDone && (
                  <p className="text-[12px] text-gray-400">신용평가 완료 후 실행 가능</p>
                )}
              </Section>

              {/* ── 4단계: 본심사 ─────────────────────────────── */}
              <Section title="4단계 · 본심사 (Final Review)">
                {review ? (
                  <div className="grid grid-cols-4 gap-4 text-[13px] mb-4">
                    <KV k="심사 ID" v={review.revId} />
                    <KV k="심사 유형" v={review.revTypeCd === 'AUTO' ? '자동' : '수동'} />
                    <KV k="심사 상태" v={
                      <span className={`text-[11px] px-2 py-0.5 rounded border ${REV_STATUS[review.revStatusCd]?.cls ?? ''}`}>
                        {REV_STATUS[review.revStatusCd]?.text ?? review.revStatusCd}
                      </span>
                    } />
                    <KV k="권고결정" v={review.revDecisionCd ?? '-'} />
                    <KV k="승인금액" v={review.approvedAmount != null ? `${(review.approvedAmount / 10000).toLocaleString('ko-KR')}만원` : '-'} />
                    <KV k="승인금리" v={review.approvedRateBps != null ? `${(review.approvedRateBps / 100).toFixed(2)}%` : '-'} />
                    <KV k="거절사유" v={review.rejectReasonCd ?? '-'} />
                    <KV k="편향 심각도" v={review.biasSeverityCd ?? '-'} />
                    {review.revAiTrackCd && (
                      <KV k="AI 트랙" v={
                        <span className={`text-[11px] px-2 py-0.5 rounded border font-semibold ${
                          review.revAiTrackCd === 'TRACK_1' ? 'bg-green-100 text-green-700 border-green-300' :
                          review.revAiTrackCd === 'TRACK_2' ? 'bg-yellow-100 text-yellow-700 border-yellow-300' :
                          'bg-red-100 text-red-700 border-red-300'}`}>
                          {review.revAiTrackCd}
                        </span>
                      } />
                    )}
                    {review.revAiPd != null && (
                      <KV k="AI PD" v={`${(review.revAiPd * 100).toFixed(2)}%`} />
                    )}
                    {review.revAiRationale && (
                      <div className="col-span-4">
                        <p className="text-[11px] text-gray-400 mb-0.5">AI 근거</p>
                        <p className="text-[12px] text-gray-700 whitespace-pre-wrap">{review.revAiRationale}</p>
                      </div>
                    )}
                  </div>
                ) : (
                  <p className="text-sm text-gray-400 mb-3">본심사 미실행</p>
                )}

                {/* 심사 실행 */}
                {(!review || status === 'EXPIRED') && (
                  <div className="flex flex-wrap gap-3 items-end mt-2">
                    <label className="text-[12px] text-gray-600">
                      유형
                      <select value={revType} onChange={e => setRevType(e.target.value)}
                        className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px]">
                        <option value="MANUAL">수동</option>
                        <option value="AUTO">자동</option>
                      </select>
                    </label>
                    <label className="text-[12px] text-gray-600">
                      심사자 ID
                      <input type="number" value={reviewerId} onChange={e => setReviewerId(e.target.value)}
                        className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px] w-20" />
                    </label>
                    <Btn label="본심사 시작" disabled={busy || !dsrDone}
                      onClick={() => act(() => adminReviewApi.run(numApplId, { revTypeCd: revType, reviewerId: parseInt(reviewerId) }), '본심사가 시작되었습니다.')} />
                    <Btn label="자동 결정" disabled={busy || !dsrDone} variant="outline"
                      onClick={() => act(() => adminReviewApi.autoDecide(numApplId), '자동 결정이 완료되었습니다.')} />
                    {!dsrDone && <span className="text-[11px] text-gray-400">DSR 완료 후 본심사 가능</span>}
                  </div>
                )}

                {/* 확정 (PENDING_APPROVAL) */}
                {status === 'PENDING_APPROVAL' && (
                  <div className="flex flex-wrap gap-3 items-end mt-2 pt-3 border-t border-gray-100">
                    <label className="text-[12px] text-gray-600">
                      확정자 ID
                      <input type="number" value={reviewerId} onChange={e => setReviewerId(e.target.value)}
                        className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px] w-20" />
                    </label>
                    <label className="text-[12px] text-gray-600">
                      비고
                      <input type="text" value={confirmRemark} onChange={e => setConfirmRemark(e.target.value)}
                        placeholder="(선택)"
                        className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px] w-48" />
                    </label>
                    <Btn label="심사 확정" disabled={busy}
                      onClick={() => act(() => adminReviewApi.confirm(numApplId, { reviewerId: parseInt(reviewerId), confirmRemark }), '심사가 확정되었습니다.')} />
                  </div>
                )}

                {/* 편향 인지 (BIAS_REVIEWING) */}
                {status === 'BIAS_REVIEWING' && (
                  <div className="mt-2 pt-3 border-t border-gray-100">
                    <div className="mb-3 text-[13px] text-red-700 bg-red-50 border border-red-200 rounded px-3 py-2">
                      AI 편향 감지: <strong>{review.biasSeverityCd}</strong>
                      {review.biasSeverityCd === 'BLOCKED' && ' — 오버라이드 필요'}
                    </div>
                    {review.biasSeverityCd === 'BLOCKED' ? (
                      <div className="flex flex-wrap gap-3 items-end">
                        <label className="text-[12px] text-gray-600">
                          오버라이드 사유
                          <input type="text" value={overrideReason} onChange={e => setOverrideReason(e.target.value)}
                            className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px] w-64" />
                        </label>
                        <label className="text-[12px] text-gray-600">
                          담당자 ID
                          <input type="number" value={approverId} onChange={e => setApproverId(e.target.value)}
                            className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px] w-20" />
                        </label>
                        <Btn label="편향 오버라이드" disabled={busy || !overrideReason}
                          onClick={() => act(() => adminReviewApi.biasOverride(revId, { overrideBy: parseInt(approverId), overrideReason }), '오버라이드가 완료되었습니다.')} />
                      </div>
                    ) : (
                      <div className="flex flex-wrap gap-3 items-end">
                        <label className="text-[12px] text-gray-600">
                          인지 비고
                          <input type="text" value={ackRemark} onChange={e => setAckRemark(e.target.value)}
                            placeholder="(선택)"
                            className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px] w-64" />
                        </label>
                        <Btn label="편향 인지 처리" disabled={busy}
                          onClick={() => act(() => adminReviewApi.acknowledgeBias(numApplId, { acknowledgeRemark: ackRemark }), '편향 인지가 처리되었습니다.')} />
                      </div>
                    )}
                  </div>
                )}

                {/* 승인자 승인 (PENDING_APPROVER) */}
                {status === 'PENDING_APPROVER' && (
                  <div className="mt-2 pt-3 border-t border-gray-100">
                    {advisoryReports.some(r => r.severityCd === 'CRITICAL' && r.advrStatusCd !== 'ACKED' && r.advrStatusCd !== 'RESOLVED') && (
                      <div className="mb-3 px-3 py-2 bg-red-100 border border-red-400 text-red-800 text-[12px] font-semibold rounded">
                        ⚠ CRITICAL 미해소 Advisory 리포트가 있습니다 — 승인 전 검토 필요
                      </div>
                    )}
                    <p className="text-[12px] text-gray-500 mb-2">4-eye 원칙: 심사자와 다른 ID를 입력하세요.</p>
                    <div className="flex flex-wrap gap-3 items-end">
                      <label className="text-[12px] text-gray-600">
                        승인자 ID
                        <input type="number" value={approverId} onChange={e => setApproverId(e.target.value)}
                          className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px] w-20" />
                      </label>
                      <Btn label="승인" disabled={busy}
                        onClick={() => act(() => adminReviewApi.approverApprove(numApplId, { approverId: parseInt(approverId), approvalDecision: 'APPROVED' }), '승인이 완료되었습니다.')} />
                      <Btn label="반려" disabled={busy} variant="danger"
                        onClick={() => act(() => adminReviewApi.approverApprove(numApplId, { approverId: parseInt(approverId), approvalDecision: 'REJECTED' }), '반려 처리되었습니다.')} />
                    </div>
                  </div>
                )}

                {/* 결정 정정 (COMPLETED) */}
                {status === 'COMPLETED' && (
                  <div className="mt-2 pt-3 border-t border-gray-100">
                    <p className="text-[12px] text-gray-500 mb-2">결정 정정</p>
                    <div className="flex flex-wrap gap-3 items-end">
                      <label className="text-[12px] text-gray-600">
                        결정
                        <select value={reviseDecision} onChange={e => setReviseDecision(e.target.value)}
                          className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px]">
                          <option value="APPROVED">승인</option>
                          <option value="REJECTED">거절</option>
                        </select>
                      </label>
                      {reviseDecision === 'APPROVED' ? (
                        <>
                          <label className="text-[12px] text-gray-600">
                            승인금액(원)
                            <input type="number" value={reviseAmount} onChange={e => setReviseAmount(e.target.value)}
                              className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px] w-32" />
                          </label>
                          <label className="text-[12px] text-gray-600">
                            금리(bps)
                            <input type="number" value={reviseRate} onChange={e => setReviseRate(e.target.value)}
                              className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px] w-24" />
                          </label>
                        </>
                      ) : (
                        <label className="text-[12px] text-gray-600">
                          거절사유
                          <select value={reviseReject} onChange={e => setReviseReject(e.target.value)}
                            className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px]">
                            {REJECT_REASONS.map(r => <option key={r} value={r}>{r}</option>)}
                          </select>
                        </label>
                      )}
                      <Btn label="정정 저장" disabled={busy}
                        onClick={() => {
                          const body: any = { revDecisionCd: reviseDecision }
                          if (reviseDecision === 'APPROVED') {
                            if (reviseAmount) body.approvedAmount = parseInt(reviseAmount)
                            if (reviseRate)   body.approvedRateBps = parseInt(reviseRate)
                          } else {
                            body.rejectReasonCd = reviseReject
                          }
                          act(() => adminReviewApi.revise(numApplId, body), '결정이 정정되었습니다.')
                        }} />
                    </div>
                  </div>
                )}
              </Section>

              {/* AI 어드바이스 */}
              {advices.length > 0 && (
                <Section title={`AI 어드바이스 (${advices.length}건)`}>
                  <div className="space-y-2">
                    {advices.map((a: any, i: number) => (
                      <div key={i} className="text-[12px] bg-blue-50 border border-blue-200 rounded px-3 py-2">
                        <span className={`inline-block px-1.5 py-0.5 rounded text-[10px] font-bold mr-2 ${
                          a.severityCd === 'HIGH' ? 'bg-red-200 text-red-800' :
                          a.severityCd === 'MEDIUM' ? 'bg-orange-200 text-orange-800' :
                          'bg-gray-200 text-gray-700'}`}>{a.severityCd}</span>
                        <span className="text-blue-900">{a.adviceContent ?? a.content ?? JSON.stringify(a)}</span>
                      </div>
                    ))}
                  </div>
                </Section>
              )}

              {/* Review Advisory 리포트 (advisory-service) */}
              {advisoryReports.length > 0 && (
                <Section title={`Review Advisory 리포트 (${advisoryReports.length}건)`}>
                  <div className="space-y-2">
                    {advisoryReports.map((r: any, i: number) => (
                      <div key={i} className={`text-[12px] border rounded px-3 py-2 ${
                        r.severityCd === 'CRITICAL' ? 'bg-red-50 border-red-300' :
                        r.severityCd === 'HIGH'     ? 'bg-orange-50 border-orange-300' :
                        r.severityCd === 'MEDIUM'   ? 'bg-yellow-50 border-yellow-200' :
                        'bg-gray-50 border-gray-200'}`}>
                        <div className="flex items-center gap-2 mb-1">
                          <span className={`text-[10px] px-1.5 py-0.5 rounded font-bold ${
                            r.severityCd === 'CRITICAL' ? 'bg-red-200 text-red-800' :
                            r.severityCd === 'HIGH'     ? 'bg-orange-200 text-orange-800' :
                            r.severityCd === 'MEDIUM'   ? 'bg-yellow-200 text-yellow-800' :
                            'bg-gray-200 text-gray-700'}`}>{r.severityCd}</span>
                          <span className="font-semibold text-gray-800">{r.advrTitle}</span>
                          <span className="ml-auto text-[10px] text-gray-400">{r.advrStatusCd}</span>
                        </div>
                        {r.advrSummary && <p className="text-gray-600">{r.advrSummary}</p>}
                      </div>
                    ))}
                  </div>
                </Section>
              )}

              {/* 체크 로그 */}
              <Section title="체크 로그">
                {checks.length > 0 && (
                  <table className="w-full text-[12px] mb-4">
                    <thead>
                      <tr className="bg-gray-50 border-b">
                        {['항목', '결과', '비고', '기록일시'].map(h => (
                          <th key={h} className="px-3 py-2 text-left font-semibold text-gray-600">{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {checks.map((c: any, i: number) => (
                        <tr key={i}>
                          <td className="px-3 py-2 text-gray-700">{c.checkItem}</td>
                          <td className="px-3 py-2">
                            <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${c.checkResult === 'PASS' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                              {c.checkResult}
                            </span>
                          </td>
                          <td className="px-3 py-2 text-gray-500">{c.remark ?? '-'}</td>
                          <td className="px-3 py-2 text-gray-400">{c.checkedAt?.slice(0, 16).replace('T', ' ') ?? '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
                {revId && (
                  <div className="flex gap-3 items-end">
                    <label className="text-[12px] text-gray-600">
                      체크 항목
                      <input type="text" value={checkItem} onChange={e => setCheckItem(e.target.value)}
                        className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px] w-48" />
                    </label>
                    <label className="text-[12px] text-gray-600">
                      결과
                      <select value={checkResult} onChange={e => setCheckResult(e.target.value)}
                        className="ml-2 border border-gray-300 rounded px-2 py-1 text-[12px]">
                        <option value="PASS">PASS</option>
                        <option value="FAIL">FAIL</option>
                      </select>
                    </label>
                    <Btn label="체크 추가" disabled={busy || !checkItem}
                      onClick={() => act(
                        () => adminReviewApi.addCheck(revId, { checkItem, checkResult }),
                        '체크가 추가되었습니다.'
                      ).then(() => setCheckItem(''))} />
                  </div>
                )}
              </Section>

            </div>
          )}
        </div>
      </main>
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5">
      <h2 className="text-[13px] font-semibold text-gray-700 mb-4 pb-2 border-b border-gray-100">{title}</h2>
      {children}
    </div>
  )
}

function KV({ k, v }: { k: string; v: React.ReactNode }) {
  return (
    <div>
      <p className="text-[11px] text-gray-400 mb-0.5">{k}</p>
      <p className="text-[13px] font-medium text-gray-800">{v}</p>
    </div>
  )
}

function Btn({ label, onClick, disabled, variant = 'primary' }: {
  label: string; onClick: () => void; disabled?: boolean; variant?: 'primary' | 'outline' | 'danger'
}) {
  const cls = variant === 'danger'
    ? 'bg-red-600 text-white hover:opacity-90'
    : variant === 'outline'
    ? 'border border-gray-300 text-gray-700 hover:bg-gray-50'
    : 'bg-[#1B3A6B] text-white hover:opacity-90'
  return (
    <button onClick={onClick} disabled={disabled}
      className={`px-4 py-1.5 text-[12px] rounded transition-all disabled:opacity-50 ${cls}`}>
      {label}
    </button>
  )
}
