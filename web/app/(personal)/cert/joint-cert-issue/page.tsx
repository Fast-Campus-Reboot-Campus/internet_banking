'use client'

import Link from 'next/link'
import { useState } from 'react'
import { api } from '@/lib/api'


const TERMS = [
  { label: '전자금융거래기본약관', required: true },
  { label: '전자금융서비스이용약관', required: true },
  { label: '전자금융서비스설명서', required: true },
  { label: '개인(신용)정보 수집·이용 동의서(개인 공동/금융인증서 발급용)', required: true },
  { label: '고유식별정보 수집·이용 동의서(개인 공동/금융인증서 발급용)', required: true },
]

const STEPS = ['약관동의 및 사용자 본인확인', 'OTP 인증', '발급 완료']

export default function JointCertIssuePage() {
  const [step, setStep] = useState<1 | 2 | 3>(1)
  const [allChecked, setAllChecked] = useState(true)
  const [checked, setChecked] = useState<boolean[]>(TERMS.map(() => true))
  const [userId, setUserId] = useState('')
  const [rrn1, setRrn1] = useState('')
  const [rrn2, setRrn2] = useState('')
  const [password, setPassword] = useState('')
  const [otpCode, setOtpCode] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [issuedCert, setIssuedCert] = useState<{ serialNumber: string; expiryDate: string } | null>(null)

  function toggleAll() {
    const next = !allChecked
    setAllChecked(next)
    setChecked(TERMS.map(() => next))
  }

  function toggleOne(i: number) {
    const next = checked.map((v, idx) => (idx === i ? !v : v))
    setChecked(next)
    setAllChecked(next.every(Boolean))
  }

  function handleNextStep() {
    if (!checked.every(Boolean)) { setError('필수 약관에 모두 동의해 주세요.'); return }
    if (!userId) { setError('아이디를 입력해 주세요.'); return }
    if (rrn1.length !== 6 || rrn2.length !== 7) { setError('주민등록번호를 올바르게 입력해 주세요.'); return }
    if (!password) { setError('비밀번호를 입력해 주세요.'); return }
    setError('')
    setStep(2)
  }

  async function handleIssue() {
    if (otpCode.length !== 6) { setError('OTP 6자리를 입력해 주세요.'); return }
    setError(''); setLoading(true)
    try {
      const { data: res } = await api.post('/api/v1/auth/cert/issue', {
        loginId: userId,
        password,
        certType: 'CERT_COMMON',
      })
      const cert = res.data
      // 발급된 인증서를 localStorage에 저장해 로그인 모달에서 사용
      localStorage.setItem('issuedJointCert', JSON.stringify({
        serialNumber: cert.serialNumber,
        certType: cert.certType,
        user: userId,
        expiry: `${cert.expiryDate.slice(0,4)}.${cert.expiryDate.slice(4,6)}.${cert.expiryDate.slice(6,8)}`,
        issuer: cert.issuerName,
      }))
      setIssuedCert(cert)
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      setError(e.response?.data?.message ?? '인증서 발급 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-kb-container mx-auto px-6 py-8">
      <main className="space-y-6">

        {/* 브레드크럼 */}
        <div className="flex items-center gap-1 text-[12px] text-kb-text-muted">
          <Link href="/cert" className="hover:text-kb-text">인증센터(개인)</Link>
          <span>&gt;</span>
          <span>공동인증서(구 공인인증서)</span>
          <span>&gt;</span>
          <span>공동인증서 발급/재발급</span>
          <span>&gt;</span>
          <span className="text-kb-text font-medium">개인용 인증서 발급</span>
        </div>

        {/* 페이지 제목 */}
        <h2 className="text-[22px] font-bold text-kb-text border-b-2 border-[#0D5C47] pb-3">
          개인용 인증서 발급
        </h2>

        {/* 단계 표시 */}
        <div className="flex items-center gap-0">
          {STEPS.map((label, i) => {
            const stepNum = i + 1
            const isActive = stepNum === step
            const isDone = stepNum < step
            return (
              <div key={i} className="flex items-center">
                <div className={`flex items-center justify-center rounded-full text-[11px] font-bold border-2 px-3 h-7
                  ${isActive ? 'bg-[#0D5C47] border-[#0D5C47] text-white'
                  : isDone ? 'bg-white border-[#0D5C47] text-[#0D5C47]'
                  : 'border-gray-300 text-gray-400'}`}
                >
                  {label}
                </div>
                {i < STEPS.length - 1 && <div className="w-6 h-px bg-gray-300 mx-1" />}
              </div>
            )
          })}
        </div>

        {/* 약관동의 섹션 */}
        <section>
          <h3 className="text-[15px] font-bold text-kb-text mb-3">약관동의 및 사용자 본인 확인</h3>

          {/* 전체약관보기 */}
          <div
            onClick={toggleAll}
            className="flex items-center justify-between px-4 py-3 border border-kb-border bg-gray-50 cursor-pointer hover:bg-kb-beige-light mb-1"
          >
            <div className="flex items-center gap-2">
              <span className="text-[#0D5C47] font-bold">✓</span>
              <span className="text-[13px] font-bold text-kb-text">전체약관보기</span>
            </div>
            <span className="text-kb-text-muted">&gt;</span>
          </div>

          {/* 개별 약관 */}
          <div className="border border-kb-border divide-y divide-kb-border">
            {TERMS.map((term, i) => (
              <div
                key={term.label}
                className="flex items-center justify-between px-4 py-3 hover:bg-kb-beige-light cursor-pointer"
                onClick={() => toggleOne(i)}
              >
                <div className="flex items-center gap-2">
                  <span className={`font-bold text-[13px] ${checked[i] ? 'text-[#0D5C47]' : 'text-gray-300'}`}>✓</span>
                  {term.required && (
                    <span className="text-[11px] text-[#0D5C47] font-semibold">[필수]</span>
                  )}
                  <span className="text-[13px] text-kb-text-body">{term.label}</span>
                </div>
                <span className="text-kb-text-muted">&gt;</span>
              </div>
            ))}
          </div>
        </section>

        {/* 사용자 본인확인 섹션 */}
        <section className="border border-kb-border p-6 space-y-4">
          <h3 className="text-[15px] font-bold text-kb-text border-b border-kb-border pb-3">사용자 본인확인</h3>

          {/* 사용자 ID */}
          <div className="flex items-center gap-3">
            <label className="w-24 text-[13px] text-kb-text-body text-right flex-shrink-0">사용자 ID</label>
            <input
              type="text"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              className="border border-kb-border px-2 py-1.5 text-[13px] w-40 outline-none focus:border-blue-400"
            />
            <Link href="/customer/id-inquiry" className="text-[12px] text-[#0D5C47] hover:underline">
              ID를 모르시는 경우↗
            </Link>
          </div>

          {/* 주민등록번호 */}
          <div className="flex items-center gap-3">
            <label className="w-24 text-[13px] text-kb-text-body text-right flex-shrink-0">주민등록번호</label>
            <div className="flex items-center gap-1">
              <input
                type="text"
                maxLength={6}
                value={rrn1}
                onChange={(e) => setRrn1(e.target.value.replace(/\D/g, ''))}
                className="border border-kb-border px-2 py-1.5 text-[13px] w-24 outline-none focus:border-blue-400"
              />
              <span className="text-gray-500">-</span>
              <input
                type="password"
                maxLength={7}
                value={rrn2}
                onChange={(e) => setRrn2(e.target.value.replace(/\D/g, ''))}
                className="border border-kb-border px-2 py-1.5 text-[13px] w-24 outline-none focus:border-blue-400"
              />
            </div>
            <label className="flex items-center gap-1 text-[12px] text-kb-text-body cursor-pointer">
              <input type="checkbox" className="w-3.5 h-3.5" />
              마우스로 입력
            </label>
          </div>
          <p className="text-[11px] text-kb-text-muted pl-[108px]">
            ① 숫자만 입력하실 수 있으며, 붙여넣기는 지원되지 않습니다.
          </p>

          <p className="text-[12px] text-kb-text-body pt-2">
            위의 전자금융거래기본약관과 전자금융서비스이용약관의 내용에 동의하고 인터넷뱅킹 서비스를 이용하시겠습니까?
          </p>

          {/* 비밀번호 */}
          <div className="flex items-center gap-3">
            <label className="w-24 text-[13px] text-kb-text-body text-right flex-shrink-0">비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="로그인 비밀번호"
              className="border border-kb-border px-2 py-1.5 text-[13px] w-40 outline-none focus:border-[#0D5C47]"
            />
          </div>

          {error && <p className="text-[12px] text-red-500 pl-[108px]">{error}</p>}

          {/* 발급 완료 화면 */}
          {issuedCert && (
            <div className="border-2 border-[#0D5C47] bg-[#F0FAF7] p-5 space-y-3">
              <div className="flex items-center gap-2">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#0D5C47" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10"/><path d="M8 12l3 3 5-5"/>
                </svg>
                <p className="text-[14px] font-bold text-[#0D5C47]">공동인증서 발급이 완료되었습니다</p>
              </div>
              <div className="bg-white border border-[#C8E8DF] p-4 space-y-1.5 text-[12px]">
                <div className="flex gap-3">
                  <span className="w-20 text-kb-text-muted flex-shrink-0">일련번호</span>
                  <span className="text-kb-text font-medium break-all">{issuedCert.serialNumber}</span>
                </div>
                <div className="flex gap-3">
                  <span className="w-20 text-kb-text-muted flex-shrink-0">인증서 유형</span>
                  <span className="text-kb-text">공동인증서(구 공인인증서)</span>
                </div>
                <div className="flex gap-3">
                  <span className="w-20 text-kb-text-muted flex-shrink-0">유효기간</span>
                  <span className="text-kb-text">
                    {issuedCert.expiryDate.slice(0,4)}.{issuedCert.expiryDate.slice(4,6)}.{issuedCert.expiryDate.slice(6,8)} 까지 (3년)
                  </span>
                </div>
              </div>
              <p className="text-[11px] text-kb-text-muted">
                발급된 인증서는 로그인 화면의 &lsquo;공동·금융인증서&rsquo; 탭에서 사용할 수 있습니다.
              </p>
            </div>
          )}

          {/* 버튼 섹션 */}
          <div className="flex gap-2 pt-2">
            {issuedCert ? (
              <>
                <Link
                  href="/login"
                  className="px-8 py-2.5 text-[13px] font-bold text-white hover:opacity-90"
                  style={{ backgroundColor: '#0D5C47' }}
                >
                  로그인 화면으로
                </Link>
                <Link
                  href="/cert/joint-cert-management"
                  className="px-8 py-2.5 border border-kb-border text-[13px] text-kb-text-body hover:bg-kb-beige-light flex items-center"
                >
                  인증서 관리
                </Link>
              </>
            ) : step === 1 ? (
              <>
                <button
                  onClick={handleNextStep}
                  className="px-8 py-2.5 text-[13px] font-bold text-white hover:opacity-90"
                  style={{ backgroundColor: '#0D5C47' }}
                >
                  다음
                </button>
                <Link href="/cert" className="px-8 py-2.5 border border-kb-border text-[13px] text-kb-text-body hover:bg-kb-beige-light flex items-center">
                  취소
                </Link>
              </>
            ) : (
              <>
                <button
                  onClick={handleIssue}
                  disabled={loading}
                  className="px-8 py-2.5 text-[13px] font-bold text-white hover:opacity-90 disabled:opacity-50"
                  style={{ backgroundColor: '#0D5C47' }}
                >
                  {loading ? '발급 중...' : '인증서 발급'}
                </button>
                <button
                  onClick={() => { setStep(1); setError(''); setOtpCode('') }}
                  className="px-8 py-2.5 border border-kb-border text-[13px] text-kb-text-body hover:bg-kb-beige-light"
                >
                  이전
                </button>
              </>
            )}
          </div>
        </section>

        {/* OTP 인증 섹션 (step 2) */}
        {step === 2 && !issuedCert && (
          <section className="border border-kb-border p-6 space-y-4">
            <h3 className="text-[15px] font-bold text-kb-text border-b border-kb-border pb-3">OTP 인증</h3>
            <p className="text-[13px] text-kb-text-body">
              OTP 단말기에 표시된 6자리 숫자를 입력해 주세요.
            </p>
            <div className="flex items-center gap-3">
              <label className="w-24 text-[13px] text-kb-text-body text-right flex-shrink-0">OTP 번호</label>
              <input
                type="text"
                maxLength={6}
                value={otpCode}
                onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
                onKeyDown={(e) => e.key === 'Enter' && handleIssue()}
                placeholder="6자리 입력"
                className="border border-kb-border px-2 py-1.5 text-[13px] w-32 outline-none focus:border-[#0D5C47] tracking-widest text-center"
              />
            </div>
            <p className="text-[11px] text-kb-text-muted pl-[108px]">
              OTP 단말기가 없는 경우 AXful인증서로 발급받으실 수 있습니다.
            </p>
          </section>
        )}

    </main>
    </div>
  )
}
