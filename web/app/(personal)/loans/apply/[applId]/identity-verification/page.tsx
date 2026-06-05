'use client'

import { useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { loanApplicationApi } from '@/lib/loan-api'

const METHODS = [
  { code: 'PASS_APP', label: 'PASS 앱 인증', desc: '통신사 PASS 앱으로 인증' },
  { code: 'KAKAO',    label: '카카오 인증',   desc: '카카오톡으로 인증' },
  { code: 'CERT',     label: '공동인증서',    desc: '공동인증서(구 공인인증서)로 인증' },
]

export default function IdentityVerificationPage() {
  const { applId } = useParams<{ applId: string }>()
  const router = useRouter()

  const [method, setMethod] = useState('PASS_APP')
  const [mobileNo, setMobileNo] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)
  const [masked, setMasked] = useState('')

  function formatMobile(raw: string) {
    const digits = raw.replace(/\D/g, '').slice(0, 11)
    if (digits.length <= 3) return digits
    if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`
    return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
  }

  function onMobileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const digits = e.target.value.replace(/\D/g, '').slice(0, 11)
    setMobileNo(digits)
  }

  async function handleVerify() {
    if (!mobileNo || mobileNo.length < 10) {
      setError('휴대폰 번호를 올바르게 입력해 주세요.')
      return
    }
    setSubmitting(true)
    setError('')
    try {
      const { data: res } = await loanApplicationApi.verifyIdentity(Number(applId), {
        idvMethodCd: method,
        idvTargetCd: 'BORROWER',
        mobileNo,
      })
      setMasked(res.data?.mobileNoMasked ?? '')
      setDone(true)
    } catch (err: any) {
      setError(err.response?.data?.message ?? '본인확인 중 오류가 발생했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  if (done) {
    return (
      <div className="max-w-kb-container mx-auto px-6 py-4 pb-16">
        <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
          <span>개인뱅킹</span><span>&gt;</span>
          <span>대출</span><span>&gt;</span>
          <Link href="/loans/apply" className="hover:underline">대출 신청</Link>
          <span>&gt;</span>
          <span className="font-semibold text-kb-text">본인확인</span>
        </div>
        <h1 className="text-[22px] font-bold text-kb-text mb-6 pb-2 border-b-2 border-kb-primary">본인확인</h1>

        <div className="flex flex-col items-center py-12">
          <div className="w-20 h-20 rounded-full bg-kb-primary flex items-center justify-center mb-5">
            <svg viewBox="0 0 40 40" fill="none" className="w-10 h-10">
              <path d="M8 20l8 8 16-16" stroke="#333" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </div>
          <p className="text-[20px] font-bold text-kb-text mb-2">본인확인 완료</p>
          {masked && <p className="text-[14px] text-kb-text-muted mb-8">{masked}</p>}
          <button
            onClick={() => router.push(`/loans/apply/result?applId=${applId}`)}
            className="px-14 py-3 bg-kb-primary text-[14px] font-bold text-kb-text hover:brightness-95 transition-all"
          >
            신청 결과 확인
          </button>
        </div>
      </div>
    )
  }

  const canSubmit = mobileNo.length >= 10 && !submitting

  return (
    <div className="max-w-kb-container mx-auto px-6 py-4 pb-16">
      <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
        <span>개인뱅킹</span><span>&gt;</span>
        <span>대출</span><span>&gt;</span>
        <Link href="/loans/apply" className="hover:underline">대출 신청</Link>
        <span>&gt;</span>
        <span className="font-semibold text-kb-text">본인확인</span>
      </div>

      <h1 className="text-[22px] font-bold text-kb-text mb-6 pb-2 border-b-2 border-kb-primary">본인확인</h1>

      <div className="border border-[#b3cce8] bg-[#f0f6ff] p-4 mb-6 text-[13px] text-kb-text-body leading-relaxed">
        <p>· 대출 신청 본인 여부를 확인합니다. 현재는 간편 인증 stub으로 항상 통과 처리됩니다.</p>
        <p>· 입력하신 휴대폰 번호는 마스킹 처리되며 평문은 저장되지 않습니다.</p>
      </div>

      <section className="mb-6">
        <h2 className="text-lg font-bold text-kb-text mb-5 pb-2 border-b border-kb-primary-border">
          인증 방법 선택 <span className="text-kb-red text-[11px] font-normal ml-1">* 필수</span>
        </h2>
        <div className="grid grid-cols-3 gap-4">
          {METHODS.map(m => (
            <button
              key={m.code}
              onClick={() => setMethod(m.code)}
              className={`border rounded-xl p-5 text-left transition-colors ${
                method === m.code
                  ? 'border-kb-text bg-kb-primary/20'
                  : 'border-kb-primary-border hover:bg-kb-primary-bg'
              }`}
            >
              <p className="text-[14px] font-bold text-kb-text mb-1">{m.label}</p>
              <p className="text-[12px] text-kb-text-muted">{m.desc}</p>
            </button>
          ))}
        </div>
      </section>

      <section className="mb-6">
        <h2 className="text-lg font-bold text-kb-text mb-5 pb-2 border-b border-kb-primary-border">
          휴대폰 번호 입력 <span className="text-kb-red text-[11px] font-normal ml-1">* 필수</span>
        </h2>
        <div className="border border-kb-primary-border divide-y divide-kb-border overflow-hidden">
          <div className="flex items-center px-5 py-4 gap-4">
            <label className="w-36 text-[13px] font-medium text-kb-text flex-shrink-0">
              휴대폰 번호 <span className="text-kb-red">*</span>
            </label>
            <input
              type="tel"
              value={formatMobile(mobileNo)}
              onChange={onMobileChange}
              placeholder="010-0000-0000"
              className="flex-1 border border-kb-primary-border px-3 py-2 text-[13px] focus:outline-none max-w-xs"
            />
          </div>
        </div>
      </section>

      {error && <p className="text-center text-[13px] text-kb-red mb-4">{error}</p>}

      <div className="flex justify-center gap-3">
        <Link
          href="/loans/apply"
          className="px-14 py-3 border border-kb-primary-border text-[14px] text-kb-text hover:bg-kb-primary-bg transition-colors"
        >
          취소
        </Link>
        <button
          onClick={handleVerify}
          disabled={!canSubmit}
          className={`px-14 py-3 text-[14px] font-bold transition-all ${
            canSubmit
              ? 'bg-kb-primary text-white hover:opacity-85'
              : 'bg-gray-200 text-gray-400 cursor-not-allowed'
          }`}
        >
          {submitting ? '인증 중...' : '본인확인'}
        </button>
      </div>
    </div>
  )
}
