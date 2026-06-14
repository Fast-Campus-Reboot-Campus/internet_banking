'use client'
import { KB_PRIMARY,KB_PRIMARY_BG } from '@/lib/theme'

import Link from 'next/link'
import { useState } from 'react'
import { api } from '@/lib/api'
import MobileAuthField from '@/components/MobileAuthField'

type Step = 'verify' | 'id-result' | 'change' | 'done'

const NOTICES_VERIFY = [
  '개인고객(인터넷뱅킹 가입한 개인사업자 포함)께서 ID 조회·사용자암호 재설정을 하실 수 있습니다.',
  '본인확인을 위해 성명·주민등록번호와 휴대폰 본인인증(SMS)이 필요합니다.',
  '가입 시 본인확인한 정보와 동일해야 조회됩니다.',
  '사용자암호는 다른 사이트의 비밀번호와 다르게 설정하시고 주기적으로 변경하시기 바랍니다.',
]

const NOTICES_CHANGE = [
  '사용자암호(PW)?  AXful Bank 아이디 로그인 시 필요한 비밀번호입니다.',
  '새로 지정할 사용자암호 : 영문/숫자/특수문자 조합 8~12자리로 설정해야 합니다.',
  '사용불가: ID와 동일한 암호설정, 같은 숫자 반복, 연속된 숫자, 연속된 문자(알파벳 순서 등), 노출되기 쉬운 주민등록번호·생일·전화번호 등',
]


function NoticeBox({ items }: { items: string[] }) {
  return (
    <div className="bg-kb-primary-bg border border-kb-border rounded-xl px-5 py-4 space-y-2">
      {items.map((n, i) => (
        <div key={i} className="flex items-start gap-2 text-[13px] text-kb-text-body">
          <span className="flex-shrink-0 mt-0.5">·</span>
          {n}
        </div>
      ))}
    </div>
  )
}

function PwInput({ value, onChange, placeholder }: {
  value: string; onChange: (v: string) => void; placeholder?: string
}) {
  const [show, setShow] = useState(false)
  return (
    <div className="relative inline-flex items-center">
      <input type={show ? 'text' : 'password'} value={value} onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        className="border border-kb-border px-3 py-1.5 pr-9 w-64 outline-none text-[13px] focus:border-kb-primary transition-colors" />
      <button type="button" onClick={() => setShow(v => !v)}
        className="absolute right-2.5 text-kb-text-muted hover:text-kb-text" tabIndex={-1}>
        {show
          ? <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
          : <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
        }
      </button>
    </div>
  )
}

function TableRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <tr className="border-b border-kb-border last:border-b-0">
      <td className="px-5 py-3.5 font-semibold text-[13px] text-kb-text w-40 whitespace-nowrap align-top"
        style={{ backgroundColor: KB_PRIMARY_BG }}>
        {label}
      </td>
      <td className="border-l border-kb-border px-5 py-3">{children}</td>
    </tr>
  )
}

// ID를 글자 단위 박스로 표시
function IdDisplay({ id }: { id: string }) {
  return (
    <div className="flex flex-col gap-1">
      <div className="flex gap-1">
        {id.split('').map((ch, i) => (
          <div key={i} className="flex flex-col items-center gap-0.5">
            <div className="w-9 h-9 border border-kb-border flex items-center justify-center text-[15px] font-bold text-kb-text bg-white">
              {ch}
            </div>
            <span className="text-[10px] text-kb-text-muted">
              {/[0-9]/.test(ch) ? '숫자' : '영문'}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}

export default function IdPasswordPage() {
  const [step, setStep] = useState<Step>('verify')

  // 본인확인 (휴대폰 본인인증)
  const [name,           setName]           = useState('')
  const [rrnFront,       setRrnFront]       = useState('')
  const [rrnBack,        setRrnBack]        = useState('')
  const [verificationId, setVerificationId] = useState<number | null>(null)

  // 조회 결과
  const [loginId,      setLoginId]      = useState('')
  const [customerName, setCustomerName] = useState('')

  // 재설정
  const [newPw,        setNewPw]        = useState('')
  const [newPwConfirm, setNewPwConfirm] = useState('')
  const [error,        setError]        = useState('')
  const [loading,      setLoading]      = useState(false)

  // 본인확인(verificationId) → ID 조회 (#45)
  async function handleFindId() {
    if (verificationId == null) { alert('휴대폰 본인인증을 완료해주세요.'); return }
    setError(''); setLoading(true)
    try {
      const { data: res } = await api.post('/api/v1/auth/find-id', { verificationId })
      setLoginId(res.data.loginId)
      setCustomerName(res.data.customerName)
      setStep('id-result')
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      alert(e.response?.data?.message ?? '본인확인에 실패했습니다. 가입 시 정보와 동일한지 확인해주세요.')
    } finally {
      setLoading(false)
    }
  }

  // 본인확인(verificationId) → 사용자암호 재설정 (#47)
  async function handleChange() {
    // 정책: 영문/숫자/특수문자 조합 8~12자리 (숫자만·짧은 것 거부) — #48
    if (newPw.length < 8 || newPw.length > 12) { setError('새 사용자암호는 8~12자리로 입력해주세요.'); return }
    const hasLetter  = /[A-Za-z]/.test(newPw)
    const hasDigit   = /[0-9]/.test(newPw)
    const hasSpecial = /[^A-Za-z0-9]/.test(newPw)
    if (!(hasLetter && hasDigit && hasSpecial)) { setError('새 사용자암호는 영문·숫자·특수문자를 모두 조합해야 합니다.'); return }
    if (newPw !== newPwConfirm) { setError('사용자암호가 일치하지 않습니다.'); return }
    if (verificationId == null) { setError('휴대폰 본인인증이 만료되었습니다. 처음부터 다시 시도해주세요.'); return }
    setError(''); setLoading(true)
    try {
      await api.post('/api/v1/auth/reset-password', { verificationId, newPassword: newPw })
      setStep('done')
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      setError(e.response?.data?.message ?? '사용자암호 재설정에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  function reset() {
    setStep('verify')
    setName(''); setRrnFront(''); setRrnBack(''); setVerificationId(null)
    setLoginId(''); setCustomerName('')
    setNewPw(''); setNewPwConfirm(''); setError('')
  }

  return (
    <div className="max-w-kb-container mx-auto px-6 py-8">

      {/* 브레드크럼 */}
      <div className="flex items-center gap-1 text-[12px] text-kb-text-muted mb-6">
        <Link href="/" className="hover:underline">홈</Link>
        <span>›</span>
        <span>고객센터</span>
        <span>›</span>
        <span>고객정보관리</span>
        <span>›</span>
        <span className="font-semibold text-kb-text">ID 조회 / 사용자암호 설정</span>
      </div>

      <h1 className="text-[24px] font-bold text-kb-text mb-8">ID 조회 / 사용자암호 설정</h1>

      {/* ── STEP 1: 본인확인 (휴대폰 본인인증) ── */}
      {step === 'verify' && (
        <div className="space-y-5">
          <NoticeBox items={NOTICES_VERIFY} />

          <div className="border border-kb-border rounded-xl overflow-hidden">
            <table className="w-full text-[13px]">
              <tbody>
                <TableRow label="성명(고객명)">
                  <input type="text" value={name} onChange={e => setName(e.target.value)}
                    placeholder="가입 시 본인확인한 성명"
                    className="border border-kb-border px-3 py-1.5 w-64 outline-none text-[13px] focus:border-kb-primary" />
                </TableRow>
                <TableRow label="주민등록번호">
                  <div className="flex items-center gap-1">
                    <input type="text" inputMode="numeric" maxLength={6} value={rrnFront}
                      onChange={e => setRrnFront(e.target.value.replace(/\D/g, '').slice(0, 6))}
                      placeholder="앞 6자리"
                      className="border border-kb-border px-3 py-1.5 w-28 outline-none text-[13px] focus:border-kb-primary" />
                    <span className="text-kb-text-muted">-</span>
                    <input type="password" inputMode="numeric" maxLength={7} value={rrnBack}
                      onChange={e => setRrnBack(e.target.value.replace(/\D/g, '').slice(0, 7))}
                      placeholder="뒤 7자리"
                      className="border border-kb-border px-3 py-1.5 w-28 outline-none text-[13px] focus:border-kb-primary" />
                  </div>
                </TableRow>
                <TableRow label="휴대폰 본인인증">
                  <MobileAuthField
                    purpose="IDENTITY_VERIFY"
                    identity={{ name, rrn: rrnFront + rrnBack }}
                    onVerified={(_phone, vid) => setVerificationId(vid ?? null)}
                  />
                </TableRow>
              </tbody>
            </table>
          </div>

          <div className="flex justify-center gap-3">
            <Link href="/login"
              className="border border-kb-border px-12 py-2.5 text-[14px] text-kb-text-body hover:bg-kb-primary-bg transition-colors">
              취소
            </Link>
            <button onClick={handleFindId} disabled={loading || verificationId == null}
              className="px-12 py-2.5 text-[14px] font-bold text-white rounded-lg hover:opacity-85 disabled:opacity-50 transition-opacity"
              style={{ backgroundColor: KB_PRIMARY }}>
              {loading ? '조회 중...' : 'ID 조회'}
            </button>
          </div>
        </div>
      )}

      {/* ── STEP 2: ID 조회 결과 ── */}
      {step === 'id-result' && (
        <div className="space-y-5">
          <NoticeBox items={['ID 조회 결과입니다. 이어서 사용자암호를 재설정할 수 있습니다.']} />

          <div className="border border-kb-border rounded-xl overflow-hidden">
            <table className="w-full text-[13px]">
              <tbody>
                <TableRow label="고객">
                  <span className="font-semibold text-kb-text">{customerName}</span>
                </TableRow>
                <TableRow label="고객구분">
                  <span className="text-kb-text-body">뱅킹이체회원</span>
                </TableRow>
                <TableRow label="ID">
                  <IdDisplay id={loginId} />
                </TableRow>
              </tbody>
            </table>
          </div>

          <div className="flex justify-center gap-3">
            <Link href="/login"
              className="border border-kb-border px-10 py-2.5 text-[14px] text-kb-text-body hover:bg-kb-primary-bg transition-colors">
              로그인 화면으로 이동
            </Link>
            <button onClick={() => { setStep('change'); setError('') }}
              className="px-10 py-2.5 text-[14px] font-bold text-white rounded-lg hover:opacity-85 transition-opacity"
              style={{ backgroundColor: KB_PRIMARY }}>
              사용자암호 재설정
            </button>
          </div>
        </div>
      )}

      {/* ── STEP 3: 사용자암호 재설정 ── */}
      {step === 'change' && (
        <div className="space-y-5">
          <NoticeBox items={NOTICES_CHANGE} />

          {/* 새 암호 입력 */}
          <div className="border border-kb-border rounded-xl overflow-hidden">
            <table className="w-full text-[13px]">
              <tbody>
                <TableRow label="새로 지정할 사용자암호">
                  <PwInput value={newPw} onChange={setNewPw} placeholder="영문/숫자/특수문자 조합(8~12자리)" />
                </TableRow>
                <TableRow label="사용자암호 확인">
                  <div className="space-y-1">
                    <PwInput value={newPwConfirm} onChange={setNewPwConfirm} placeholder="새 사용자암호 재입력" />
                    {newPwConfirm && newPw !== newPwConfirm && (
                      <p className="text-[12px] text-red-500">사용자암호가 일치하지 않습니다.</p>
                    )}
                    {newPwConfirm && newPw === newPwConfirm && newPw.length >= 8 && (
                      <p className="text-[12px] font-semibold" style={{ color: KB_PRIMARY }}>✓ 일치합니다.</p>
                    )}
                  </div>
                </TableRow>
              </tbody>
            </table>
          </div>

          {error && <p className="text-center text-[13px] text-red-500">{error}</p>}

          <div className="flex justify-center gap-3">
            <button onClick={reset}
              className="border border-kb-border px-12 py-2.5 text-[14px] text-kb-text-body hover:bg-kb-primary-bg transition-colors">
              취소
            </button>
            <button onClick={handleChange} disabled={loading}
              className="px-12 py-2.5 text-[14px] font-bold text-white rounded-lg hover:opacity-85 disabled:opacity-50 transition-opacity"
              style={{ backgroundColor: KB_PRIMARY }}>
              {loading ? '처리 중...' : '확인'}
            </button>
          </div>
        </div>
      )}

      {/* ── STEP 4: 완료 ── */}
      {step === 'done' && (
        <div className="space-y-5">
          <div className="border border-kb-border bg-kb-primary-bg px-6 py-8 rounded-xl flex items-center gap-6">
            <div className="w-16 h-16 rounded-full flex items-center justify-center flex-shrink-0"
              style={{ backgroundColor: KB_PRIMARY }}>
              <svg viewBox="0 0 24 24" fill="none" className="w-8 h-8" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="20,6 9,17 4,12"/>
              </svg>
            </div>
            <div>
              <p className="text-[16px] font-bold mb-1" style={{ color: KB_PRIMARY }}>사용자암호 변경이 완료되었습니다.</p>
              <p className="text-[13px] text-kb-text-muted">변경된 사용자암호로 로그인하세요.</p>
            </div>
          </div>
          <div className="flex justify-center gap-3">
            <Link href="/login"
              className="px-10 py-2.5 text-[14px] font-bold text-white rounded-lg hover:opacity-85 transition-opacity"
              style={{ backgroundColor: KB_PRIMARY }}>
              로그인하기
            </Link>
            <Link href="/"
              className="border border-kb-border px-10 py-2.5 text-[14px] text-kb-text-body hover:bg-kb-primary-bg transition-colors">
              메인으로
            </Link>
          </div>
        </div>
      )}
    </div>
  )
}
