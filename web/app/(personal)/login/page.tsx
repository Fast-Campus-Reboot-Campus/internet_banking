'use client'

import { useState, useEffect, useRef } from 'react'
import Link from 'next/link'
import { api } from '@/lib/api'

type LoginTab = 'kb인증서' | '공동금융인증서' | '아이디'

export default function LoginPage() {
  const [tab, setTab] = useState<LoginTab>('kb인증서')
  const [showLoginSetting, setShowLoginSetting] = useState(false)

  return (
    <>
      {/* 페이지 타이틀 바 */}
      <div className="bg-white border-b border-kb-border">
        <div className="max-w-kb-container mx-auto px-6 py-4">
          <h1 className="text-2xl font-bold text-kb-text">로그인</h1>
        </div>
      </div>

      {/* 본문 */}
      <div className="py-6" style={{ backgroundColor: '#F0FAF7' }}>
        <div className="w-full max-w-[1100px] mx-auto">

          {/* 로그인 카드 */}
          <div className="bg-white shadow-sm">

            {/* 탭 */}
            <div className="flex border-b border-kb-border">
              <TabButton active={tab === 'kb인증서'} onClick={() => setTab('kb인증서')}>
                <KBShieldIcon />
                AXful인증서
              </TabButton>
              <TabButton active={tab === '공동금융인증서'} onClick={() => setTab('공동금융인증서')}>
                공동·금융인증서
              </TabButton>
              <TabButton active={tab === '아이디'} onClick={() => setTab('아이디')}>
                아이디 로그인
              </TabButton>
            </div>

            {/* 탭 컨텐츠 */}
            <div className="min-h-[280px] flex flex-col justify-center">
              {tab === 'kb인증서' && <KBCertTab />}
              {tab === '공동금융인증서' && <CommonCertTab />}
              {tab === '아이디' && <IdLoginTab />}
            </div>

            {/* 하단 바로가기 */}
            <div className="border-t border-kb-border">
              <div className="grid grid-cols-4">
                {[
                  { icon: <KBShieldIcon size={20} />, label: 'AXful인증서 발급', href: '/cert-cps' as string | null },
                  { icon: <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="currentColor" strokeWidth="1.8" style={{ color: '#0D5C47' }}><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 018 0v4"/></svg>, label: '인증센터', href: '/cert' as string | null },
                  { icon: <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="currentColor" strokeWidth="1.8" style={{ color: '#0D5C47' }}><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>, label: '로그인 설정', href: null },
                  { icon: <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="currentColor" strokeWidth="1.8" style={{ color: '#0D5C47' }}><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>, label: '인증서 이용 안내', href: '/banking/first-visit' as string | null },
                ].map((item, i) => (
                  item.href === null ? (
                    <button
                      key={item.label}
                      onClick={() => setShowLoginSetting(true)}
                      className={`flex flex-col items-center gap-1.5 py-4 text-sm text-kb-text-body
                                  hover:bg-[#F0FAF7] transition-colors w-full
                                  ${i > 0 ? 'border-l border-kb-border' : ''}`}
                    >
                      <div className="flex items-center justify-center w-6 h-6">{item.icon}</div>
                      <span>{item.label}</span>
                    </button>
                  ) : (
                    <Link
                      key={item.label}
                      href={item.href}
                      className={`flex flex-col items-center gap-1.5 py-4 text-sm text-kb-text-body
                                  hover:bg-[#F0FAF7] transition-colors
                                  ${i > 0 ? 'border-l border-kb-border' : ''}`}
                    >
                      <div className="flex items-center justify-center w-6 h-6">{item.icon}</div>
                      <span>{item.label}</span>
                    </Link>
                  )
                ))}
              </div>
            </div>
          </div>

          {showLoginSetting && <LoginSettingModal onClose={() => setShowLoginSetting(false)} />}

          {/* 카드 하단 안내 */}
          <div className="mt-4 px-1 space-y-1">
            <p className="text-sm" style={{ color: '#0D5C47' }}>
              • 인터넷뱅킹 종료 시, 안전한 금융거래를 위하여 반드시 [로그아웃]버튼을 눌러 종료하시기 바랍니다.
            </p>
            <p className="text-sm" style={{ color: '#0D5C47' }}>
              • 로그인설정 버튼을 통해 자주 쓰는 로그인 방식을 설정해두면 더 간편하게 로그인 하실 수 있습니다.
            </p>
          </div>
        </div>
      </div>
    </>
  )
}

/* ── 탭 버튼 ── */
function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      onClick={onClick}
      className={`flex-1 py-4 text-base font-medium transition-colors duration-kb
                  flex items-center justify-center gap-1.5
        ${active
          ? 'bg-white border-b-2'
          : 'bg-[#F0FAF7] text-kb-text-muted hover:bg-[#E2F5EF]'
        }`}
      style={active ? { color: '#0D5C47', borderColor: '#0D5C47' } : {}}
    >
      {children}
    </button>
  )
}

/* ── AXful인증서 탭 (QR) ── */
type QrStatus = 'idle' | 'generating' | 'pending' | 'scanned' | 'approved' | 'expired' | 'error'

function KBCertTab() {
  const [status, setStatus] = useState<QrStatus>('idle')
  const [confirmCode, setConfirmCode] = useState('')
  const [timeLeft, setTimeLeft] = useState(0)
  const [qrSeed, setQrSeed] = useState(0)
  const [errorMsg, setErrorMsg] = useState('')
  const [approveId, setApproveId] = useState('')
  const [approvePw, setApprovePw] = useState('')
  const [approveError, setApproveError] = useState('')
  const [approveLoading, setApproveLoading] = useState(false)

  // 카운트다운
  useEffect(() => {
    if (status !== 'pending' && status !== 'scanned') return
    if (timeLeft <= 0) { setStatus('expired'); return }
    const t = setTimeout(() => setTimeLeft((s) => s - 1), 1000)
    return () => clearTimeout(t)
  }, [status, timeLeft])

  const tokenHashRef = useRef<string>('')

  // QR 생성 (실제 백엔드)
  async function handleGenerate() {
    setStatus('generating')
    setErrorMsg('')
    try {
      const { data } = await api.post('/api/v1/auth/qr/generate', {})
      tokenHashRef.current = data.data.tokenHash
      setConfirmCode(data.data.confirmCode)
      setQrSeed(Math.random())
      setTimeLeft(179)
      setStatus('pending')
    } catch {
      setErrorMsg('QR 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.')
      setStatus('error')
    }
  }

  // 폴링: 2초마다 status 조회
  useEffect(() => {
    if (status !== 'pending' && status !== 'scanned') return
    const poll = setInterval(async () => {
      try {
        const { data } = await api.get(`/api/v1/auth/qr/status?token=${tokenHashRef.current}`)
        const s: string = data.data.status
        if (s === 'SCANNED')  setStatus('scanned')
        if (s === 'EXPIRED')  { setStatus('expired'); clearInterval(poll) }
        if (s === 'APPROVED') {
          clearInterval(poll)
          setStatus('approved')
          localStorage.removeItem('sessionExpiry')
          localStorage.setItem('accessToken',  data.data.accessToken)
          localStorage.setItem('access_token', data.data.accessToken)
          localStorage.setItem('customerId',   String(data.data.customerId))
          if (data.data.refreshToken) localStorage.setItem('refreshToken', data.data.refreshToken)
          try {
            const me = await api.get('/api/v1/customers/me')
            localStorage.setItem('user', JSON.stringify({ name: me.data.data.name }))
          } catch {}
          window.location.href = '/'
        }
      } catch {}
    }, 2000)
    return () => clearInterval(poll)
  }, [status])

  async function handleApprove() {
    if (!approveId || !approvePw) { setApproveError('아이디와 비밀번호를 입력해주세요.'); return }
    setApproveError('')
    setApproveLoading(true)
    try {
      await api.post('/api/v1/auth/qr/approve', {
        tokenHash: tokenHashRef.current,
        loginId: approveId,
        password: approvePw,
      })
      setStatus('scanned')
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      setApproveError(e.response?.data?.message ?? '승인에 실패했습니다.')
    } finally {
      setApproveLoading(false)
    }
  }

  function handleReset() {
    setStatus('idle')
    setTimeLeft(0)
    setErrorMsg('')
    setApproveError('')
    setApproveId('')
    setApprovePw('')
    tokenHashRef.current = ''
  }

  const mm = String(Math.floor(timeLeft / 60)).padStart(2, '0')
  const ss = String(timeLeft % 60).padStart(2, '0')

  return (
    <div className="flex flex-col items-center gap-4 px-12 pt-8 pb-6">
      <p className="text-xl font-bold text-kb-text">QR코드로 로그인</p>

      {/* QR 이미지 영역 */}
      <div className="w-[140px] h-[140px] border border-kb-border flex items-center justify-center bg-white relative overflow-hidden">
        {status === 'idle' && <QRPlaceholder />}
        {(status === 'pending' || status === 'scanned' || status === 'approved') && (
          <QRCode seed={qrSeed} />
        )}
        {status === 'scanned' && (
          <div className="absolute inset-0 bg-white/75 flex flex-col items-center justify-center gap-1">
            <div className="w-8 h-8 rounded-full border-[3px] border-t-transparent animate-spin" style={{ borderColor: '#5BC9A8', borderTopColor: 'transparent' }} />
            <span className="text-[11px] font-bold" style={{ color: '#0D5C47' }}>앱 확인 중</span>
          </div>
        )}
        {status === 'approved' && (
          <div className="absolute inset-0 bg-white/90 flex flex-col items-center justify-center gap-1">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#5BC9A8" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10"/><path d="M8 12l3 3 5-5"/>
            </svg>
            <span className="text-[11px] font-bold" style={{ color: '#0D5C47' }}>승인 완료</span>
          </div>
        )}
        {status === 'expired' && (
          <div className="absolute inset-0 bg-white/90 flex flex-col items-center justify-center gap-1">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#9CA3AF" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><circle cx="12" cy="16" r="0.5" fill="#9CA3AF"/>
            </svg>
            <span className="text-[11px] text-kb-text-muted">만료됨</span>
          </div>
        )}
      </div>

      {/* 상태별 안내 */}
      {(status === 'idle' || status === 'error') && (
        <div className="w-full space-y-2 text-center">
          {status === 'error' && (
            <p className="text-sm text-red-500">{errorMsg}</p>
          )}
          <button
            onClick={handleGenerate}
            className="w-full py-3.5 text-base font-bold text-white rounded-lg hover:opacity-85 transition-opacity"
            style={{ backgroundColor: '#0D5C47' }}
          >
            QR코드 생성하기
          </button>
        </div>
      )}

      {status === 'generating' && (
        <p className="text-sm text-kb-text-muted">QR코드를 생성 중입니다...</p>
      )}

      {status === 'pending' && (
        <div className="w-full space-y-3">
          <div className="text-center space-y-1">
            <p className="text-base text-kb-text">확인 코드 <span className="font-bold">{confirmCode}</span></p>
            <p className="text-base font-medium" style={{ color: '#0D5C47' }}>남은 시간 {mm}분 {ss}초</p>
            <p className="text-sm text-kb-text-muted leading-relaxed">
              QR코드를 스캔하여 확인코드를 입력 후 승인해주세요.
            </p>
          </div>

          {/* 테스트용 모바일 승인 */}
          <div className="border rounded-xl p-4 space-y-2" style={{ borderColor: '#E2F5EF', backgroundColor: '#F8FFFE' }}>
            <p className="text-[12px] font-bold text-center" style={{ color: '#0D5C47' }}>
              테스트용 모바일 승인
            </p>
            <input
              type="text"
              placeholder="아이디"
              value={approveId}
              onChange={(e) => setApproveId(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-[13px] outline-none"
              style={{ borderColor: '#5BC9A8' }}
            />
            <input
              type="password"
              placeholder="비밀번호"
              value={approvePw}
              onChange={(e) => setApprovePw(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleApprove()}
              className="w-full border rounded-lg px-3 py-2 text-[13px] outline-none"
              style={{ borderColor: '#5BC9A8' }}
            />
            {approveError && <p className="text-[12px] text-red-500">{approveError}</p>}
            <button
              onClick={handleApprove}
              disabled={approveLoading}
              className="w-full py-2 text-[13px] font-bold text-white rounded-lg hover:opacity-85 disabled:opacity-60 transition-opacity"
              style={{ backgroundColor: '#0D5C47' }}
            >
              {approveLoading ? '승인 중...' : '앱에서 승인하기'}
            </button>
            <p className="text-[11px] text-center text-kb-text-muted">
              실제 서비스에서는 AXful 앱에서 승인합니다
            </p>
          </div>
        </div>
      )}

      {status === 'scanned' && (
        <div className="text-center space-y-1">
          <p className="text-base font-bold" style={{ color: '#0D5C47' }}>앱에서 인증 중입니다...</p>
          <p className="text-sm text-kb-text-muted">잠시 후 자동으로 로그인됩니다.</p>
          <p className="text-sm" style={{ color: '#0D5C47' }}>남은 시간 {mm}분 {ss}초</p>
        </div>
      )}

      {status === 'approved' && (
        <div className="text-center space-y-1">
          <p className="text-base font-bold" style={{ color: '#0D5C47' }}>인증 완료! 로그인 중...</p>
        </div>
      )}

      {status === 'expired' && (
        <div className="text-center space-y-2">
          <p className="text-sm text-kb-text-muted">QR코드가 만료되었습니다.</p>
          <button
            onClick={handleReset}
            className="w-full py-3 text-base font-bold text-white rounded-lg hover:opacity-85 transition-opacity"
            style={{ backgroundColor: '#0D5C47' }}
          >
            다시 생성하기
          </button>
        </div>
      )}
    </div>
  )
}

/* ── 공동·금융인증서 탭 ── */
function CommonCertTab() {
  const [showCertModal, setShowCertModal] = useState(false)
  const [showJointCertModal, setShowJointCertModal] = useState(false)
  const [showKBStarModal, setShowKBStarModal] = useState(false)

  return (
    <>
      <div className="flex divide-x divide-kb-border py-8">
        {/* 좌: 공동인증서 */}
        <div className="flex-1 flex flex-col items-center gap-4 px-10">
          <p className="text-sm text-kb-text-body whitespace-nowrap">공동인증서(구 공인인증서)</p>
          <button
            className="w-full py-3 text-sm whitespace-nowrap font-semibold text-white rounded-lg hover:opacity-85 transition-opacity" style={{ backgroundColor: '#0D5C47' }}
            onClick={() => setShowJointCertModal(true)}
          >공동인증서(구 공인인증서) 로그인</button>
          <button className="w-full py-3 text-sm whitespace-nowrap font-semibold rounded-lg border-2 hover:bg-[#F0FAF7] transition-colors" style={{ borderColor: '#0D5C47', color: '#0D5C47' }} onClick={() => setShowKBStarModal(true)}>AXful 앱 연동 로그인</button>
          <p className="text-sm whitespace-nowrap">
            <Link href="/cert/joint-cert-issue" className="hover:underline" style={{ color: '#0D5C47' }}>공동인증서(구 공인인증서) 발급</Link>
            <span className="mx-2 text-kb-border">|</span>
            <Link href="/cert/joint-cert-management" className="hover:underline" style={{ color: '#0D5C47' }}>인증서 관리</Link>
          </p>
        </div>

        {/* 우: 금융인증서 */}
        <div className="flex-1 flex flex-col items-center gap-4 px-10">
          <p className="text-sm text-kb-text-body whitespace-nowrap">금융인증서(브라우저인증서)</p>
          <button
            onClick={() => setShowCertModal(true)}
            className="w-full py-3 text-sm whitespace-nowrap font-semibold text-white rounded-lg hover:opacity-85 transition-opacity" style={{ backgroundColor: '#0D5C47' }}
          >
            금융인증서(브라우저인증서) 로그인
          </button>
          <p className="text-sm whitespace-nowrap">
            <Link href="/cert/fin-cert-issue" className="hover:underline" style={{ color: '#0D5C47' }}>금융인증서 발급</Link>
            <span className="mx-2 text-kb-border">|</span>
            <Link href="/cert/cert-management" className="hover:underline" style={{ color: '#0D5C47' }}>인증서 관리</Link>
          </p>
        </div>
      </div>

      {showCertModal && <FinCertModal onClose={() => setShowCertModal(false)} />}
      {showJointCertModal && <JointCertModal onClose={() => setShowJointCertModal(false)} />}
      {showKBStarModal && <KBStarModal onClose={() => setShowKBStarModal(false)} />}
    </>
  )
}

/* ── 공동인증서 모달 (전자 서명 작성) ── */
const STORAGE_TYPES = [
  { label: '하드디스크', icon: '🖥️' },
  { label: '이동식',     icon: '💾' },
  { label: '보안토큰',   icon: '🔒' },
  { label: '휴대폰',     icon: '📱' },
  { label: '안전디스크', icon: '💿' },
  { label: '간편인증',   icon: '✓'  },
]

const MOCK_JOINT_CERTS = [
  {
    id: 'joint_1',
    serialNumber: 'COMMON-TEST-2024-000001',
    certType: 'CERT_COMMON',
    type: '금융결제원',
    user: '홍길동(0100-0101-****-****)',
    expiry: '2027.06.01',
    issuer: '한국전자인증',
  },
]

async function handleCertLogin(certSerialNumber: string, certType: string, pin: string) {
  const { data } = await api.post('/api/v1/auth/cert-login', { certSerialNumber, pin, certType })
  localStorage.removeItem('sessionExpiry')
  localStorage.setItem('accessToken', data.data.accessToken)
  localStorage.setItem('access_token', data.data.accessToken)
  localStorage.setItem('customerId', String(data.data.customerId))
  if (data.data.refreshToken) localStorage.setItem('refreshToken', data.data.refreshToken)
  try {
    const me = await api.get('/api/v1/customers/me')
    localStorage.setItem('user', JSON.stringify({ name: me.data.data.name }))
  } catch {}
  window.location.href = '/'
}

function JointCertModal({ onClose }: { onClose: () => void }) {
  const [storageType, setStorageType] = useState('하드디스크')
  const [selectedCert, setSelectedCert] = useState<string | null>(null)
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleConfirm() {
    if (!selectedCert) { setError('인증서를 선택해 주세요.'); return }
    if (!password)     { setError('인증서 암호를 입력해 주세요.'); return }
    setError('')
    setLoading(true)
    try {
      const cert = MOCK_JOINT_CERTS.find(c => c.id === selectedCert)!
      await handleCertLogin(cert.serialNumber, cert.certType, password)
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      setError(e.response?.data?.message ?? '인증서 암호가 맞지 않습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50">
      <div className="bg-white shadow-2xl" style={{ width: 520 }}>

        {/* 타이틀 바 */}
        <div className="flex items-center justify-between px-4 py-2 border-b border-kb-border" style={{ backgroundColor: '#F0FAF7' }}>
          <span className="text-[13px] font-medium text-kb-text">전자 서명 작성</span>
          <button onClick={onClose} className="text-kb-text-muted hover:text-kb-text text-lg leading-none">✕</button>
        </div>

        {/* AXful인증서 배너 */}
        <div className="flex items-center gap-4 px-4 py-3 border-b" style={{ backgroundColor: '#F0FAF7', borderColor: '#E2F5EF' }}>
          <div className="flex-1">
            <p className="text-[12px] font-bold" style={{ color: '#0D5C47' }}>금융생활을 넘어 일상생활까지 AXful인증서로</p>
            <p className="text-[11px] mt-0.5" style={{ color: '#0D5C47' }}>간편하게 발급에서 안전하게 보관하고 평생 쉽게 이용하는 AXful인증서</p>
          </div>
          <div className="flex items-center gap-1 flex-shrink-0">
            <div className="w-10 h-10 rounded-lg flex items-center justify-center text-white text-[11px] font-extrabold" style={{ backgroundColor: '#0D5C47' }}>
              AX
            </div>
          </div>
        </div>

        <div className="px-5 py-4 space-y-4">
          {/* 저장 위치 선택 */}
          <div>
            <p className="text-[13px] font-bold text-kb-text mb-2">인증서 저장 위치를 선택해 주세요</p>
            <div className="flex gap-1.5">
              {STORAGE_TYPES.map((st) => (
                <button
                  key={st.label}
                  onClick={() => setStorageType(st.label)}
                  className={`relative flex flex-col items-center gap-1 px-2 py-2 border text-[11px] min-w-[60px] transition-colors
                    ${storageType === st.label
                      ? 'text-kb-text font-semibold'
                      : 'border-kb-border text-kb-text-muted hover:bg-[#F0FAF7]'
                    }`}
                >
                  {storageType === st.label && (
                    <span className="absolute top-1 left-1 text-[10px] font-bold" style={{ color: '#0D5C47' }}>✓</span>
                  )}
                  <span className="text-lg leading-none mt-1">{st.icon}</span>
                  <span>{st.label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* 인증서 목록 */}
          <div>
            <p className="text-[13px] font-bold text-kb-text mb-2">사용할 인증서를 선택해 주세요</p>
            <table className="w-full border border-kb-border text-[12px]">
              <thead>
                <tr className="bg-[#F0FAF7]">
                  {['구분', '사용자', '만료일', '발급자'].map((h) => (
                    <th key={h} className="border-r last:border-r-0 border-kb-border py-1.5 px-2 font-medium text-kb-text text-left">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {MOCK_JOINT_CERTS.map((cert) => (
                  <tr
                    key={cert.id}
                    onClick={() => setSelectedCert(cert.id)}
                    className={`cursor-pointer ${selectedCert === cert.id ? 'bg-[#F0FAF7]' : 'hover:bg-[#F0FAF7]'}`}
                  >
                    <td className="border-r border-gray-200 py-1.5 px-2">{cert.type}</td>
                    <td className="border-r border-gray-200 py-1.5 px-2">{cert.user}</td>
                    <td className="border-r border-gray-200 py-1.5 px-2">{cert.expiry}</td>
                    <td className="py-1.5 px-2">{cert.issuer}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="flex justify-end gap-1 mt-1">
              {['인증서 보기', '인증서 찾기', '인증서 삭제'].map((btn) => (
                <button key={btn} className="border border-kb-border text-[11px] px-2 py-1 text-kb-text-body hover:bg-[#F0FAF7]">
                  {btn}
                </button>
              ))}
            </div>
          </div>

          {/* 인증서 암호 */}
          <div>
            <p className="text-[13px] font-bold text-kb-text mb-2">인증서 암호를 입력해 주세요</p>
            <div className="flex gap-2">
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleConfirm()}
                className="flex-1 border border-kb-border px-2 py-1.5 text-[13px] outline-none focus:border-kb-taupe"
              />
              <button className="border border-kb-border px-3 text-kb-text-muted hover:bg-[#F0FAF7] text-[18px]">⌨</button>
            </div>
            <p className="text-[11px] text-gray-500 mt-1">안전한 금융거래를 위해 6개월마다 인증서 암호를 변경하시기 바랍니다.</p>
            {error && <p className="text-[12px] text-red-500 mt-1">{error}</p>}
          </div>
        </div>

        {/* 확인 / 취소 */}
        <div className="flex justify-center gap-3 px-5 pb-5">
          <button
            onClick={handleConfirm}
            disabled={loading}
            className="px-12 py-2 text-white text-[14px] font-bold rounded-lg hover:opacity-85 disabled:opacity-60 transition-opacity"
            style={{ backgroundColor: '#0D5C47' }}
          >
            확인
          </button>
          <button
            onClick={onClose}
            className="px-12 py-2 border border-kb-border text-[14px] text-kb-text-body hover:bg-[#F0FAF7]"
          >
            취소
          </button>
        </div>
      </div>
    </div>
  )
}

/* ── 아이디 로그인 탭 ── */
function IdLoginTab() {
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleLogin() {
    if (!loginId || !password) {
      setError('아이디와 비밀번호를 입력해주세요.')
      return
    }
    setError('')
    setLoading(true)
    try {
      const { data } = await api.post('/api/v1/auth/login', { loginId, password })
      localStorage.removeItem('sessionExpiry')
      localStorage.setItem('accessToken', data.data.accessToken)
      localStorage.setItem('access_token', data.data.accessToken)
      localStorage.setItem('customerId', String(data.data.customerId))
      if (data.data.refreshToken) {
        localStorage.setItem('refreshToken', data.data.refreshToken)
      }

      // 이름 표시를 위해 내 정보 조회
      try {
        const me = await api.get('/api/v1/customers/me')
        localStorage.setItem('user', JSON.stringify({ name: me.data.data.name }))
      } catch {}

      window.location.href = '/'
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } }
      setError(axiosErr.response?.data?.message ?? '로그인에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="py-10 px-16">
      {/* form-table 스타일 */}
      <div className="space-y-2 mb-5">
        {/* 아이디 */}
        <div className="flex items-center gap-3">
          <label className="w-20 text-body text-kb-text-body text-right flex-shrink-0">아이디</label>
          <input
            type="text"
            placeholder="아이디"
            value={loginId}
            onChange={(e) => setLoginId(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
            className="input flex-1"
            autoComplete="username"
          />
        </div>

        {/* 사용자암호 */}
        <div className="flex items-center gap-3">
          <label className="w-20 text-body text-kb-text-body text-right flex-shrink-0">사용자암호</label>
          <div className="flex-1 flex items-center gap-2">
            <input
              type="password"
              placeholder="사용자암호"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
              className="input flex-1"
              autoComplete="current-password"
            />
          </div>
        </div>
      </div>

      {/* 에러 메시지 */}
      {error && (
        <div className="pl-[92px] mb-3">
          <p className="text-caption text-kb-red">{error}</p>
        </div>
      )}

      {/* 로그인 버튼 */}
      <div className="pl-[92px]">
        <button
          onClick={handleLogin}
          disabled={loading}
          className="w-full py-3 text-body font-bold text-white rounded-lg hover:opacity-85 transition-opacity disabled:opacity-60" style={{ backgroundColor: '#0D5C47' }}
        >
          {loading ? '로그인 중...' : '로그인'}
        </button>

        <div className="flex items-center justify-center gap-3 mt-4 text-caption" style={{ color: '#0D5C47' }}>
          <Link href="/support/customer-info/id-password" className="hover:underline">ID 조회 / 사용자암호 설정</Link>
          <span className="text-kb-border">|</span>
          <Link href="/support/customer-info/online-join" className="hover:underline">회원가입</Link>
        </div>
      </div>
    </div>
  )
}

/* ── AXful 연동 로그인 모달 ── */
function KBStarModal({ onClose }: { onClose: () => void }) {
  type StarTab = 'push' | 'qr'
  const [tab, setTab] = useState<StarTab>('push')
  const [phoneMiddle, setPhoneMiddle] = useState('')
  const [phoneLast, setPhoneLast] = useState('')
  const [saveInfo, setSaveInfo] = useState(false)
  const [sent, setSent] = useState(false)

  function handleSend() {
    if (!phoneMiddle || !phoneLast) return
    setSent(true)
  }

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50">
      <div className="bg-white shadow-2xl rounded-sm" style={{ width: 360 }}>

        {/* 타이틀 */}
        <div className="px-5 pt-5 pb-3">
          <p className="text-[16px] font-bold text-kb-text">AXful 연동 로그인</p>
        </div>

        {/* 탭 */}
        <div className="flex px-5 gap-0 mb-0">
          <button
            onClick={() => { setTab('push'); setSent(false) }}
            className={`px-4 py-2 text-[13px] font-medium border transition-colors
              ${tab === 'push'
                ? 'text-white border-transparent'
                : 'bg-white text-gray-500 border-gray-300 hover:bg-[#F0FAF7]'}`}
          >
            PUSH(휴대폰 번호)
          </button>
          <button
            onClick={() => { setTab('qr'); setSent(false) }}
            className={`px-4 py-2 text-[13px] font-medium border-t border-b border-r transition-colors
              ${tab === 'qr'
                ? 'text-white border-transparent'
                : 'bg-white text-gray-500 border-gray-300 hover:bg-[#F0FAF7]'}`}
          >
            QR코드 또는 인증번호
          </button>
        </div>

        {/* 탭 컨텐츠 */}
        <div className="px-5 py-4 min-h-[260px]">
          {tab === 'push' && !sent && (
            <div className="space-y-4">
              {/* 안내 문구 */}
              <div className="flex gap-2 text-[12px] text-gray-600 leading-relaxed">
                <span className="text-[#0D5C47] flex-shrink-0 mt-0.5">ℹ</span>
                <div>
                  인증서가 저장된 휴대폰번호를 입력하시지요.<br />
                  <span className="text-[#0D5C47]">스타뱅킹앱 실행 시 지문·Face ID인증 팝업이 나오는경우</span><br />
                  해당 팝업 종료 후 연동인증을 진행해주세요.
                </div>
              </div>

              {/* 전화번호 입력 */}
              <div className="flex items-center gap-1">
                <select className="border border-gray-400 text-[13px] px-1 py-1.5 bg-white text-gray-700">
                  <option>010</option>
                  <option>011</option>
                  <option>016</option>
                  <option>017</option>
                  <option>018</option>
                  <option>019</option>
                </select>
                <span className="text-gray-400">-</span>
                <input
                  type="text"
                  maxLength={4}
                  value={phoneMiddle}
                  onChange={(e) => setPhoneMiddle(e.target.value.replace(/\D/g, ''))}
                  className="border border-gray-400 px-2 py-1.5 text-[13px] w-[80px] outline-none focus:border-[#5BC9A8]"
                />
                <span className="text-gray-400">-</span>
                <input
                  type="text"
                  maxLength={4}
                  value={phoneLast}
                  onChange={(e) => setPhoneLast(e.target.value.replace(/\D/g, ''))}
                  className="border border-gray-400 px-2 py-1.5 text-[13px] w-[80px] outline-none focus:border-[#5BC9A8]"
                />
              </div>

              {/* 내 정보 저장 */}
              <label className="flex items-center gap-2 text-[12px] text-gray-600 cursor-pointer">
                <input
                  type="checkbox"
                  checked={saveInfo}
                  onChange={(e) => setSaveInfo(e.target.checked)}
                  className="w-3.5 h-3.5"
                />
                내 정보 저장하기 (개인 컴퓨터에서만 저장하세요.)
              </label>

              {/* 전송 버튼 */}
              <button
                onClick={handleSend}
                disabled={!phoneMiddle || !phoneLast}
                className="w-full py-2.5 text-white text-[14px] font-bold rounded-lg hover:opacity-85 disabled:opacity-50 transition-opacity" style={{ backgroundColor: '#0D5C47' }}
              >
                전송
              </button>
            </div>
          )}

          {tab === 'push' && sent && (
            <div className="flex flex-col items-center justify-center gap-3 py-8 text-center">
              <div className="w-14 h-14 rounded-full border-2 border-[#5BC9A8] flex items-center justify-center">
                <span className="text-[#5BC9A8] text-2xl">📱</span>
              </div>
              <p className="text-[14px] font-bold text-kb-text">스타뱅킹 앱을 확인해주세요</p>
              <p className="text-[12px] text-gray-500">앱에서 인증을 완료하면 자동으로 로그인됩니다.</p>
            </div>
          )}

          {tab === 'qr' && (
            <div className="flex flex-col items-center justify-center gap-3 py-6">
              <div className="w-[120px] h-[120px] border border-gray-300 flex items-center justify-center bg-gray-50">
                <QRPlaceholder />
              </div>
              <p className="text-[12px] text-gray-500 text-center">
                AXful 앱에서 QR코드를 스캔하거나<br />인증번호를 입력하세요.
              </p>
            </div>
          )}
        </div>

        {/* AXful 뱅킹 아이콘 */}
        <div className="px-5 pb-2">
          <div className="w-10 h-10 rounded-lg flex items-center justify-center text-white text-[10px] font-extrabold" style={{ backgroundColor: '#0D5C47' }}>
            AX
          </div>
        </div>

        {/* 하단 */}
        <div className="flex items-center justify-between px-5 py-3 border-t border-gray-200">
          <div className="text-[11px] text-gray-400">
            <span>CertGate 2.2.0 (221025)</span>
            <br />
            <button className="text-red-500 hover:underline">처음 이용하신다면</button>
          </div>
          <button
            onClick={onClose}
            className="border border-gray-400 px-5 py-1.5 text-[13px] text-gray-600 hover:bg-gray-50"
          >
            취소
          </button>
        </div>
      </div>
    </div>
  )
}

/* ── 숫자패드 셔플 ── */
function shufflePad(): string[] {
  const digits = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']
  for (let i = digits.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[digits[i], digits[j]] = [digits[j], digits[i]]
  }
  return digits
}

/* ── 금융인증서 모달 ── */
function FinCertModal({ onClose }: { onClose: () => void }) {
  const [step, setStep] = useState<'confirm' | 'yeskey'>('confirm')
  const [pin, setPin] = useState('')
  const [pad, setPad] = useState<string[]>(shufflePad)
  const [attemptsLeft, setAttemptsLeft] = useState(10)
  const [errorMsg, setErrorMsg] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const t = setTimeout(() => setStep('yeskey'), 900)
    return () => clearTimeout(t)
  }, [])

  async function submitPin(finalPin: string) {
    setLoading(true)
    try {
      await handleCertLogin('FINCERT-TEST-2024-000001', 'CERT_FIN', finalPin)
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      const next = attemptsLeft - 1
      setAttemptsLeft(next)
      setErrorMsg(e.response?.data?.message ?? `비밀번호가\n맞지 않습니다\n(${next}회 남음)`)
      setPin('')
      setPad(shufflePad())
    } finally {
      setLoading(false)
    }
  }

  function handleDigit(d: string) {
    if (pin.length >= 6 || loading) return
    const next = pin + d
    setPin(next)
    if (next.length === 6) submitPin(next)
  }

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50">

      {/* ── STEP 1: 팝업창 확인 ── */}
      <div className="flex shadow-2xl" style={{ width: 680 }}>
        {/* 좌: 메인 패널 */}
        <div className="bg-white flex-1 flex" style={{ minHeight: 360 }}>
          {/* 탭 사이드바 */}
          <div className="w-[110px] border-r border-gray-200 bg-gray-50 py-1 flex-shrink-0">
            {[
              { label: '금융인증서', icon: '☁️', active: true },
              { label: '브라우저인증서', icon: '🌐', active: false },
              { label: '금융인증서\n(사업자)', icon: '☁️', active: false },
            ].map((item) => (
              <div
                key={item.label}
                className={`px-2 py-3 flex flex-col items-center gap-1 text-[11px] text-center cursor-pointer transition-colors
                  ${item.active
                    ? 'bg-white border-l-2 font-semibold text-kb-text'
                    : 'text-kb-text-muted hover:bg-white opacity-60'
                  }`}
              >
                <span className="text-xl">{item.icon}</span>
                <span className="whitespace-pre-line leading-tight">{item.label}</span>
              </div>
            ))}
          </div>

          {/* 메인 컨텐츠 */}
          <div className="flex-1 flex flex-col">
            <div className="flex items-center justify-between px-3 py-1.5 bg-gray-100 border-b border-gray-200">
              <button className="text-gray-500 hover:text-gray-700 text-sm px-1">−</button>
              <span className="text-[13px] font-medium text-gray-700">금융인증서</span>
              <button onClick={onClose} className="text-gray-500 hover:text-gray-700 text-sm px-1">✕</button>
            </div>
            <div className="flex-1 flex flex-col items-center justify-center gap-4 px-8 py-8">
              <div className="w-12 h-12 rounded border-2 border-[#5BC9A8] flex items-center justify-center">
                <span className="text-[#5BC9A8] text-2xl font-bold">✓</span>
              </div>
              <p className="text-[16px] font-medium text-kb-text">팝업창을 확인해주세요.</p>
              <p className="text-[13px] text-center leading-relaxed text-gray-500">
                <span className="text-[#0D5C47] font-medium">클라우드에 저장하는</span>{' '}
                <span className="text-kb-text font-medium">새로운 인증서</span>
                <br />
                금융인증서의 팝업창이 열리지 않았다면{' '}
                <span className="text-kb-text font-medium">아래 버튼을</span>
                <br />
                눌러 인증을 진행해주세요.
              </p>
              <button
                onClick={() => setStep('yeskey')}
                className="mt-2 px-10 py-2 text-white text-[14px] font-bold rounded-lg hover:opacity-85 transition-opacity"
                style={{ backgroundColor: '#0D5C47' }}
              >
                확인
              </button>
            </div>
          </div>
        </div>

        {/* 우: 이용안내 패널 */}
        <div className="w-[200px] bg-white border-l border-gray-200 flex flex-col" style={{ minHeight: 360 }}>
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
            <span className="text-[12px] font-bold text-gray-700">금융인증서 이용안내</span>
            <span className="text-xl">☁️</span>
          </div>
          <div className="px-4 py-4 space-y-4 text-[11px] text-gray-600 leading-relaxed flex-1 overflow-y-auto">
            <div>
              <p className="font-bold text-gray-700 mb-1">인증서가 없는 경우</p>
              <ul className="space-y-1 pl-0">
                <li>• [인증센터] &gt; 금융인증서 &gt; 금융인증서 발급/재발급 에서 인증서를 발급받을 수 있어요.</li>
                <li className="mt-1">• 스마트폰에서 금융인증서를 발급하신도 금융결제원 클라우드에 연결하여 인증서를 불러올 수 있어요.</li>
              </ul>
            </div>
            <div>
              <p className="font-bold text-gray-700 mb-1">타행/타기관에서 발급한 인증서인 경우</p>
              <ul className="pl-0">
                <li>• 타행/타기관인증서 등록 후 로그인 하실 수 있어요.</li>
              </ul>
            </div>
            <div>
              <p className="font-bold text-gray-700 mb-1">금융결제원 클라우드로 연결하기</p>
              <ul className="pl-0">
                <li>• 성명/생년월일/휴대폰번호를 입력 후 금융결제원 클라우드 나온 숫자 2자리를 휴대폰 문자로 전송하여 연결됩니다.</li>
              </ul>
            </div>
          </div>
          <div className="px-4 py-3 border-t border-gray-200">
            <button className="text-[11px] text-[#0D5C47] hover:underline">다시 보지않기</button>
          </div>
        </div>
      </div>

      {/* ── STEP 2: YESKEY PIN 오버레이 ── */}
      {step === 'yeskey' && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/30 z-10">
          <div className="bg-white shadow-2xl" style={{ width: 500 }}>
            <div className="flex items-center justify-between px-4 py-2.5 border-b border-kb-border" style={{ backgroundColor: '#F0FAF7' }}>
              <span className="text-[13px] font-medium text-kb-text">금융인증서비스</span>
              <button onClick={onClose} className="text-kb-text-muted hover:text-kb-text">✕</button>
            </div>

            <div className="flex" style={{ minHeight: 400 }}>
              {/* 좌: YESKEY 브랜드 */}
              <div className="w-[140px] border-r border-gray-200 flex flex-col items-center justify-center gap-3 bg-gray-50 py-8 flex-shrink-0">
                <p className="font-extrabold text-[15px] tracking-tight" style={{ color: '#1a5fb4' }}>YESKEY</p>
                <div className="w-10 h-4 bg-gray-300 rounded-sm flex items-center justify-center">
                  <span className="text-[8px] text-gray-600 font-bold">금융인증서</span>
                </div>
                <div className="w-14 h-14 rounded-full border-2 border-orange-400 flex items-center justify-center mt-1 bg-white">
                  <div className="text-center">
                    <p className="text-[9px] font-bold text-orange-400 leading-tight">Trust</p>
                    <p className="text-[9px] font-bold text-orange-400 leading-tight">Sign</p>
                  </div>
                </div>
              </div>

              {/* 우: PIN 입력 */}
              <div className="flex-1 flex flex-col items-center py-6 px-5 gap-3">
                <p className="text-[13px] font-medium" style={{ color: '#0D5C47' }}>
                  고객님의 금융인증서
                </p>
                <p className="text-[17px] font-bold text-kb-text">비밀번호를 입력해주세요</p>

                <div className="flex gap-2 my-1">
                  {Array.from({ length: 6 }).map((_, i) => (
                    <div
                      key={i}
                      className="w-9 h-9 rounded flex items-center justify-center border-2"
                      style={i < pin.length ? { backgroundColor: '#0D5C47', borderColor: '#0D5C47' } : { borderColor: '#D1D5DB', backgroundColor: 'white' }}
                    >
                      {i < pin.length && <span className="text-white text-sm font-bold">●</span>}
                    </div>
                  ))}
                </div>

                <button className="text-[12px] hover:underline" style={{ color: '#0D5C47' }}>
                  비밀번호를 잊으셨나요?
                </button>

                <div className="grid grid-cols-3 gap-0.5 w-full mt-1">
                  {pad.slice(0, 9).map((d) => (
                    <button
                      key={d}
                      onClick={() => handleDigit(d)}
                      disabled={loading}
                      className="h-12 text-[18px] font-medium text-kb-text hover:bg-gray-100 transition-colors disabled:opacity-40 select-none"
                      tabIndex={-1}
                    >
                      {d}
                    </button>
                  ))}
                  <button
                    onClick={() => { setPad(shufflePad()); setPin('') }}
                    className="h-12 flex items-center justify-center hover:bg-gray-100 transition-colors text-gray-500 text-xl"
                    tabIndex={-1}
                  >↻</button>
                  <button
                    onClick={() => handleDigit(pad[9])}
                    disabled={loading}
                    className="h-12 text-[18px] font-medium text-kb-text hover:bg-gray-100 transition-colors disabled:opacity-40 select-none"
                    tabIndex={-1}
                  >{pad[9]}</button>
                  <button
                    onClick={() => setPin((p) => p.slice(0, -1))}
                    className="h-12 flex items-center justify-center hover:bg-gray-100 transition-colors"
                    tabIndex={-1}
                  >
                    <span className="bg-gray-500 text-white text-xs rounded px-1.5 py-0.5">✕</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 오류 모달 */}
      {errorMsg && (
        <div className="fixed inset-0 z-[200] flex items-center justify-center">
          <div className="bg-white shadow-xl rounded p-8 min-w-[220px] text-center">
            <p className="text-body text-kb-text whitespace-pre-line leading-relaxed">{errorMsg}</p>
            <button
              onClick={() => setErrorMsg('')}
              className="mt-5 text-body hover:underline"
              style={{ color: '#0D5C47' }}
            >
              확인
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

/* ── 아이콘 컴포넌트 ── */
function KBShieldIcon({ size = 16 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="#0D5C47" strokeWidth="1.8">
      <path d="M12 2l7 3v6c0 4.5-3 8-7 9-4-1-7-4.5-7-9V5l7-3z"/>
      <path d="M9 12l2 2 4-4"/>
    </svg>
  )
}

function seededRand(seed: number) {
  let s = Math.floor(seed * 1e9)
  return () => {
    s = (Math.imul(s, 1664525) + 1013904223) | 0
    return (s >>> 0) / 0xffffffff
  }
}

function QRCode({ seed }: { seed: number }) {
  const SIZE = 25
  const CELL = 5
  const rand = seededRand(seed)
  const grid: boolean[][] = Array.from({ length: SIZE }, () => Array(SIZE).fill(false))

  function setFinder(row: number, col: number) {
    for (let r = 0; r < 7; r++)
      for (let c = 0; c < 7; c++)
        grid[row + r][col + c] =
          r === 0 || r === 6 || c === 0 || c === 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4)
  }
  setFinder(0, 0)
  setFinder(0, SIZE - 7)
  setFinder(SIZE - 7, 0)

  for (let i = 8; i < SIZE - 8; i++) {
    grid[6][i] = i % 2 === 0
    grid[i][6] = i % 2 === 0
  }

  const reserved = new Set<string>()
  for (let r = 0; r < 9; r++) for (let c = 0; c < 9; c++) reserved.add(`${r},${c}`)
  for (let r = 0; r < 9; r++) for (let c = SIZE - 8; c < SIZE; c++) reserved.add(`${r},${c}`)
  for (let r = SIZE - 8; r < SIZE; r++) for (let c = 0; c < 9; c++) reserved.add(`${r},${c}`)
  for (let i = 6; i < SIZE - 6; i++) { reserved.add(`6,${i}`); reserved.add(`${i},6`) }

  for (let r = 0; r < SIZE; r++)
    for (let c = 0; c < SIZE; c++)
      if (!reserved.has(`${r},${c}`)) grid[r][c] = rand() > 0.5

  const total = SIZE * CELL
  return (
    <svg width={total} height={total} viewBox={`0 0 ${total} ${total}`}>
      <rect width={total} height={total} fill="white" />
      {grid.flatMap((row, r) =>
        row.map((filled, c) =>
          filled ? <rect key={`${r}-${c}`} x={c * CELL} y={r * CELL} width={CELL} height={CELL} fill="#1a1a1a" /> : null
        )
      )}
    </svg>
  )
}

function QRPlaceholder() {
  return (
    <svg width="100" height="100" viewBox="0 0 100 100" fill="none" opacity="0.3">
      <rect x="5" y="5" width="35" height="35" rx="2" stroke="#333" strokeWidth="3" fill="none"/>
      <rect x="13" y="13" width="19" height="19" fill="#333"/>
      <rect x="60" y="5" width="35" height="35" rx="2" stroke="#333" strokeWidth="3" fill="none"/>
      <rect x="68" y="13" width="19" height="19" fill="#333"/>
      <rect x="5" y="60" width="35" height="35" rx="2" stroke="#333" strokeWidth="3" fill="none"/>
      <rect x="13" y="68" width="19" height="19" fill="#333"/>
      <rect x="60" y="60" width="10" height="10" fill="#333"/>
      <rect x="75" y="60" width="10" height="10" fill="#333"/>
      <rect x="60" y="75" width="10" height="10" fill="#333"/>
      <rect x="75" y="75" width="10" height="10" fill="#333"/>
      <rect x="85" y="85" width="10" height="10" fill="#333"/>
    </svg>
  )
}

/* ── 로그인 설정 모달 ── */
const LOGIN_SETTING_OPTIONS = [
  '공동인증서(구 공인인증서)',
  '금융인증서',
  'AXful인증서',
  '아이디',
  'QR코드',
  '사용안함',
]

function LoginSettingModal({ onClose }: { onClose: () => void }) {
  const [selected, setSelected] = useState('사용안함')

  return (
    <div className="fixed inset-0 z-[300] flex items-center justify-center bg-black/40">
      <div className="bg-white w-[400px] shadow-lg">
        {/* 헤더 */}
        <div className="flex items-center justify-between px-6 py-4" style={{ backgroundColor: '#3D4F47' }}>
          <p className="text-body font-bold text-white">로그인 설정</p>
          <button onClick={onClose} className="text-white/80 hover:text-white text-xl leading-none">✕</button>
        </div>

        {/* 안내 */}
        <div className="mx-6 mt-5 mb-4 border border-kb-border px-4 py-3 bg-[#F0FAF7]">
          <p className="text-caption text-kb-text-body">
            · 자주 쓰는 로그인 방식을 설정하고 간편하게 로그인하세요.
          </p>
        </div>

        {/* 라디오 옵션 */}
        <div className="mx-6 border border-kb-border divide-y divide-kb-border mb-6">
          {LOGIN_SETTING_OPTIONS.map((option) => (
            <label
              key={option}
              className="flex items-center gap-3 px-5 py-3 cursor-pointer hover:bg-[#F0FAF7] transition-colors"
            >
              <input
                type="radio"
                name="login-setting"
                value={option}
                checked={selected === option}
                onChange={() => setSelected(option)}
                className="accent-kb-taupe w-4 h-4 flex-shrink-0"
              />
              <span className="text-caption text-kb-text">{option}</span>
            </label>
          ))}
        </div>

        {/* 설정 버튼 */}
        <div className="flex justify-center pb-6">
          <button
            onClick={onClose}
            className="px-14 py-2.5 text-white text-body font-bold rounded-lg hover:opacity-85 transition-opacity"
            style={{ backgroundColor: '#0D5C47' }}
          >
            설정
          </button>
        </div>
      </div>
    </div>
  )
}
