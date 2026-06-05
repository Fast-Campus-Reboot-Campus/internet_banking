'use client'
import { KB_PRIMARY } from '@/lib/theme'

import { useState, Suspense } from 'react'
import { useSearchParams } from 'next/navigation'
import { api } from '@/lib/api'

type Status = 'idle' | 'loading' | 'approved' | 'error'

function ApproveForm() {
  const searchParams  = useSearchParams()
  const tokenHash     = searchParams.get('token') ?? ''

  const [loginId, setLoginId]   = useState('')
  const [password, setPassword] = useState('')
  const [status, setStatus]     = useState<Status>('idle')
  const [errorMsg, setErrorMsg] = useState('')

  async function handleApprove() {
    if (!loginId || !password) { setErrorMsg('아이디와 비밀번호를 입력해 주세요.'); return }
    if (!tokenHash) { setErrorMsg('유효하지 않은 QR 토큰입니다.'); return }
    setErrorMsg(''); setStatus('loading')
    try {
      await api.post('/api/v1/auth/qr-cert/approve', { tokenHash, loginId, password })
      setStatus('approved')
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      setErrorMsg(e.response?.data?.message ?? '승인에 실패했습니다. 다시 시도해 주세요.')
      setStatus('error')
    }
  }

  return (
    <div className="min-h-screen bg-[#F5F7F6] flex items-center justify-center p-4">
      <div className="bg-white w-full max-w-sm border border-kb-border shadow-md overflow-hidden">

        {/* 헤더 */}
        <div className="px-6 py-5 border-b border-kb-border" style={{ backgroundColor: KB_PRIMARY }}>
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-white/20 rounded-full flex items-center justify-center">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <div>
              <p className="text-white font-bold text-[15px]">AXful인증서 발급 승인</p>
              <p className="text-white/70 text-[11px]">AXful Bank 앱 시뮬레이터</p>
            </div>
          </div>
        </div>

        {status === 'approved' ? (
          /* 승인 완료 화면 */
          <div className="px-6 py-10 flex flex-col items-center gap-4">
            <div className="w-16 h-16 rounded-full flex items-center justify-center" style={{ backgroundColor: '#E8F5F0' }}>
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#0D5C47" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"/><path d="M8 12l3 3 5-5"/>
              </svg>
            </div>
            <div className="text-center space-y-1">
              <p className="text-[16px] font-bold text-kb-text">발급 승인 완료</p>
              <p className="text-[13px] text-kb-text-muted">PC 화면에서 인증서 발급이 완료됩니다.</p>
            </div>
            <div className="w-full bg-kb-beige-light border border-kb-border px-4 py-3 text-[12px] text-kb-text-body space-y-1">
              <p>· 이 창은 닫으셔도 됩니다.</p>
              <p>· PC 인터넷뱅킹 화면에서 발급 결과를 확인하세요.</p>
            </div>
          </div>
        ) : (
          /* 승인 폼 */
          <div className="px-6 py-6 space-y-5">

            <div className="bg-[#FFF9E6] border border-[#E8D88A] px-4 py-3 text-[12px] text-[#7A6200] space-y-0.5">
              <p className="font-bold">AXful인증서 발급 요청이 도착했습니다</p>
              <p>PC 인터넷뱅킹에서 인증서 발급을 요청했습니다. 본인이 요청한 경우에만 승인하세요.</p>
            </div>

            {/* 아이디 */}
            <div className="space-y-1.5">
              <label className="text-[13px] font-medium text-kb-text">사용자 ID</label>
              <input
                type="text"
                value={loginId}
                onChange={(e) => setLoginId(e.target.value)}
                placeholder="아이디 입력"
                className="w-full border border-kb-border px-3 py-2.5 text-[13px] outline-none focus:border-kb-primary rounded-sm"
              />
            </div>

            {/* 비밀번호 */}
            <div className="space-y-1.5">
              <label className="text-[13px] font-medium text-kb-text">비밀번호</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="비밀번호 입력"
                onKeyDown={(e) => e.key === 'Enter' && handleApprove()}
                className="w-full border border-kb-border px-3 py-2.5 text-[13px] outline-none focus:border-kb-primary rounded-sm"
              />
            </div>

            {errorMsg && (
              <p className="text-[12px] text-red-500">{errorMsg}</p>
            )}

            <button
              onClick={handleApprove}
              disabled={status === 'loading'}
              className="w-full py-3 text-[14px] font-bold text-white hover:opacity-90 disabled:opacity-50 transition-opacity rounded-sm"
              style={{ backgroundColor: KB_PRIMARY }}
            >
              {status === 'loading' ? (
                <span className="flex items-center justify-center gap-2">
                  <span className="w-4 h-4 rounded-full border-2 border-t-transparent border-white animate-spin" />
                  승인 처리 중...
                </span>
              ) : '발급 승인하기'}
            </button>

            <p className="text-[11px] text-kb-text-muted text-center">
              본인이 요청하지 않은 경우 승인하지 마세요.
            </p>
          </div>
        )}
      </div>
    </div>
  )
}

export default function AxfulCertApprovePage() {
  return (
    <Suspense>
      <ApproveForm />
    </Suspense>
  )
}
