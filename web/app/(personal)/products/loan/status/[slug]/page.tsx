'use client'

import Link from 'next/link'
import { useState } from 'react'
import { useParams, useSearchParams } from 'next/navigation'
import LoanSidebar from '@/components/inquiry/LoanSidebar'
import { api } from '@/lib/api'

const inputCls  = "border rounded-lg px-3 py-2 text-[13px] outline-none focus:ring-1 transition-all"
const inputStyle = { borderColor: '#D1D5DB' }

function DocsPage({ applId }: { applId: string }) {
  const DOC_LIST = ['재직증명서', '소득확인서류', '건강보험료 납부확인서']
  const [files, setFiles] = useState<Record<string, File | null>>(
    Object.fromEntries(DOC_LIST.map(d => [d, null]))
  )
  const [msg, setMsg] = useState('')

  function handleSubmit() {
    if (!Object.values(files).some(Boolean)) { setMsg('제출할 서류를 선택해주세요.'); return }
    setMsg('서류가 제출되었습니다. 심사 담당자가 검토 후 안내 드립니다.')
  }

  return (
    <div>
      <div className="rounded-xl px-5 py-4 mb-5 text-[12px] space-y-1.5"
        style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
        <p className="text-kb-text-muted">· 대출 실행 후 요구된 사후 서류를 제출합니다.</p>
        <p className="text-kb-text-muted">· 파일 형식: PDF, JPG, PNG (최대 10MB)</p>
        {applId && <p className="font-medium" style={{ color: '#0D5C47' }}>· 신청번호: {applId}</p>}
      </div>
      <div className="rounded-xl overflow-hidden mb-5" style={{ border: '1px solid #E2F5EF' }}>
        <table className="w-full text-[13px]">
          <thead>
            <tr style={{ backgroundColor: '#F0FAF7', borderBottom: '2px solid #E2F5EF' }}>
              {['서류명', '필수여부', '파일 선택'].map(h => (
                <th key={h} className="px-4 py-3 text-center font-semibold text-[12px]" style={{ color: '#0D5C47' }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {DOC_LIST.map((name, i, arr) => (
              <tr key={name} className="hover:bg-[#F8FFFE]"
                style={{ borderBottom: i < arr.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
                <td className="px-4 py-3">{name}</td>
                <td className="px-4 py-3 text-center">
                  <span className={`text-[11px] font-bold px-2 py-0.5 rounded ${i < 2 ? 'text-white' : 'text-kb-text-muted'}`}
                    style={i < 2 ? { backgroundColor: '#E05555' } : { border: '1px solid #E2F5EF' }}>
                    {i < 2 ? '필수' : '선택'}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <input type="file" accept=".pdf,.jpg,.png"
                    onChange={e => setFiles(prev => ({ ...prev, [name]: e.target.files?.[0] ?? null }))}
                    className="text-[12px] text-kb-text-muted" />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {msg && <p className="text-[13px] mb-4 font-medium" style={{ color: '#0D5C47' }}>{msg}</p>}
      <button onClick={handleSubmit}
        className="px-12 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity"
        style={{ backgroundColor: '#0D5C47' }}>제출</button>
    </div>
  )
}

function SignPage({ applId }: { applId: string }) {
  const [agreed, setAgreed] = useState(false)
  const [signed, setSigned] = useState(false)
  const [loading, setLoading] = useState(false)

  async function handleSign() {
    if (!agreed) return
    setLoading(true)
    await new Promise(r => setTimeout(r, 800))
    setSigned(true)
    setLoading(false)
  }

  if (signed) return (
    <div className="rounded-xl p-6 flex items-center gap-5" style={{ backgroundColor: '#F0FAF7', border: '1px solid #E2F5EF' }}>
      <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: '#0D5C47' }}>
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
      </div>
      <div>
        <p className="text-[16px] font-bold mb-1" style={{ color: '#0D5C47' }}>전자서명이 완료되었습니다.</p>
        <p className="text-[12px] text-kb-text-muted">약정이 체결되었으며 대출 실행을 진행할 수 있습니다.</p>
      </div>
    </div>
  )

  return (
    <div>
      <div className="rounded-xl px-5 py-4 mb-5 text-[12px] space-y-1.5"
        style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
        <p className="text-kb-text-muted">· 대출 약정서에 전자서명합니다. 금융인증서가 필요합니다.</p>
        {applId && <p className="font-medium" style={{ color: '#0D5C47' }}>· 신청번호: {applId}</p>}
      </div>
      <div className="rounded-xl p-5 mb-5" style={{ border: '1px solid #E2F5EF' }}>
        <p className="text-[14px] font-bold text-kb-text mb-3">전자서명 원문</p>
        <div className="space-y-1.5 text-[13px] text-kb-text-muted mb-4">
          <p>· 본인은 대출 약정의 모든 조건을 확인하고 이해하였습니다.</p>
          <p>· 이자 및 원금 상환 의무를 성실히 이행할 것을 확인합니다.</p>
          <p>· 개인정보 수집·이용 및 제3자 제공에 동의합니다.</p>
        </div>
        <label className="flex items-center gap-3 cursor-pointer">
          <input type="checkbox" checked={agreed} onChange={e => setAgreed(e.target.checked)}
            className="w-4 h-4" style={{ accentColor: '#0D5C47' }} />
          <span className="text-[13px] font-medium text-kb-text">위 내용을 모두 확인하고 동의합니다.</span>
        </label>
      </div>
      <button onClick={handleSign} disabled={!agreed || loading}
        className="px-12 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity disabled:opacity-40"
        style={{ backgroundColor: '#0D5C47' }}>
        {loading ? '서명 중...' : '금융인증서로 서명'}
      </button>
    </div>
  )
}

function ExecutePage({ applId }: { applId: string }) {
  const [account, setAccount] = useState('')
  const [step, setStep]       = useState<'form' | 'done'>('form')
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')

  async function handleExecute() {
    if (!account) { setError('입금 계좌를 입력해주세요.'); return }
    setError(''); setLoading(true)
    try {
      await api.post(`/api/loan-contracts/${applId}/executions`, { depositAccountNo: account })
      setStep('done')
    } catch {
      setError('대출 실행에 실패했습니다. 고객센터(1588-9999)로 문의해주세요.')
    } finally { setLoading(false) }
  }

  if (step === 'done') return (
    <div>
      <div className="rounded-xl p-6 flex items-center gap-5 mb-6" style={{ backgroundColor: '#F0FAF7', border: '1px solid #E2F5EF' }}>
        <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: '#0D5C47' }}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
        </div>
        <div>
          <p className="text-[16px] font-bold mb-1" style={{ color: '#0D5C47' }}>대출 실행이 완료되었습니다.</p>
          <p className="text-[12px] text-kb-text-muted">신청하신 계좌로 대출금이 지급됩니다. 입금까지 수 분이 소요될 수 있습니다.</p>
        </div>
      </div>
      <Link href="/products/loan/my"
        className="px-8 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity"
        style={{ backgroundColor: '#0D5C47' }}>내 대출 현황 확인</Link>
    </div>
  )

  return (
    <div>
      <div className="rounded-xl px-5 py-4 mb-5 text-[12px] space-y-1.5"
        style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
        <p className="text-kb-text-muted">· 승인된 대출을 실행합니다. 대출금이 지정 계좌로 즉시 지급됩니다.</p>
        <p className="font-medium" style={{ color: '#E05555' }}>· 실행 후에는 취소가 불가합니다. 계좌를 반드시 확인하세요.</p>
        {applId && <p className="font-medium" style={{ color: '#0D5C47' }}>· 신청번호: {applId}</p>}
      </div>
      <div className="rounded-xl overflow-hidden mb-5" style={{ border: '1px solid #E2F5EF' }}>
        <div className="flex items-center">
          <div className="w-[180px] px-5 py-3 text-[13px] font-semibold flex-shrink-0"
            style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', alignSelf: 'stretch', display: 'flex', alignItems: 'center' }}>
            입금 계좌번호
          </div>
          <div className="flex-1 px-5 py-3">
            <input type="text" value={account} onChange={e => setAccount(e.target.value)}
              placeholder="입금받을 계좌번호 입력" className={inputCls + ' w-full max-w-sm'} style={inputStyle} />
          </div>
        </div>
      </div>
      {error && <p className="text-[13px] mb-4" style={{ color: '#E05555' }}>{error}</p>}
      <button onClick={handleExecute} disabled={loading}
        className="px-12 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity disabled:opacity-50"
        style={{ backgroundColor: '#0D5C47' }}>
        {loading ? '실행 중...' : '대출 실행'}
      </button>
    </div>
  )
}

const PAGE_META: Record<string, { title: string; breadcrumb: string }> = {
  docs:    { title: '사후서류제출',         breadcrumb: '사후서류제출' },
  sign:    { title: '전자서명 (약정 체결)', breadcrumb: '전자서명' },
  execute: { title: '대출 실행',            breadcrumb: '대출 실행' },
}

export default function StatusSlugPage() {
  const params       = useParams()
  const searchParams = useSearchParams()
  const slug   = params.slug as string
  const applId = searchParams.get('applId') ?? ''
  const meta   = PAGE_META[slug]

  if (!meta) return (
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6 flex gap-8">
        <LoanSidebar />
        <div className="flex-1 flex items-center justify-center py-20">
          <p className="text-[15px] text-kb-text-muted">페이지를 찾을 수 없습니다.</p>
        </div>
      </div>
    </main>
  )

  return (
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6">
        <nav className="text-[12px] text-kb-text-muted mb-4 flex items-center gap-1">
          <Link href="/" className="hover:underline">개인뱅킹</Link><span>›</span>
          <Link href="/products/loan/status" className="hover:underline">대출진행현황</Link><span>›</span>
          <span className="text-kb-text font-medium">{meta.breadcrumb}</span>
        </nav>
        <div className="flex gap-8">
          <LoanSidebar />
          <div className="flex-1 min-w-0">
            <h1 className="text-[22px] font-bold text-kb-text mb-6">{meta.title}</h1>
            <div className="border-t-2 pt-6" style={{ borderColor: '#0D5C47' }}>
              {slug === 'docs'    && <DocsPage    applId={applId} />}
              {slug === 'sign'    && <SignPage    applId={applId} />}
              {slug === 'execute' && <ExecutePage applId={applId} />}
            </div>
          </div>
        </div>
      </div>
    </main>
  )
}
