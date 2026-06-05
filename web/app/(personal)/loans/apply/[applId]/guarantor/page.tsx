'use client'

import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'next/navigation'
import Link from 'next/link'
import { guarantorApi } from '@/lib/loan-api'

const RELATION_TYPES = [
  { code: 'SPOUSE', label: '배우자' },
  { code: 'PARENT', label: '부모' },
  { code: 'CHILD',  label: '자녀' },
  { code: 'FAMILY', label: '가족' },
  { code: 'FRIEND', label: '지인' },
]

const GAGR_TYPE = [
  { code: 'JOINT',   label: '연대보증', desc: '채무 전액에 대해 연대 책임' },
  { code: 'PARTIAL', label: '부분보증', desc: '보증 금액 한도 내 책임' },
]

const STATUS_LABEL: Record<string, { text: string; cls: string }> = {
  REGISTERED: { text: '서명 대기',   cls: 'border-yellow-400 text-yellow-700 bg-yellow-50' },
  SIGNED:     { text: '서명 완료',   cls: 'border-green-400  text-green-700  bg-green-50' },
  CANCELED:   { text: '취소됨',      cls: 'border-gray-300   text-gray-400   bg-gray-50' },
}

function formatMobile(raw: string) {
  const d = raw.replace(/\D/g, '').slice(0, 11)
  if (d.length <= 3) return d
  if (d.length <= 7) return `${d.slice(0, 3)}-${d.slice(3)}`
  return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`
}

type GagrItem = {
  gagrId: number
  guarantorNameMasked: string
  mobileNoMasked: string
  relationTypeCd: string
  gagrTypeCd: string
  guaranteeAmount: number
  gagrStatusCd: string
  consentedAt: string | null
}

export default function GuarantorPage() {
  const { applId } = useParams<{ applId: string }>()

  const [items, setItems]       = useState<GagrItem[]>([])
  const [loading, setLoading]   = useState(true)

  // 등록 폼
  const [name, setName]               = useState('')
  const [mobile, setMobile]           = useState('')
  const [relation, setRelation]       = useState('SPOUSE')
  const [gagrType, setGagrType]       = useState('JOINT')
  const [amount, setAmount]           = useState('')
  const [submitting, setSubmitting]   = useState(false)
  const [formError, setFormError]     = useState('')

  // 서명 모달
  const [signingId, setSigningId]     = useState<number | null>(null)
  const [signingError, setSiginingError] = useState('')

  // 취소 모달
  const [cancelingId, setCancelingId] = useState<number | null>(null)
  const [cancelRemark, setCancelRemark] = useState('')
  const [actionLoading, setActionLoading] = useState(false)

  const load = useCallback(async () => {
    try {
      const { data: res } = await guarantorApi.list(Number(applId))
      setItems(res.data?.items ?? [])
    } catch {} finally { setLoading(false) }
  }, [applId])

  useEffect(() => { load() }, [load])

  async function handleRegister() {
    const digits = mobile.replace(/\D/g, '')
    if (!name.trim()) { setFormError('보증인 성명을 입력해 주세요.'); return }
    if (digits.length < 7) { setFormError('휴대폰 번호를 올바르게 입력해 주세요.'); return }
    if (!amount) { setFormError('보증 금액을 입력해 주세요.'); return }
    setSubmitting(true); setFormError('')
    try {
      await guarantorApi.register(Number(applId), {
        guarantorName:     name.trim(),
        guarantorMobileNo: digits,
        relationTypeCd:    relation,
        gagrTypeCd:        gagrType,
        guaranteeAmount:   parseInt(amount.replace(/,/g, '')),
      })
      setName(''); setMobile(''); setAmount('')
      await load()
    } catch (err: any) {
      setFormError(err.response?.data?.message ?? '보증인 등록 중 오류가 발생했습니다.')
    } finally { setSubmitting(false) }
  }

  async function handleSign(gagrId: number) {
    setActionLoading(true); setSiginingError('')
    try {
      // stub: URL·해시는 서버에서 의미 없이 저장됨
      await guarantorApi.sign(Number(applId), gagrId, {
        signedDocUrl:  `https://docs.stub/guarantor/${gagrId}.pdf`,
        signedDocHash: `stub-hash-${gagrId}`,
      })
      setSigningId(null)
      await load()
    } catch (err: any) {
      setSiginingError(err.response?.data?.message ?? '서명 처리 중 오류가 발생했습니다.')
    } finally { setActionLoading(false) }
  }

  async function handleCancel(gagrId: number) {
    setActionLoading(true)
    try {
      await guarantorApi.cancel(Number(applId), gagrId, {
        cancelReasonCd: 'USER_REQUEST',
        cancelRemark:   cancelRemark || undefined,
      })
      setCancelingId(null); setCancelRemark('')
      await load()
    } catch (err: any) {
      setFormError(err.response?.data?.message ?? '취소 처리 중 오류가 발생했습니다.')
    } finally { setActionLoading(false) }
  }

  const canSubmit = !!name.trim() && mobile.replace(/\D/g, '').length >= 7 && !!amount && !submitting

  return (
    <div className="max-w-kb-container mx-auto px-6 py-4 pb-16">
      <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
        <span>개인뱅킹</span><span>&gt;</span>
        <span>대출</span><span>&gt;</span>
        <Link href="/loans/apply" className="hover:underline">대출 신청</Link>
        <span>&gt;</span>
        <span className="font-semibold text-kb-text">보증인 동의</span>
      </div>

      <h1 className="text-[22px] font-bold text-kb-text mb-6 pb-2 border-b-2 border-kb-primary">보증인 동의</h1>

      <div className="border border-kb-primary-border bg-kb-primary-bg p-4 mb-6 text-[13px] text-kb-text-body leading-relaxed">
        <p>· 보증인을 등록한 후 전자서명을 완료해야 보증이 유효합니다.</p>
        <p>· 보증인의 실명·휴대폰 번호는 마스킹 처리됩니다.</p>
      </div>

      {/* 등록된 보증인 목록 */}
      {!loading && items.length > 0 && (
        <section className="mb-8">
          <h2 className="text-lg font-bold text-kb-text mb-5 pb-2 border-b border-kb-primary-border">등록된 보증인</h2>
          <div className="space-y-3">
            {items.map(g => {
              const status = STATUS_LABEL[g.gagrStatusCd] ?? { text: g.gagrStatusCd, cls: 'border-kb-primary-border text-kb-text-muted' }
              return (
                <div key={g.gagrId} className="border border-kb-primary-border rounded-xl overflow-hidden">
                  <div className="bg-kb-primary-bg px-5 py-3 flex justify-between items-center border-b border-kb-primary-border">
                    <div className="flex items-center gap-3">
                      <span className="text-[13px] font-bold text-kb-text">{g.guarantorNameMasked}</span>
                      <span className="text-[12px] text-kb-text-muted">{g.mobileNoMasked}</span>
                      <span className="text-[11px] text-kb-text-muted">
                        {RELATION_TYPES.find(r => r.code === g.relationTypeCd)?.label ?? g.relationTypeCd}
                      </span>
                    </div>
                    <span className={`text-[11px] px-2 py-0.5 rounded border ${status.cls}`}>
                      {status.text}
                    </span>
                  </div>
                  <div className="px-5 py-3 text-[13px] flex items-center gap-6 text-kb-text-body">
                    <span>
                      <span className="font-medium text-kb-text">보증 유형</span>:{' '}
                      {GAGR_TYPE.find(t => t.code === g.gagrTypeCd)?.label ?? g.gagrTypeCd}
                    </span>
                    <span>
                      <span className="font-medium text-kb-text">보증 금액</span>:{' '}
                      {g.guaranteeAmount.toLocaleString('ko-KR')}원
                    </span>
                    {g.consentedAt && (
                      <span className="text-kb-text-muted text-[12px]">
                        동의: {g.consentedAt.slice(0, 16).replace('T', ' ')}
                      </span>
                    )}
                  </div>
                  {g.gagrStatusCd !== 'CANCELED' && (
                    <div className="px-5 py-3 border-t border-kb-primary-border flex gap-2">
                      {g.gagrStatusCd === 'REGISTERED' && (
                        <button onClick={() => { setSigningId(g.gagrId); setSiginingError('') }}
                          className="px-5 py-1.5 bg-kb-primary border border-kb-text text-[12px] font-bold text-kb-text hover:brightness-95 transition-all">
                          전자서명
                        </button>
                      )}
                      <button onClick={() => { setCancelingId(g.gagrId); setCancelRemark('') }}
                        className="px-5 py-1.5 border border-kb-primary-border text-[12px] text-kb-text-muted hover:bg-kb-primary-bg transition-colors">
                        취소
                      </button>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </section>
      )}

      {/* 보증인 등록 폼 */}
      <section className="mb-6">
        <h2 className="text-lg font-bold text-kb-text mb-5 pb-2 border-b border-kb-primary-border">보증인 등록</h2>
        <div className="border border-kb-primary-border divide-y divide-kb-border overflow-hidden">

          <div className="flex items-center px-5 py-4 gap-4">
            <label className="w-36 text-[13px] font-medium text-kb-text flex-shrink-0">
              보증인 성명 <span className="text-kb-red">*</span>
            </label>
            <input value={name} onChange={e => setName(e.target.value)}
              placeholder="실명 입력"
              className="border border-kb-primary-border px-3 py-2 text-[13px] focus:outline-none w-48" />
          </div>

          <div className="flex items-center px-5 py-4 gap-4">
            <label className="w-36 text-[13px] font-medium text-kb-text flex-shrink-0">
              휴대폰 번호 <span className="text-kb-red">*</span>
            </label>
            <input type="tel"
              value={formatMobile(mobile)}
              onChange={e => setMobile(e.target.value.replace(/\D/g, '').slice(0, 11))}
              placeholder="010-0000-0000"
              className="border border-kb-primary-border px-3 py-2 text-[13px] focus:outline-none w-48" />
          </div>

          <div className="flex items-center px-5 py-4 gap-4">
            <label className="w-36 text-[13px] font-medium text-kb-text flex-shrink-0">관계</label>
            <div className="flex gap-2 flex-wrap">
              {RELATION_TYPES.map(r => (
                <button key={r.code} onClick={() => setRelation(r.code)}
                  className={`px-4 py-1.5 border text-[12px] rounded-lg transition-colors ${
                    relation === r.code
                      ? 'bg-kb-primary border-kb-text font-bold text-kb-text'
                      : 'border-kb-primary-border text-kb-text-muted hover:bg-kb-primary-bg'}`}>
                  {r.label}
                </button>
              ))}
            </div>
          </div>

          <div className="flex items-start px-5 py-4 gap-4">
            <label className="w-36 text-[13px] font-medium text-kb-text flex-shrink-0 pt-2">
              보증 유형 <span className="text-kb-red">*</span>
            </label>
            <div className="flex gap-3">
              {GAGR_TYPE.map(t => (
                <button key={t.code} onClick={() => setGagrType(t.code)}
                  className={`border rounded-xl p-4 text-left transition-colors w-44 ${
                    gagrType === t.code
                      ? 'border-kb-text bg-kb-primary/20'
                      : 'border-kb-primary-border hover:bg-kb-primary-bg'}`}>
                  <p className="text-[13px] font-bold text-kb-text mb-1">{t.label}</p>
                  <p className="text-[11px] text-kb-text-muted">{t.desc}</p>
                </button>
              ))}
            </div>
          </div>

          <div className="flex items-center px-5 py-4 gap-4">
            <label className="w-36 text-[13px] font-medium text-kb-text flex-shrink-0">
              보증 금액 <span className="text-kb-red">*</span>
            </label>
            <div className="flex items-center gap-2">
              <input type="text"
                value={amount ? parseInt(amount.replace(/,/g, '')).toLocaleString('ko-KR') : ''}
                onChange={e => setAmount(e.target.value.replace(/[^\d]/g, ''))}
                placeholder="0"
                className="border border-kb-primary-border px-3 py-2 text-[13px] w-40 focus:outline-none text-right" />
              <span className="text-[13px] text-kb-text">원</span>
            </div>
          </div>
        </div>
      </section>

      {formError && <p className="text-center text-[13px] text-kb-red mb-4">{formError}</p>}

      <div className="flex justify-center gap-3 mb-8">
        <Link href={`/loans/apply/result?applId=${applId}`}
          className="px-10 py-3 border border-kb-primary-border text-[14px] text-kb-text hover:bg-kb-primary-bg transition-colors">
          신청 결과로
        </Link>
        <button onClick={handleRegister} disabled={!canSubmit}
          className={`px-14 py-3 text-[14px] font-bold transition-all ${
            canSubmit
              ? 'bg-kb-primary text-white hover:opacity-85'
              : 'bg-gray-200 text-gray-400 cursor-not-allowed'}`}>
          {submitting ? '등록 중...' : '보증인 등록'}
        </button>
      </div>

      {/* 전자서명 확인 모달 */}
      {signingId !== null && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-lg w-[400px] p-8">
            <p className="text-[18px] font-bold text-kb-text mb-2">전자서명 진행</p>
            <p className="text-[13px] text-kb-text-body mb-6 leading-relaxed">
              보증 약정서에 전자서명합니다.<br />서명 완료 후 보증이 유효하게 됩니다.
            </p>
            {signingError && <p className="text-[12px] text-kb-red mb-3">{signingError}</p>}
            <div className="flex gap-2 justify-end">
              <button onClick={() => setSigningId(null)}
                className="px-6 py-2 border border-kb-primary-border text-[13px] text-kb-text hover:bg-kb-primary-bg">
                취소
              </button>
              <button onClick={() => handleSign(signingId)} disabled={actionLoading}
                className="px-6 py-2 bg-kb-primary text-[13px] font-bold text-kb-text hover:brightness-95 disabled:opacity-50">
                {actionLoading ? '처리 중...' : '서명 완료'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 취소 확인 모달 */}
      {cancelingId !== null && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-lg w-[400px] p-8">
            <p className="text-[18px] font-bold text-kb-text mb-2">보증인 약정 취소</p>
            <p className="text-[13px] text-kb-text-body mb-4">취소 사유를 입력해 주세요 (선택).</p>
            <textarea value={cancelRemark} onChange={e => setCancelRemark(e.target.value)}
              rows={3} placeholder="취소 사유"
              className="w-full border border-kb-primary-border px-3 py-2 text-[13px] focus:outline-none mb-4 resize-none" />
            <div className="flex gap-2 justify-end">
              <button onClick={() => setCancelingId(null)}
                className="px-6 py-2 border border-kb-primary-border text-[13px] text-kb-text hover:bg-kb-primary-bg">
                닫기
              </button>
              <button onClick={() => handleCancel(cancelingId)} disabled={actionLoading}
                className="px-6 py-2 bg-kb-red text-[13px] font-bold text-white hover:opacity-90 disabled:opacity-50">
                {actionLoading ? '처리 중...' : '취소 확정'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
