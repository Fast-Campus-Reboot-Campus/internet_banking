'use client'

import { useEffect, useState } from 'react'
import { api } from '@/lib/api'

interface SettingsData {
  name: string
  email: string | null
  phone: string | null
  zipCode: string | null
  address: string | null
  addressDetail: string | null
  smsReceiveYn: boolean
  emailReceiveYn: boolean
  postalReceiveYn: boolean
}

type Tab = '프로필 수정' | '비밀번호 변경' | '알림 설정' | '화면 설정'
const TABS: Tab[] = ['프로필 수정', '비밀번호 변경', '알림 설정', '화면 설정']

/* ── 프로필 수정 ── */
function ProfileTab({ data }: { data: SettingsData }) {
  const [phone, setPhone] = useState(data.phone ?? '')
  const [email, setEmail] = useState(data.email ?? '')
  const [zipCode, setZipCode] = useState(data.zipCode ?? '')
  const [address, setAddress] = useState(data.address ?? '')
  const [addressDetail, setAddressDetail] = useState(data.addressDetail ?? '')
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState('')

  async function handleSave() {
    setError('')
    try {
      await api.put('/api/v1/customers/me/profile', { email, phone, zipCode, address, addressDetail })
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    } catch {
      setError('저장에 실패했습니다.')
    }
  }

  return (
    <div>
      <table className="w-full border-collapse text-[13px]">
        <tbody>
          <tr>
            <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[160px]">이름</td>
            <td className="border border-kb-border px-4 py-3 text-kb-text-body">{data.name}</td>
          </tr>
          {[
            { label: '휴대폰 번호', value: phone, setter: setPhone },
            { label: '이메일', value: email, setter: setEmail },
            { label: '우편번호', value: zipCode, setter: setZipCode },
            { label: '자택 주소', value: address, setter: setAddress },
            { label: '상세 주소', value: addressDetail, setter: setAddressDetail },
          ].map(row => (
            <tr key={row.label}>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[160px]">{row.label}</td>
              <td className="border border-kb-border px-4 py-3">
                <input
                  type="text"
                  value={row.value}
                  onChange={e => row.setter(e.target.value)}
                  className="w-full max-w-xs border border-kb-border px-3 py-1.5 text-[13px] focus:outline-none focus:border-kb-taupe"
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="flex items-center gap-3 mt-5">
        <button
          onClick={handleSave}
          className="bg-kb-yellow px-10 py-2.5 text-[13px] font-bold text-kb-text hover:brightness-95 transition-all"
        >
          저장
        </button>
        {saved && <span className="text-[13px] text-kb-green">저장되었습니다.</span>}
        {error && <span className="text-[13px] text-kb-red">{error}</span>}
      </div>
    </div>
  )
}

/* ── 비밀번호 변경 ── */
function PasswordTab() {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  async function handleChange() {
    setError('')
    if (!current || !next || !confirm) { setError('모든 항목을 입력해 주세요.'); return }
    if (next.length < 8) { setError('비밀번호는 8자 이상이어야 합니다.'); return }
    if (next !== confirm) { setError('새 비밀번호가 일치하지 않습니다.'); return }
    try {
      await api.put('/api/v1/customers/me/password', { currentPassword: current, newPassword: next })
      setSuccess(true)
      setCurrent(''); setNext(''); setConfirm('')
      setTimeout(() => setSuccess(false), 3000)
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? '비밀번호 변경에 실패했습니다.')
    }
  }

  return (
    <div>
      <div className="border border-[#b3cce8] bg-[#f0f6ff] p-4 mb-5 text-[13px] text-kb-text-body space-y-1">
        <p>· 비밀번호는 영문, 숫자, 특수문자를 포함하여 8~20자로 설정해 주세요.</p>
        <p>· 동일한 비밀번호를 3회 이상 연속 사용할 수 없습니다.</p>
        <p>· 90일마다 비밀번호 변경을 권장합니다.</p>
      </div>
      <table className="w-full border-collapse text-[13px] mb-4">
        <tbody>
          {[
            { label: '현재 비밀번호', value: current, setter: setCurrent },
            { label: '새 비밀번호', value: next, setter: setNext },
            { label: '새 비밀번호 확인', value: confirm, setter: setConfirm },
          ].map(row => (
            <tr key={row.label}>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[180px]">{row.label}</td>
              <td className="border border-kb-border px-4 py-3">
                <input
                  type="password"
                  value={row.value}
                  onChange={e => row.setter(e.target.value)}
                  className="w-full max-w-xs border border-kb-border px-3 py-1.5 text-[13px] focus:outline-none focus:border-kb-taupe"
                  placeholder="입력하세요"
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {error && <p className="text-[13px] text-kb-red mb-3">{error}</p>}
      {success && <p className="text-[13px] text-kb-green mb-3">비밀번호가 변경되었습니다.</p>}
      <button
        onClick={handleChange}
        className="bg-kb-yellow px-10 py-2.5 text-[13px] font-bold text-kb-text hover:brightness-95 transition-all"
      >
        비밀번호 변경
      </button>
    </div>
  )
}

/* ── 알림 설정 ── */
type NotifKey = 'smsReceiveYn' | 'emailReceiveYn' | 'postalReceiveYn'

const NOTIF_ITEMS: { key: NotifKey; label: string; desc: string }[] = [
  { key: 'smsReceiveYn',   label: 'SMS 수신 알림',    desc: '이체·로그인 등 주요 거래 알림을 SMS로 받습니다.' },
  { key: 'emailReceiveYn', label: '이메일 수신 알림',  desc: '마케팅 및 이벤트 정보를 이메일로 받습니다.' },
  { key: 'postalReceiveYn',label: '우편 수신 알림',    desc: '금리 변동 등 금융 안내를 우편으로 받습니다.' },
]

function NotificationTab({ data }: { data: SettingsData }) {
  const [notifs, setNotifs] = useState<Record<NotifKey, boolean>>({
    smsReceiveYn:    data.smsReceiveYn,
    emailReceiveYn:  data.emailReceiveYn,
    postalReceiveYn: data.postalReceiveYn,
  })
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState('')

  function toggle(key: NotifKey) {
    setNotifs(prev => ({ ...prev, [key]: !prev[key] }))
  }

  async function handleSave() {
    setError('')
    try {
      await api.put('/api/v1/customers/me/notification', notifs)
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    } catch {
      setError('저장에 실패했습니다.')
    }
  }

  return (
    <div>
      <div className="border border-kb-border-dark rounded-xl divide-y divide-kb-border overflow-hidden mb-5">
        {NOTIF_ITEMS.map(item => (
          <div key={item.key} className="flex items-center justify-between px-5 py-4">
            <div>
              <p className="text-base font-semibold text-kb-text">{item.label}</p>
              <p className="text-sm text-kb-text-muted mt-0.5">{item.desc}</p>
            </div>
            <button
              onClick={() => toggle(item.key)}
              className={`relative w-12 h-6 rounded-full transition-colors flex-shrink-0 ${notifs[item.key] ? 'bg-kb-blue' : 'bg-gray-300'}`}
            >
              <span className={`absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform ${notifs[item.key] ? 'translate-x-7' : 'translate-x-1'}`} />
            </button>
          </div>
        ))}
      </div>
      <div className="flex items-center gap-3">
        <button
          onClick={handleSave}
          className="bg-kb-yellow px-10 py-2.5 text-[13px] font-bold text-kb-text hover:brightness-95 transition-all"
        >
          저장
        </button>
        {saved && <span className="text-[13px] text-kb-green">저장되었습니다.</span>}
        {error && <span className="text-[13px] text-kb-red">{error}</span>}
      </div>
    </div>
  )
}

/* ── 화면 설정 (localStorage) ── */
type FontSize = '소' | '중' | '대'
type ColorTheme = '기본' | '고대비'

function ScreenTab() {
  const [fontSize, setFontSize] = useState<FontSize>('중')
  const [colorTheme, setColorTheme] = useState<ColorTheme>('기본')
  const [mainPageLayout, setMainPageLayout] = useState('기본형')
  const [quickMenuVisible, setQuickMenuVisible] = useState(true)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    const stored = localStorage.getItem('screenSettings')
    if (stored) {
      try {
        const s = JSON.parse(stored)
        if (s.fontSize) setFontSize(s.fontSize)
        if (s.colorTheme) setColorTheme(s.colorTheme)
        if (s.mainPageLayout) setMainPageLayout(s.mainPageLayout)
        if (s.quickMenuVisible !== undefined) setQuickMenuVisible(s.quickMenuVisible)
      } catch { /* ignore */ }
    }
  }, [])

  function handleSave() {
    localStorage.setItem('screenSettings', JSON.stringify({ fontSize, colorTheme, mainPageLayout, quickMenuVisible }))
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-[14px] font-bold text-kb-text mb-3">글자 크기</h3>
        <div className="flex gap-2">
          {(['소', '중', '대'] as FontSize[]).map(s => (
            <button
              key={s}
              onClick={() => setFontSize(s)}
              className={`border px-8 py-2.5 transition-colors ${
                fontSize === s
                  ? 'bg-kb-yellow border-kb-taupe font-bold text-kb-text'
                  : 'border-kb-border text-kb-text-muted hover:bg-kb-beige-light'
              }`}
              style={{ fontSize: s === '소' ? '12px' : s === '중' ? '14px' : '16px' }}
            >
              가
            </button>
          ))}
        </div>
      </div>

      <div>
        <h3 className="text-[14px] font-bold text-kb-text mb-3">색상 테마</h3>
        <div className="flex gap-2">
          {(['기본', '고대비'] as ColorTheme[]).map(t => (
            <button
              key={t}
              onClick={() => setColorTheme(t)}
              className={`border px-6 py-2 text-[13px] transition-colors ${
                colorTheme === t
                  ? 'bg-kb-yellow border-kb-taupe font-bold text-kb-text'
                  : 'border-kb-border text-kb-text-muted hover:bg-kb-beige-light'
              }`}
            >
              {t}
            </button>
          ))}
        </div>
      </div>

      <div>
        <h3 className="text-[14px] font-bold text-kb-text mb-3">메인 화면 레이아웃</h3>
        <div className="flex gap-2">
          {['기본형', '간편형'].map(l => (
            <button
              key={l}
              onClick={() => setMainPageLayout(l)}
              className={`border px-6 py-2 text-[13px] transition-colors ${
                mainPageLayout === l
                  ? 'bg-kb-yellow border-kb-taupe font-bold text-kb-text'
                  : 'border-kb-border text-kb-text-muted hover:bg-kb-beige-light'
              }`}
            >
              {l}
            </button>
          ))}
        </div>
      </div>

      <div className="flex items-center justify-between border border-kb-border-dark rounded-xl px-5 py-4">
        <div>
          <p className="text-base font-semibold text-kb-text">퀵메뉴 표시</p>
          <p className="text-sm text-kb-text-muted mt-0.5">메인 화면 상단 퀵메뉴 바를 표시합니다.</p>
        </div>
        <button
          onClick={() => setQuickMenuVisible(v => !v)}
          className={`relative w-12 h-6 rounded-full transition-colors flex-shrink-0 ${quickMenuVisible ? 'bg-kb-blue' : 'bg-gray-300'}`}
        >
          <span className={`absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform ${quickMenuVisible ? 'translate-x-7' : 'translate-x-1'}`} />
        </button>
      </div>

      <div className="flex items-center gap-3">
        <button
          onClick={handleSave}
          className="bg-kb-yellow px-10 py-2.5 text-[13px] font-bold text-kb-text hover:brightness-95 transition-all"
        >
          저장
        </button>
        {saved && <span className="text-[13px] text-kb-green">저장되었습니다.</span>}
      </div>
    </div>
  )
}

/* ── 메인 컴포넌트 ── */
export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<Tab>('프로필 수정')
  const [settingsData, setSettingsData] = useState<SettingsData | null>(null)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    api.get('/api/v1/customers/me/settings')
      .then(res => setSettingsData(res.data.data))
      .catch(() => setLoadError('설정 정보를 불러오지 못했습니다.'))
  }, [])

  return (
    <div className="max-w-kb-container mx-auto px-6 py-10 pb-16">
      <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
        <span>개인뱅킹</span><span>&gt;</span>
        <span className="font-semibold text-kb-text">설정</span>
      </div>

      <h1 className="text-2xl font-bold text-kb-text mb-6 pb-2 border-b-2 border-kb-text">설정</h1>

      <div className="flex border-b border-kb-border mb-6">
        {TABS.map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-5 py-3 text-base border-b-2 -mb-px transition-colors ${
              activeTab === tab
                ? 'border-kb-text font-bold text-kb-text'
                : 'border-transparent text-kb-text-muted hover:text-kb-text'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      <div>
        {loadError && (
          <p className="text-[13px] text-kb-red mb-4">{loadError}</p>
        )}
        {!settingsData && !loadError && (
          <p className="text-[13px] text-kb-text-muted">불러오는 중...</p>
        )}
        {settingsData && (
          <>
            {activeTab === '프로필 수정'  && <ProfileTab data={settingsData} />}
            {activeTab === '비밀번호 변경' && <PasswordTab />}
            {activeTab === '알림 설정'    && <NotificationTab data={settingsData} />}
            {activeTab === '화면 설정'    && <ScreenTab />}
          </>
        )}
      </div>
    </div>
  )
}
