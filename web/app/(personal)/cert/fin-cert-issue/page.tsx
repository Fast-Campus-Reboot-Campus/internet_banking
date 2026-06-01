'use client'

import Link from 'next/link'
import { useState } from 'react'
import { api } from '@/lib/api'

const SIDEBAR_MENUS = [
  {
    label: '금융인증서',
    expanded: true,
    items: [
      { label: '인증서 발급/재발급', active: true, href: '/cert/fin-cert-issue' },
      { label: '타행·타기관 인증서 등록', href: '#' },
      { label: '타행·타기관 인증서 해제', href: '#' },
      { label: '인증서 갱신', href: '#' },
      { label: '인증서 폐기', href: '#' },
      { label: '인증서 관리', href: '#' },
      { label: '이용안내', href: '#' },
    ],
  },
]

const TERMS = [
  { label: '전자금융거래기본약관', required: true },
  { label: '전자금융서비스이용약관', required: true },
  { label: '전자금융서비스설명서', required: true },
  { label: '금융결제원 인증서 이용약관', required: true },
  { label: '개인정보 수집 및 이용고지', required: true },
  { label: '개인정보 제3자 제공동의', required: true },
  { label: '개인(신용)정보 수집·이용 동의서', required: true },
  { label: '고유식별정보 수집·이용 동의서', required: true },
]

export default function FinCertIssuePage() {
  const [expandedTerms, setExpandedTerms] = useState<Set<number>>(new Set())
  const [checkedTerms, setCheckedTerms] = useState<Set<number>>(new Set())
  const [allChecked, setAllChecked] = useState(false)
  const [userId, setUserId] = useState('')
  const [rrn1, setRrn1] = useState('')
  const [rrn2, setRrn2] = useState('')
  const [password, setPassword] = useState('')
  const [mouseInput, setMouseInput] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [issuedCert, setIssuedCert] = useState<{ serialNumber: string; expiryDate: string } | null>(null)

  function toggleTerm(i: number) {
    setExpandedTerms((prev) => {
      const next = new Set(prev)
      if (next.has(i)) { next.delete(i) } else { next.add(i) }
      return next
    })
  }

  function checkTerm(i: number) {
    setCheckedTerms((prev) => {
      const next = new Set(prev)
      if (next.has(i)) { next.delete(i) } else { next.add(i) }
      setAllChecked(next.size === TERMS.length)
      return next
    })
  }

  function handleAllCheck() {
    if (allChecked) {
      setCheckedTerms(new Set())
      setAllChecked(false)
    } else {
      setCheckedTerms(new Set(TERMS.map((_, i) => i)))
      setAllChecked(true)
    }
  }

  async function handleIssue() {
    if (checkedTerms.size !== TERMS.length) { setError('필수 약관에 모두 동의해 주세요.'); return }
    if (!userId) { setError('아이디를 입력해 주세요.'); return }
    if (rrn1.length !== 6 || rrn2.length !== 7) { setError('주민등록번호를 올바르게 입력해 주세요.'); return }
    if (!password) { setError('비밀번호를 입력해 주세요.'); return }
    setError(''); setLoading(true)
    try {
      const { data: res } = await api.post('/api/v1/auth/cert/issue', {
        loginId: userId,
        password,
        certType: 'CERT_FIN',
      })
      const cert = res.data
      localStorage.setItem('issuedFinCert', JSON.stringify({
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
    <div className="max-w-kb-container mx-auto px-6 py-8 flex gap-8">

      {/* 좌측 사이드바 */}
      <aside className="w-52 flex-shrink-0">
        <div className="bg-white border border-kb-border">
          <div className="bg-kb-text px-4 py-3">
            <p className="text-body font-bold text-white">인증센터(개인)</p>
          </div>
          {SIDEBAR_MENUS.map((section) => (
            <div key={section.label}>
              <div className="px-4 py-3 border-b border-kb-border bg-kb-beige-light">
                <p className="text-body font-bold text-kb-text">{section.label}</p>
              </div>
              {section.items.map((item) => (
                <Link
                  key={item.label}
                  href={item.href}
                  className={`block px-5 py-2.5 text-caption border-b border-kb-border transition-colors
                    ${item.active
                      ? 'bg-[#0D5C47] font-bold text-white'
                      : 'text-kb-text-body hover:bg-kb-beige-light'
                    }`}
                >
                  {item.label}
                </Link>
              ))}
            </div>
          ))}
        </div>
      </aside>

      {/* 우측 메인 */}
      <main className="flex-1 min-w-0 space-y-8">

        {/* 브레드크럼 */}
        <div className="flex items-center gap-2 text-caption text-kb-text-muted">
          <Link href="/cert" className="hover:text-kb-text">인증센터(개인)</Link>
          <span>&gt;</span>
          <span>금융인증서</span>
          <span>&gt;</span>
          <span className="text-kb-text font-medium">인증서 발급/재발급</span>
        </div>

        {/* 페이지 제목 */}
        <h2 className="text-[22px] font-bold text-kb-text border-b-2 border-[#0D5C47] pb-3">
          인증서 발급/재발급
        </h2>

        {/* 금융인증서 서비스 안내 */}
        <div className="border border-kb-border bg-kb-beige-light p-6 space-y-3">
          <p className="text-body font-bold text-kb-text">금융인증서 서비스 안내</p>
          <ul className="space-y-2 text-caption text-kb-text-body list-disc list-inside">
            <li>금융인증서는 금융결제원의 클라우드에 저장되어 PC·스마트폰 등 기기에 관계없이 이용 가능합니다.</li>
            <li>유효기간은 3년이며 만료 전 갱신을 통해 계속 사용 가능합니다.</li>
            <li>발급 수수료는 무료입니다.</li>
          </ul>
        </div>

        {/* 약관 및 서비스설명서 */}
        <section>
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-body font-bold text-kb-text">약관 및 서비스설명서</h3>
            <label className="flex items-center gap-2 cursor-pointer text-caption text-kb-text-body select-none">
              <CheckBox checked={allChecked} onChange={handleAllCheck} />
              전체동의
            </label>
          </div>

          <div className="border border-kb-border divide-y divide-kb-border">
            {TERMS.map((term, i) => (
              <div key={term.label}>
                <div className="flex items-center px-4 py-3 gap-3">
                  <CheckBox checked={checkedTerms.has(i)} onChange={() => checkTerm(i)} />
                  <span className="text-caption text-[#0D5C47] font-medium mr-1">[필수]</span>
                  <span className="flex-1 text-caption text-kb-text">{term.label}</span>
                  <button
                    onClick={() => toggleTerm(i)}
                    className="text-caption text-kb-text-muted hover:text-kb-text transition-colors px-2"
                  >
                    {expandedTerms.has(i) ? '▲' : '▼'}
                  </button>
                </div>
                {expandedTerms.has(i) && (
                  <div className="px-6 py-4 bg-kb-beige-light text-caption text-kb-text-muted leading-relaxed border-t border-kb-border">
                    {term.label}에 관한 약관 내용입니다. 서비스 이용에 동의해주시면 금융인증서 발급 서비스를 이용하실 수 있습니다.
                  </div>
                )}
              </div>
            ))}
          </div>
        </section>

        {/* 사용자 확인 */}
        <section className="border border-kb-border">
          <h3 className="text-body font-bold text-kb-text px-6 py-4 border-b border-kb-border">사용자 확인</h3>

          {/* 사용자 ID */}
          <div className="flex items-center px-6 py-4 gap-4 border-b border-kb-border">
            <label className="w-36 text-caption font-medium text-kb-text flex-shrink-0">사용자 ID</label>
            <div className="flex-1 flex items-center gap-4">
              <input
                type="text"
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                className="flex-1 border border-kb-border px-3 py-2 text-caption text-kb-text focus:outline-none focus:border-kb-taupe"
                placeholder="사용자 ID 입력"
              />
              <Link href="#" className="text-caption text-[#0D5C47] hover:underline whitespace-nowrap">
                ID를 모르시는 경우
              </Link>
            </div>
          </div>

          {/* 주민등록번호 */}
          <div className="flex items-center px-6 py-4 gap-4 border-b border-kb-border">
            <label className="w-36 text-caption font-medium text-kb-text flex-shrink-0">주민등록번호</label>
            <div className="flex-1 flex items-center gap-3">
              <input
                type="text"
                value={rrn1}
                onChange={(e) => setRrn1(e.target.value.replace(/\D/g, '').slice(0, 6))}
                maxLength={6}
                className="w-28 border border-kb-border px-3 py-2 text-caption text-kb-text focus:outline-none focus:border-kb-taupe text-center tracking-widest"
                placeholder="생년월일"
              />
              <span className="text-kb-text-muted">-</span>
              {mouseInput ? (
                <input
                  type="password"
                  value={rrn2}
                  onChange={(e) => setRrn2(e.target.value.replace(/\D/g, '').slice(0, 7))}
                  maxLength={7}
                  className="w-28 border border-kb-border px-3 py-2 text-caption text-kb-text focus:outline-none focus:border-kb-taupe text-center tracking-widest"
                />
              ) : (
                <div className="flex items-center gap-1">
                  {Array.from({ length: 7 }).map((_, j) => (
                    <div key={j} className="w-7 h-8 border border-kb-border bg-kb-beige-light flex items-center justify-center">
                      <span className="text-kb-text-muted text-body">●</span>
                    </div>
                  ))}
                </div>
              )}
              <label className="flex items-center gap-1.5 cursor-pointer select-none ml-2">
                <CheckBox checked={mouseInput} onChange={() => setMouseInput(!mouseInput)} />
                <span className="text-caption text-kb-text-body">마우스로 입력</span>
              </label>
            </div>
          </div>

          {/* 비밀번호 */}
          <div className="flex items-center px-6 py-4 gap-4">
            <label className="w-36 text-caption font-medium text-kb-text flex-shrink-0">비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="로그인 비밀번호"
              className="flex-1 border border-kb-border px-3 py-2 text-caption text-kb-text focus:outline-none focus:border-[#0D5C47]"
            />
          </div>
        </section>

        {error && <p className="text-[12px] text-red-500">{error}</p>}

        {/* 발급 완료 화면 */}
        {issuedCert && (
          <div className="border-2 border-[#0D5C47] bg-[#F0FAF7] p-5 space-y-3">
            <div className="flex items-center gap-2">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#0D5C47" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"/><path d="M8 12l3 3 5-5"/>
              </svg>
              <p className="text-[14px] font-bold text-[#0D5C47]">금융인증서 발급이 완료되었습니다</p>
            </div>
            <div className="bg-white border border-[#C8E8DF] p-4 space-y-1.5 text-[12px]">
              <div className="flex gap-3">
                <span className="w-20 text-kb-text-muted flex-shrink-0">일련번호</span>
                <span className="text-kb-text font-medium break-all">{issuedCert.serialNumber}</span>
              </div>
              <div className="flex gap-3">
                <span className="w-20 text-kb-text-muted flex-shrink-0">인증서 유형</span>
                <span className="text-kb-text">금융인증서</span>
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

        {/* 버튼 */}
        <div className="flex justify-center gap-3 pt-2">
          {issuedCert ? (
            <>
              <Link
                href="/login"
                className="px-14 py-3 text-body font-bold text-white hover:opacity-90 transition-all"
                style={{ backgroundColor: '#0D5C47' }}
              >
                로그인 화면으로
              </Link>
              <Link
                href="/cert/cert-management"
                className="px-14 py-3 border border-kb-border text-body text-kb-text hover:bg-kb-beige-light transition-colors"
              >
                인증서 관리
              </Link>
            </>
          ) : (
            <>
              <Link href="/cert" className="px-14 py-3 border border-kb-border text-body text-kb-text hover:bg-kb-beige-light transition-colors">
                취소
              </Link>
              <button
                onClick={handleIssue}
                disabled={loading}
                className="px-14 py-3 text-body font-bold text-white hover:opacity-90 transition-all disabled:opacity-50"
                style={{ backgroundColor: '#0D5C47' }}
              >
                {loading ? '발급 중...' : '확인'}
              </button>
            </>
          )}
        </div>

        {/* OTP 안내 */}
        <div className="border border-kb-border bg-white p-6 flex items-start gap-4">
          <div className="w-12 h-12 rounded-full bg-kb-beige-light flex items-center justify-center flex-shrink-0">
            <span className="text-2xl">🔑</span>
          </div>
          <div className="space-y-1">
            <p className="text-body font-bold text-kb-text">보안카드/OTP가 없으신가요?</p>
            <p className="text-caption text-kb-text-muted leading-relaxed">
              AXful인증서를 발급받으시면 OTP 없이도 안전하게 금융거래를 하실 수 있습니다.
            </p>
            <Link href="/cert/axful-cert-issue" className="text-caption text-[#0D5C47] hover:underline">
              AXful인증서 발급받기 &gt;
            </Link>
          </div>
        </div>

      </main>
    </div>
  )
}

function CheckBox({ checked, onChange }: { checked: boolean; onChange: () => void }) {
  return (
    <button
      type="button"
      onClick={onChange}
      className={`w-4 h-4 border flex-shrink-0 flex items-center justify-center transition-colors
        ${checked ? 'bg-[#0D5C47] border-[#0D5C47]' : 'bg-white border-kb-border'}`}
    >
      {checked && (
        <svg viewBox="0 0 12 10" fill="none" className="w-3 h-3">
          <path d="M1 5l3 3 7-7" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      )}
    </button>
  )
}
