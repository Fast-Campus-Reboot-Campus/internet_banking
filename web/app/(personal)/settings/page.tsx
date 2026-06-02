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

const inputCls  = "border rounded-lg px-3 py-2 text-[13px] outline-none focus:ring-1 transition-all"
const inputStyle = { borderColor: '#D1D5DB' }

function Toggle({ checked, onChange }: { checked: boolean; onChange: () => void }) {
  return (
    <button
      onClick={onChange}
      className="relative w-10 h-5 rounded-full transition-colors flex-shrink-0"
      style={{ backgroundColor: checked ? '#0D5C47' : '#D1D5DB' }}
    >
      <span className={`absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${checked ? 'translate-x-5' : 'translate-x-0.5'}`} />
    </button>
  )
}

function SaveBar({ msg, isError, onSave, saving, label = '저장' }: {
  msg: string; isError: boolean; onSave: () => void; saving: boolean; label?: string
}) {
  return (
    <div className="flex items-center gap-3 mt-6">
      <button
        onClick={onSave}
        disabled={saving}
        className="px-12 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity disabled:opacity-50"
        style={{ backgroundColor: '#0D5C47' }}>
        {saving ? '저장 중...' : label}
      </button>
      {msg && (
        <span className="text-[13px] font-medium" style={{ color: isError ? '#E05555' : '#0D5C47' }}>
          {msg}
        </span>
      )}
    </div>
  )
}

/* ── 프로필 수정 ── */
function ProfileTab({ data }: { data: SettingsData }) {
  const [phone, setPhone]               = useState(data.phone ?? '')
  const [email, setEmail]               = useState(data.email ?? '')
  const [zipCode, setZipCode]           = useState(data.zipCode ?? '')
  const [address, setAddress]           = useState(data.address ?? '')
  const [addressDetail, setAddressDetail] = useState(data.addressDetail ?? '')
  const [saving, setSaving]             = useState(false)
  const [msg, setMsg]                   = useState('')
  const [isError, setIsError]           = useState(false)

  async function handleSave() {
    setSaving(true); setMsg(''); setIsError(false)
    try {
      await api.put('/api/v1/customers/me/profile', { email, phone, zipCode, address, addressDetail })
      setMsg('저장되었습니다.')
    } catch {
      setMsg('저장에 실패했습니다.'); setIsError(true)
    } finally {
      setSaving(false)
      setTimeout(() => setMsg(''), 3000)
    }
  }

  const rows = [
    { label: '휴대폰 번호', value: phone,         setter: setPhone,         type: 'tel' },
    { label: '이메일',      value: email,         setter: setEmail,         type: 'email' },
    { label: '우편번호',    value: zipCode,       setter: setZipCode,       type: 'text' },
    { label: '자택 주소',   value: address,       setter: setAddress,       type: 'text' },
    { label: '상세 주소',   value: addressDetail, setter: setAddressDetail, type: 'text' },
  ]

  return (
    <div>
      <div className="rounded-xl overflow-hidden" style={{ border: '1px solid #E2F5EF' }}>
        <div className="flex" style={{ borderBottom: '1px solid #E2F5EF', backgroundColor: '#F0FAF7' }}>
          <div className="w-[160px] px-5 py-3 text-[13px] font-semibold flex-shrink-0" style={{ color: '#0D5C47' }}>이름</div>
          <div className="flex-1 px-5 py-3 text-[13px] text-kb-text">{data.name}</div>
        </div>
        {rows.map((row, i) => (
          <div key={row.label} className="flex items-center"
            style={{ borderBottom: i < rows.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
            <div className="w-[160px] px-5 py-3 text-[13px] font-semibold flex-shrink-0"
              style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', alignSelf: 'stretch', display: 'flex', alignItems: 'center' }}>
              {row.label}
            </div>
            <div className="flex-1 px-5 py-2.5">
              <input
                type={row.type}
                value={row.value}
                onChange={e => row.setter(e.target.value)}
                className={inputCls + ' w-full max-w-sm'}
                style={inputStyle}
              />
            </div>
          </div>
        ))}
      </div>
      <SaveBar msg={msg} isError={isError} onSave={handleSave} saving={saving} />
    </div>
  )
}

/* ── 비밀번호 변경 ── */
function PasswordTab() {
  const [current, setCurrent] = useState('')
  const [next, setNext]       = useState('')
  const [confirm, setConfirm] = useState('')
  const [saving, setSaving]   = useState(false)
  const [msg, setMsg]         = useState('')
  const [isError, setIsError] = useState(false)

  async function handleChange() {
    setMsg(''); setIsError(false)
    if (!current || !next || !confirm) { setMsg('모든 항목을 입력해 주세요.'); setIsError(true); return }
    if (next.length < 8)               { setMsg('비밀번호는 8자 이상이어야 합니다.'); setIsError(true); return }
    if (next !== confirm)              { setMsg('새 비밀번호가 일치하지 않습니다.'); setIsError(true); return }
    setSaving(true)
    try {
      await api.put('/api/v1/customers/me/password', { currentPassword: current, newPassword: next })
      setMsg('비밀번호가 변경되었습니다.'); setIsError(false)
      setCurrent(''); setNext(''); setConfirm('')
    } catch (e: unknown) {
      const m = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      setMsg(m ?? '비밀번호 변경에 실패했습니다. 현재 비밀번호를 확인해주세요.')
      setIsError(true)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <div className="rounded-xl px-5 py-4 mb-5 text-[12px] space-y-1.5"
        style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
        <p className="text-kb-text-muted">· 비밀번호는 영문·숫자·특수문자를 포함하여 8~20자로 설정해 주세요.</p>
        <p className="text-kb-text-muted">· 동일한 비밀번호를 3회 이상 연속 사용할 수 없습니다.</p>
        <p className="text-kb-text-muted">· 90일마다 비밀번호 변경을 권장합니다.</p>
      </div>

      <div className="rounded-xl overflow-hidden" style={{ border: '1px solid #E2F5EF' }}>
        {[
          { label: '현재 비밀번호',    value: current, setter: setCurrent },
          { label: '새 비밀번호',      value: next,    setter: setNext },
          { label: '새 비밀번호 확인', value: confirm, setter: setConfirm },
        ].map((row, i, arr) => (
          <div key={row.label} className="flex items-center"
            style={{ borderBottom: i < arr.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
            <div className="w-[180px] px-5 py-3 text-[13px] font-semibold flex-shrink-0"
              style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', alignSelf: 'stretch', display: 'flex', alignItems: 'center' }}>
              {row.label}
            </div>
            <div className="flex-1 px-5 py-2.5">
              <input
                type="password"
                value={row.value}
                onChange={e => row.setter(e.target.value)}
                placeholder="입력하세요"
                className={inputCls + ' w-full max-w-sm'}
                style={inputStyle}
              />
            </div>
          </div>
        ))}
      </div>

      <SaveBar msg={msg} isError={isError} onSave={handleChange} saving={saving} label="비밀번호 변경" />
    </div>
  )
}

/* ── 알림 설정 ── */
type NotifKey = 'smsReceiveYn' | 'emailReceiveYn' | 'postalReceiveYn'

const NOTIF_ITEMS: { key: NotifKey; label: string; desc: string }[] = [
  { key: 'smsReceiveYn',    label: 'SMS 수신',    desc: '이체·로그인 등 주요 거래 알림을 SMS로 받습니다.' },
  { key: 'emailReceiveYn',  label: '이메일 수신', desc: '마케팅 및 이벤트 정보를 이메일로 받습니다.' },
  { key: 'postalReceiveYn', label: '우편 수신',   desc: '금리 변동 등 금융 안내를 우편으로 받습니다.' },
]

function NotificationTab({ data }: { data: SettingsData }) {
  const [notifs, setNotifs] = useState<Record<NotifKey, boolean>>({
    smsReceiveYn:    data.smsReceiveYn,
    emailReceiveYn:  data.emailReceiveYn,
    postalReceiveYn: data.postalReceiveYn,
  })
  const [saving, setSaving] = useState(false)
  const [msg, setMsg]       = useState('')
  const [isError, setIsError] = useState(false)

  async function handleSave() {
    setSaving(true); setMsg(''); setIsError(false)
    try {
      await api.put('/api/v1/customers/me/notification', notifs)
      setMsg('알림설정이 저장되었습니다.')
    } catch {
      setMsg('저장에 실패했습니다.'); setIsError(true)
    } finally {
      setSaving(false)
      setTimeout(() => setMsg(''), 3000)
    }
  }

  return (
    <div>
      <div className="rounded-xl overflow-hidden mb-2" style={{ border: '1px solid #E2F5EF' }}>
        {NOTIF_ITEMS.map((item, i, arr) => (
          <div key={item.key}
            className="flex items-center justify-between px-5 py-4"
            style={{ borderBottom: i < arr.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
            <div>
              <p className="text-[14px] font-semibold text-kb-text">{item.label}</p>
              <p className="text-[12px] text-kb-text-muted mt-0.5">{item.desc}</p>
            </div>
            <Toggle checked={notifs[item.key]} onChange={() => setNotifs(p => ({ ...p, [item.key]: !p[item.key] }))} />
          </div>
        ))}
      </div>
      <SaveBar msg={msg} isError={isError} onSave={handleSave} saving={saving} />
    </div>
  )
}

/* ── 화면 설정 ── */
type FontSize   = '소' | '중' | '대'
type ColorTheme = '기본' | '고대비'

function ScreenTab() {
  const [fontSize,         setFontSize]         = useState<FontSize>('중')
  const [colorTheme,       setColorTheme]       = useState<ColorTheme>('기본')
  const [mainPageLayout,   setMainPageLayout]   = useState('기본형')
  const [quickMenuVisible, setQuickMenuVisible] = useState(true)
  const [msg, setMsg]                           = useState('')

  useEffect(() => {
    try {
      const s = JSON.parse(localStorage.getItem('screenSettings') || '{}')
      if (s.fontSize)         setFontSize(s.fontSize)
      if (s.colorTheme)       setColorTheme(s.colorTheme)
      if (s.mainPageLayout)   setMainPageLayout(s.mainPageLayout)
      if (s.quickMenuVisible !== undefined) setQuickMenuVisible(s.quickMenuVisible)
    } catch {}
  }, [])

  function handleSave() {
    localStorage.setItem('screenSettings', JSON.stringify({ fontSize, colorTheme, mainPageLayout, quickMenuVisible }))
    setMsg('저장되었습니다.')
    setTimeout(() => setMsg(''), 2000)
  }

  const optionBtn = (active: boolean, onClick: () => void, children: React.ReactNode) => (
    <button onClick={onClick}
      className="border rounded-lg px-7 py-2 text-[13px] font-medium transition-colors"
      style={active
        ? { backgroundColor: '#0D5C47', borderColor: '#0D5C47', color: 'white', fontWeight: 700 }
        : { borderColor: '#E2F5EF', color: '#6B7280' }}>
      {children}
    </button>
  )

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-[14px] font-bold text-kb-text mb-3">글자 크기</h3>
        <div className="flex gap-2">
          {(['소', '중', '대'] as FontSize[]).map(s => optionBtn(fontSize === s, () => setFontSize(s),
            <span style={{ fontSize: s === '소' ? '12px' : s === '중' ? '14px' : '16px' }}>가 ({s})</span>
          ))}
        </div>
      </div>

      <div>
        <h3 className="text-[14px] font-bold text-kb-text mb-3">색상 테마</h3>
        <div className="flex gap-2">
          {(['기본', '고대비'] as ColorTheme[]).map(t => optionBtn(colorTheme === t, () => setColorTheme(t), t))}
        </div>
      </div>

      <div>
        <h3 className="text-[14px] font-bold text-kb-text mb-3">메인 화면 레이아웃</h3>
        <div className="flex gap-2">
          {['기본형', '간편형'].map(l => optionBtn(mainPageLayout === l, () => setMainPageLayout(l), l))}
        </div>
      </div>

      <div className="flex items-center justify-between rounded-xl px-5 py-4"
        style={{ border: '1px solid #E2F5EF' }}>
        <div>
          <p className="text-[14px] font-semibold text-kb-text">퀵메뉴 표시</p>
          <p className="text-[12px] text-kb-text-muted mt-0.5">메인 화면 상단 퀵메뉴 바를 표시합니다.</p>
        </div>
        <Toggle checked={quickMenuVisible} onChange={() => setQuickMenuVisible(v => !v)} />
      </div>

      <div className="flex items-center gap-3">
        <button onClick={handleSave}
          className="px-12 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity"
          style={{ backgroundColor: '#0D5C47' }}>
          저장
        </button>
        {msg && <span className="text-[13px] font-medium" style={{ color: '#0D5C47' }}>{msg}</span>}
      </div>
    </div>
  )
}

/* ── 메인 ── */
export default function SettingsPage() {
  const [activeTab,    setActiveTab]    = useState<Tab>('프로필 수정')
  const [settingsData, setSettingsData] = useState<SettingsData | null>(null)
  const [loadError,    setLoadError]    = useState('')

  useEffect(() => {
    api.get('/api/v1/customers/me/settings')
      .then(res => setSettingsData(res.data.data))
      .catch(() => setLoadError('설정 정보를 불러오지 못했습니다.'))
  }, [])

  return (
    <div className="max-w-kb-container mx-auto px-6 py-10 pb-16">
      <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
        <span>개인뱅킹</span><span>›</span>
        <span className="font-semibold text-kb-text">설정</span>
      </div>

      <h1 className="text-[22px] font-bold text-kb-text mb-6">설정</h1>

      {/* 탭 */}
      <div className="flex border-b mb-6" style={{ borderColor: '#E2F5EF' }}>
        {TABS.map(tab => (
          <button key={tab} onClick={() => setActiveTab(tab)}
            className="px-6 py-3 text-[14px] font-medium border-b-2 -mb-px transition-colors whitespace-nowrap"
            style={activeTab === tab
              ? { borderColor: '#0D5C47', color: '#0D5C47', fontWeight: 700 }
              : { borderColor: 'transparent', color: '#9CA3AF' }}>
            {tab}
          </button>
        ))}
      </div>

      {/* 콘텐츠 */}
      {loadError && <p className="text-[13px]" style={{ color: '#E05555' }}>{loadError}</p>}
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
  )
}
