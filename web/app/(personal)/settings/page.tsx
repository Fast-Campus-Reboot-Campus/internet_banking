'use client'

import { useState } from 'react'

type Tab = '프로필 수정' | '비밀번호 변경' | '알림 설정' | '화면 설정'
const TABS: Tab[] = ['프로필 수정', '비밀번호 변경', '알림 설정', '화면 설정']

/* ── 프로필 수정 ── */
function ProfileTab() {
  const [name, setName] = useState('홍길동')
  const [phone, setPhone] = useState('010-0000-0000')
  const [email, setEmail] = useState('test@axful.com')
  const [address, setAddress] = useState('서울특별시 ****동')
  const [saved, setSaved] = useState(false)

  function handleSave() {
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  return (
    <div className="space-y-0">
      <table className="w-full border-collapse text-[13px]">
        <tbody>
          {[
            { label: '이름', value: name, setter: setName, readOnly: true },
            { label: '휴대폰 번호', value: phone, setter: setPhone, readOnly: false },
            { label: '이메일', value: email, setter: setEmail, readOnly: false },
            { label: '자택 주소', value: address, setter: setAddress, readOnly: false },
          ].map(row => (
            <tr key={row.label}>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[160px]">
                {row.label}
              </td>
              <td className="border border-kb-border px-4 py-3">
                {row.readOnly ? (
                  <span className="text-kb-text-body">{row.value}</span>
                ) : (
                  <input
                    type="text"
                    value={row.value}
                    onChange={e => row.setter(e.target.value)}
                    className="w-full max-w-xs border border-kb-border px-3 py-1.5 text-[13px] focus:outline-none focus:border-kb-taupe"
                  />
                )}
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

  function handleChange() {
    setError('')
    if (!current || !next || !confirm) { setError('모든 항목을 입력해 주세요.'); return }
    if (next.length < 8) { setError('비밀번호는 8자 이상이어야 합니다.'); return }
    if (next !== confirm) { setError('새 비밀번호가 일치하지 않습니다.'); return }
    setSuccess(true)
    setCurrent(''); setNext(''); setConfirm('')
    setTimeout(() => setSuccess(false), 3000)
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
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[180px]">
                {row.label}
              </td>
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
type NotifKey = 'transfer' | 'login' | 'marketing' | 'event' | 'rate'
const NOTIF_ITEMS: { key: NotifKey; label: string; desc: string }[] = [
  { key: 'transfer', label: '이체 알림', desc: '입출금 거래 발생 시 알림을 받습니다.' },
  { key: 'login', label: '로그인 알림', desc: '새로운 기기에서 로그인 시 알림을 받습니다.' },
  { key: 'marketing', label: '마케팅 정보 수신', desc: '금융 상품 및 이벤트 정보를 받습니다.' },
  { key: 'event', label: '이벤트/혜택 알림', desc: '프로모션 및 포인트 적립 알림을 받습니다.' },
  { key: 'rate', label: '금리 변동 알림', desc: '보유 대출의 금리 변경 시 알림을 받습니다.' },
]

function NotificationTab() {
  const [notifs, setNotifs] = useState<Record<NotifKey, boolean>>({
    transfer: true, login: true, marketing: false, event: false, rate: true,
  })

  function toggle(key: NotifKey) {
    setNotifs(prev => ({ ...prev, [key]: !prev[key] }))
  }

  return (
    <div className="border border-kb-border-dark rounded-xl divide-y divide-kb-border overflow-hidden">
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
  )
}

/* ── 화면 설정 ── */
type FontSize = '소' | '중' | '대'
type ColorTheme = '기본' | '고대비'

function ScreenTab() {
  const [fontSize, setFontSize] = useState<FontSize>('중')
  const [colorTheme, setColorTheme] = useState<ColorTheme>('기본')
  const [mainPageLayout, setMainPageLayout] = useState('기본형')
  const [quickMenuVisible, setQuickMenuVisible] = useState(true)

  return (
    <div className="space-y-6">
      {/* 글자 크기 */}
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

      {/* 색상 테마 */}
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

      {/* 메인 화면 레이아웃 */}
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

      {/* 퀵메뉴 표시 */}
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

      <button className="bg-kb-yellow px-10 py-2.5 text-[13px] font-bold text-kb-text hover:brightness-95 transition-all">
        저장
      </button>
    </div>
  )
}

/* ── 메인 컴포넌트 ── */
export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<Tab>('프로필 수정')

  return (
    <div className="max-w-kb-container mx-auto px-6 py-10 pb-16">
      {/* 브레드크럼 */}
      <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
        <span>개인뱅킹</span><span>&gt;</span>
        <span className="font-semibold text-kb-text">설정</span>
      </div>

      <h1 className="text-2xl font-bold text-kb-text mb-6 pb-2 border-b-2 border-kb-text">설정</h1>

      {/* 탭 */}
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

      {/* 탭 콘텐츠 */}
      <div>
        {activeTab === '프로필 수정' && <ProfileTab />}
        {activeTab === '비밀번호 변경' && <PasswordTab />}
        {activeTab === '알림 설정' && <NotificationTab />}
        {activeTab === '화면 설정' && <ScreenTab />}
      </div>
    </div>
  )
}
