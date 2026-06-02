'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { api } from '@/lib/api'

interface MyPageData {
  customerId: number
  loginId: string
  name: string
  email: string | null
  phone: string | null
  zipCode: string | null
  address: string | null
  addressDetail: string | null
  birthDate: string
  genderCode: string
  customerGradeCode: string
  customerStatusCode: string
  joinedAt: string
  lastLoginAt: string | null
}

interface NotificationSettings {
  smsReceiveYn: boolean
  emailReceiveYn: boolean
  postalReceiveYn: boolean
}

function maskId(loginId: string) {
  if (loginId.length <= 4) return loginId + '****'
  return loginId.slice(0, 4) + '****'
}

function maskPhone(phone: string | null) {
  if (!phone) return '-'
  const digits = phone.replace(/\D/g, '')
  if (digits.length === 11) {
    return `${digits.slice(0, 3)} ) ${digits.slice(3, 5)}** - ${digits.slice(7, 9)}**`
  }
  return phone
}

function maskEmail(email: string | null) {
  if (!email) return '-'
  const [local, domain] = email.split('@')
  if (!domain) return email
  return local.slice(0, 4) + '****@' + domain
}

function gradeLabel(code: string) {
  const map: Record<string, string> = {
    VIP: 'VIP', GOLD: '골드', SILVER: '실버', FAMILY: '패밀리', NORMAL: '일반',
  }
  return map[code] ?? code
}

function Toggle({ checked, onChange }: { checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <button
      onClick={() => onChange(!checked)}
      className="relative w-10 h-5 rounded-full transition-colors flex-shrink-0"
      style={{ backgroundColor: checked ? '#0D5C47' : '#D1D5DB' }}
    >
      <span className={`absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${checked ? 'translate-x-5' : 'translate-x-0.5'}`} />
    </button>
  )
}

export default function MyKBPage() {
  const router = useRouter()
  const [data, setData] = useState<MyPageData | null>(null)
  const [error, setError] = useState('')

  // 알림설정
  const [notification, setNotification] = useState<NotificationSettings>({
    smsReceiveYn: false, emailReceiveYn: false, postalReceiveYn: false,
  })
  const [notifSaving, setNotifSaving] = useState(false)
  const [notifMsg, setNotifMsg] = useState('')

  // 비밀번호 변경
  const [pwForm, setPwForm] = useState({ current: '', next: '', confirm: '' })
  const [pwSaving, setPwSaving] = useState(false)
  const [pwMsg, setPwMsg] = useState('')
  const [pwError, setPwError] = useState('')

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) { router.replace('/login'); return }

    api.get('/api/v1/customers/me')
      .then(res => setData(res.data.data))
      .catch(() => {
        setError('정보를 불러오지 못했습니다. 다시 로그인해주세요.')
        localStorage.removeItem('accessToken')
        localStorage.removeItem('customerId')
      })

    api.get('/api/v1/customers/me/settings')
      .then(res => {
        const d = res.data.data
        setNotification({
          smsReceiveYn: d.smsReceiveYn,
          emailReceiveYn: d.emailReceiveYn,
          postalReceiveYn: d.postalReceiveYn,
        })
      })
      .catch(() => {})
  }, [router])

  async function handleNotifSave() {
    setNotifSaving(true)
    setNotifMsg('')
    try {
      await api.put('/api/v1/customers/me/notification', {
        smsReceiveYn: notification.smsReceiveYn,
        emailReceiveYn: notification.emailReceiveYn,
        postalReceiveYn: notification.postalReceiveYn,
      })
      setNotifMsg('알림설정이 저장되었습니다.')
    } catch {
      setNotifMsg('저장에 실패했습니다. 다시 시도해주세요.')
    } finally {
      setNotifSaving(false)
      setTimeout(() => setNotifMsg(''), 3000)
    }
  }

  async function handlePasswordChange() {
    setPwError('')
    setPwMsg('')
    if (!pwForm.current || !pwForm.next || !pwForm.confirm) {
      setPwError('모든 항목을 입력해주세요.')
      return
    }
    if (pwForm.next !== pwForm.confirm) {
      setPwError('새 비밀번호가 일치하지 않습니다.')
      return
    }
    if (pwForm.next.length < 8) {
      setPwError('새 비밀번호는 8자 이상이어야 합니다.')
      return
    }
    setPwSaving(true)
    try {
      await api.put('/api/v1/customers/me/password', {
        currentPassword: pwForm.current,
        newPassword: pwForm.next,
      })
      setPwMsg('비밀번호가 변경되었습니다.')
      setPwForm({ current: '', next: '', confirm: '' })
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      setPwError(msg ?? '비밀번호 변경에 실패했습니다. 현재 비밀번호를 확인해주세요.')
    } finally {
      setPwSaving(false)
    }
  }

  if (error) {
    return (
      <div className="max-w-kb-container mx-auto px-6 py-20 text-center">
        <p className="text-[#E05555] mb-4">{error}</p>
        <Link href="/login" style={{ color: '#0D5C47' }} className="hover:underline">로그인 페이지로 이동</Link>
      </div>
    )
  }

  if (!data) {
    return (
      <div className="max-w-kb-container mx-auto px-6 py-20 text-center text-kb-text-muted text-[14px]">
        불러오는 중...
      </div>
    )
  }

  const lastLoginDate = data.lastLoginAt ? new Date(data.lastLoginAt) : new Date(data.joinedAt)
  const lastLoginStr = `${lastLoginDate.getFullYear()}.${String(lastLoginDate.getMonth() + 1).padStart(2, '0')}.${String(lastLoginDate.getDate()).padStart(2, '0')} ${String(lastLoginDate.getHours()).padStart(2, '0')}:${String(lastLoginDate.getMinutes()).padStart(2, '0')}`

  const labelCell = "border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text"
  const valueCell = "border border-kb-border px-4 py-3 text-kb-text-body"
  const inputCls  = "border rounded-lg px-3 py-2 text-[13px] outline-none focus:ring-1 w-full"

  return (
    <div className="max-w-kb-container mx-auto px-6 py-10 pb-16">
      {/* 브레드크럼 */}
      <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
        <span>개인뱅킹</span><span>&gt;</span>
        <span className="font-semibold text-kb-text">My AXful</span>
      </div>

      <h1 className="text-2xl font-bold text-kb-text mb-1">My AXful</h1>
      <p className="text-[12px] text-kb-text-muted mb-6">최종 접속시간 : {lastLoginStr}</p>

      {/* 인터넷뱅킹정보 */}
      <section className="mb-6">
        <div className="flex justify-between items-center mb-2">
          <h2 className="text-lg font-bold text-kb-text">인터넷뱅킹정보</h2>
        </div>
        <table className="w-full border-collapse text-[13px]">
          <tbody>
            <tr>
              <td className={`${labelCell} w-[160px]`}>사용자 ID</td>
              <td className={`${valueCell} w-[300px]`}>{maskId(data.loginId)}</td>
              <td className={`${labelCell} w-[120px]`}>보안매체</td>
              <td className={valueCell}>금융인증서</td>
            </tr>
            <tr>
              <td className={labelCell}>이체한도</td>
              <td className={valueCell}>
                <div className="flex items-center gap-3">
                  <div className="text-[12px]">
                    <p>(1일) 1,000,000원</p>
                    <p>(1회) 1,000,000원</p>
                  </div>
                  <Link href="/banking/transfer-limit"
                    className="border border-kb-border px-3 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                    이체한도 변경
                  </Link>
                </div>
              </td>
              <td className={labelCell}>고객등급</td>
              <td className={valueCell}>{gradeLabel(data.customerGradeCode)}</td>
            </tr>
          </tbody>
        </table>
      </section>

      {/* 기본정보 */}
      <section className="mb-6">
        <div className="flex justify-between items-center mb-2">
          <h2 className="text-lg font-bold text-kb-text">기본정보</h2>
          <Link href="/settings"
            className="border border-kb-border px-4 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
            정보수정
          </Link>
        </div>
        <table className="w-full border-collapse text-[13px]">
          <tbody>
            <tr>
              <td className={`${labelCell} w-[160px]`}>성명</td>
              <td className={`${valueCell} w-[300px]`}>{data.name}</td>
              <td className={`${labelCell} w-[120px]`}>생년월일</td>
              <td className={valueCell}>
                {data.birthDate.slice(0, 4)}.{data.birthDate.slice(4, 6)}.{data.birthDate.slice(6, 8)}
              </td>
            </tr>
            <tr>
              <td className={labelCell}>휴대폰번호</td>
              <td className={valueCell}>{maskPhone(data.phone)}</td>
              <td className={labelCell}>이메일</td>
              <td className={valueCell}>{maskEmail(data.email)}</td>
            </tr>
            <tr>
              <td className={labelCell}>자택주소</td>
              <td className={valueCell} colSpan={3}>
                {data.address ? `(${data.zipCode ?? ''}) ${data.address} ${data.addressDetail ?? ''}`.trim() : '-'}
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      {/* 알림설정 */}
      <section className="mb-6">
        <div className="flex justify-between items-center mb-2">
          <h2 className="text-lg font-bold text-kb-text">알림설정</h2>
        </div>
        <div className="rounded-xl overflow-hidden" style={{ border: '1px solid #E2F5EF' }}>
          <div className="px-5 py-3 text-[13px] font-semibold text-kb-text"
            style={{ backgroundColor: '#F0FAF7', borderBottom: '1px solid #E2F5EF' }}>
            수신 동의
          </div>
          {[
            { key: 'smsReceiveYn' as const,     label: 'SMS 수신',   desc: '이체·입금·공지 등 주요 알림을 문자로 받습니다.' },
            { key: 'emailReceiveYn' as const,   label: '이메일 수신', desc: '이벤트·혜택·공지 등을 이메일로 받습니다.' },
            { key: 'postalReceiveYn' as const,  label: '우편 수신',   desc: '금융 관련 서류 및 안내문을 우편으로 받습니다.' },
          ].map(({ key, label, desc }, i, arr) => (
            <div key={key}
              className="flex items-center justify-between px-5 py-4"
              style={{ borderBottom: i < arr.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
              <div>
                <p className="text-[13px] font-semibold text-kb-text">{label}</p>
                <p className="text-[12px] text-kb-text-muted mt-0.5">{desc}</p>
              </div>
              <Toggle
                checked={notification[key]}
                onChange={v => setNotification(prev => ({ ...prev, [key]: v }))}
              />
            </div>
          ))}
          <div className="px-5 py-3 flex items-center justify-between"
            style={{ borderTop: '1px solid #E2F5EF', backgroundColor: '#F8FFFE' }}>
            {notifMsg && (
              <p className="text-[12px]"
                style={{ color: notifMsg.includes('실패') ? '#E05555' : '#0D5C47' }}>
                {notifMsg}
              </p>
            )}
            {!notifMsg && <span />}
            <button
              onClick={handleNotifSave}
              disabled={notifSaving}
              className="px-8 py-2 text-[13px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity disabled:opacity-50"
              style={{ backgroundColor: '#0D5C47' }}>
              {notifSaving ? '저장 중...' : '저장'}
            </button>
          </div>
        </div>
      </section>

      {/* 비밀번호 변경 */}
      <section className="mb-8">
        <div className="flex justify-between items-center mb-2">
          <h2 className="text-lg font-bold text-kb-text">비밀번호 변경</h2>
        </div>
        <div className="rounded-xl overflow-hidden" style={{ border: '1px solid #E2F5EF' }}>
          <div className="px-5 py-3 text-[13px] font-semibold text-kb-text"
            style={{ backgroundColor: '#F0FAF7', borderBottom: '1px solid #E2F5EF' }}>
            인터넷뱅킹 비밀번호
          </div>
          <div className="px-5 py-5 space-y-4">
            <div className="rounded-xl px-4 py-3 text-[12px] text-kb-text-muted space-y-1"
              style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
              <p>· 비밀번호는 8자 이상, 영문·숫자·특수문자를 조합하여 설정해주세요.</p>
              <p>· 직전 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.</p>
            </div>
            <div className="max-w-sm space-y-3">
              {[
                { key: 'current' as const, label: '현재 비밀번호', placeholder: '현재 비밀번호 입력' },
                { key: 'next' as const,    label: '새 비밀번호',   placeholder: '새 비밀번호 입력 (8자 이상)' },
                { key: 'confirm' as const, label: '새 비밀번호 확인', placeholder: '새 비밀번호 재입력' },
              ].map(({ key, label, placeholder }) => (
                <div key={key} className="flex items-center gap-4">
                  <label className="w-32 text-[13px] font-medium text-kb-text flex-shrink-0">{label}</label>
                  <input
                    type="password"
                    value={pwForm[key]}
                    onChange={e => setPwForm(prev => ({ ...prev, [key]: e.target.value }))}
                    placeholder={placeholder}
                    className={inputCls}
                    style={{ borderColor: '#D1D5DB' }}
                  />
                </div>
              ))}
            </div>
            {pwError && <p className="text-[12px] font-medium" style={{ color: '#E05555' }}>{pwError}</p>}
            {pwMsg && <p className="text-[12px] font-medium" style={{ color: '#0D5C47' }}>{pwMsg}</p>}
            <div className="flex gap-2 pt-1">
              <button
                onClick={handlePasswordChange}
                disabled={pwSaving}
                className="px-10 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity disabled:opacity-50"
                style={{ backgroundColor: '#0D5C47' }}>
                {pwSaving ? '변경 중...' : '비밀번호 변경'}
              </button>
              <button
                onClick={() => { setPwForm({ current: '', next: '', confirm: '' }); setPwError(''); setPwMsg('') }}
                className="border rounded-xl px-8 py-2.5 text-[14px] font-medium transition-colors hover:bg-[#F0FAF7]"
                style={{ borderColor: '#D1D5DB', color: '#6B7280' }}>
                취소
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* 하단 카드 */}
      <div className="grid grid-cols-2 gap-6">
        <Link href="/banking/transfer-limit"
          className="rounded-xl p-6 flex items-center justify-between hover:bg-[#F0FAF7] transition-colors group"
          style={{ border: '1px solid #E2F5EF' }}>
          <div>
            <p className="text-[15px] font-bold text-kb-text group-hover:text-[#0D5C47]">이체한도 조회/변경 &gt;</p>
            <p className="text-[13px] text-kb-text-muted mt-1">1일·1회 이체한도를 확인하고 변경하세요.</p>
          </div>
        </Link>
        <Link href="/support/customer-info/withdraw"
          className="rounded-xl p-6 flex items-center justify-between hover:bg-red-50 transition-colors group"
          style={{ border: '1px solid #E2F5EF' }}>
          <div>
            <p className="text-[15px] font-bold text-kb-text group-hover:text-[#E05555]">회원탈퇴 &gt;</p>
            <p className="text-[13px] text-kb-text-muted mt-1">인터넷뱅킹 이용을 중단하고 탈퇴합니다.</p>
          </div>
        </Link>
      </div>
    </div>
  )
}
