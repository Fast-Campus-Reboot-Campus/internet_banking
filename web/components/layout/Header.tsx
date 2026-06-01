'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useState, useEffect } from 'react'
import { api } from '@/lib/api'

// ============================================================
// GNB 메뉴 데이터
// megaMenu: 드롭다운 패널용 (카테고리 + 소분류 링크)
// ============================================================
export const GNB_MENUS = [
  {
    id: 'inquiry',
    label: '조회',
    href: '/inquiry/accounts',
    megaMenu: [
      {
        title: '계좌조회',
        href: '/inquiry/accounts',
        items: [
          { label: 'AXful Bank 계좌조회', href: '/inquiry/accounts' },
        ],
      },
      {
        title: '거래내역 조회',
        href: '/inquiry/transactions',
        items: [
          { label: '거래내역 조회', href: '/inquiry/transactions' },
        ],
      },
    ],
  },
  {
    id: 'transfer',
    label: '이체',
    href: '/transfer/account',
    megaMenu: [
      {
        title: '계좌이체',
        href: '/transfer/account',
        items: [
          { label: '계좌이체', href: '/transfer/account' },
        ],
      },
      {
        title: '이체결과 조회',
        href: '/transfer/inquiry',
        items: [
          { label: '계좌이체결과 조회', href: '/transfer/inquiry' },
        ],
      },
    ],
  },
  {
    id: 'products',
    label: '금융상품',
    href: '/products/loan',
    megaMenu: [
      {
        title: '예금 상품/가입',
        href: '/products/deposit/list',
        items: [
          { label: '예금 상품/가입',    href: '/products/deposit/list' },
          { label: '신규결과/내역 조회', href: '/products/deposit/inquiry/new' },
          { label: '예금해지',          href: '/products/deposit/inquiry/terminate' },
          { label: '해지결과/내역 조회', href: '/products/deposit/inquiry/terminate-result' },
          { label: '예금전환',          href: '/products/deposit/manage/convert' },
        ],
      },
      {
        title: '대출 상품/신청',
        href: '/products/loan/credit',
        items: [
          { label: '대출 상품/신청',               href: '/products/loan/credit' },
          { label: '대출진행현황',                href: '/products/loan/status' },
          { label: '대출관리',                    href: '/products/loan/manage/rate' },
          { label: '대출 가이드',                 href: '/products/loan/guide/rate' },
          { label: '신용평가 및 여신심사 자료제출', href: '/products/loan/credit-eval/biz-plan' },
        ],
      },
    ],
  },
  {
    id: 'manage',
    label: '뱅킹관리',
    href: '/banking/first-visit',
    megaMenu: [
      {
        title: '고객정보관리',
        href: '/support/customer-info/online-join',
        items: [
          { label: '온라인고객 신규가입', href: '/support/customer-info/online-join' },
          { label: '회원탈퇴',           href: '/support/customer-info/withdraw' },
          { label: '개인정보 수정',       href: '/settings' },
        ],
      },
      {
        title: '계좌관리',
        href: '/banking/transfer-limit',
        items: [
          { label: '이체한도 조회/변경', href: '/banking/transfer-limit' },
        ],
      },
      {
        title: '인터넷 뱅킹관리',
        href: '/banking/first-visit',
        items: [
          { label: 'ID조회/사용자암호 설정', href: '/support/customer-info/id-password' },
        ],
      },
      {
        title: '이용안내',
        href: '/support/internet-banking-guide',
        items: [
          { label: '첫 방문 고객을 위한 안내', href: '/banking/first-visit' },
          { label: '인터넷뱅킹 이용안내',      href: '/support/internet-banking-guide' },
          { label: '이용수수료 안내',          href: '/support/fee-guide' },
        ],
      },
    ],
  },
]


interface StoredUser { name: string; email: string; customer_id: number }

const SESSION_SECONDS = 10 * 60 // 10분

function formatTime(sec: number) {
  const m = String(Math.floor(sec / 60)).padStart(2, '0')
  const s = String(sec % 60).padStart(2, '0')
  return `${m}:${s}`
}

export default function Header() {
  const pathname = usePathname()
  const [activeMenu, setActiveMenu] = useState<string | null>(null)
  const [user, setUser] = useState<StoredUser | null>(null)
  const [remaining, setRemaining] = useState(SESSION_SECONDS)

  useEffect(() => {
    try {
      const stored = localStorage.getItem('user')
      if (stored) setUser(JSON.parse(stored))
    } catch {}
  }, [])

  // 카운트다운 + 자동 로그아웃
  useEffect(() => {
    if (!user) return

    const stored = localStorage.getItem('sessionExpiry')
    const expiry = stored ? parseInt(stored) : Date.now() + SESSION_SECONDS * 1000
    if (!stored) localStorage.setItem('sessionExpiry', String(expiry))

    let seconds = Math.max(0, Math.round((expiry - Date.now()) / 1000))
    setRemaining(seconds)

    const tick = setInterval(() => {
      const storedExpiry = localStorage.getItem('sessionExpiry')
      if (!storedExpiry) {
        clearInterval(tick)
        window.location.href = '/logout'
        return
      }
      const remaining = Math.max(0, Math.round((parseInt(storedExpiry) - Date.now()) / 1000))
      setRemaining(remaining)
      if (remaining <= 0) {
        clearInterval(tick)
        localStorage.removeItem('access_token')
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('sessionExpiry')
        localStorage.removeItem('user')
        localStorage.removeItem('customerId')
        window.location.href = '/logout'
      }
    }, 1000)

    return () => {
      clearInterval(tick)
    }
  }, [user])


  function handleLogout() {
    localStorage.removeItem('access_token')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('sessionExpiry')
    localStorage.removeItem('user')
    localStorage.removeItem('customerId')
    window.location.href = '/logout?reason=manual'
  }

  async function handleExtend() {
    const refreshToken = localStorage.getItem('refreshToken')
    if (!refreshToken) {
      window.location.href = '/logout'
      return
    }
    try {
      const { data } = await api.post('/api/v1/auth/refresh', { refreshToken })
      localStorage.setItem('accessToken', data.data.accessToken)
      localStorage.setItem('access_token', data.data.accessToken)
      if (data.data.refreshToken) {
        localStorage.setItem('refreshToken', data.data.refreshToken)
      }
      const newExpiry = Date.now() + SESSION_SECONDS * 1000
      localStorage.setItem('sessionExpiry', String(newExpiry))
      setRemaining(SESSION_SECONDS)
    } catch {
      window.location.href = '/logout'
    }
  }

  const currentMenu = GNB_MENUS.find((m) => m.id === activeMenu)
  const isLoginPage = pathname === '/login'
  const hideGnb = pathname.startsWith('/cert') || pathname.startsWith('/cert-biz') || isLoginPage || pathname.startsWith('/support')

  return (
    <header className="w-full bg-white relative z-50 shadow-sm">
      {/* ===== 1. 로고 + 사용자 영역 통합 ===== */}
      <div className="max-w-kb-container mx-auto px-6 flex items-center justify-between h-[60px]">
        {/* 로고 */}
        <Link href="/" className="flex items-center gap-3">
          <div className="w-[6px] h-[26px] rounded-full" style={{ backgroundColor: '#5BC9A8' }} />
          <span className="text-[26px] font-bold tracking-[0.02em]" style={{ color: '#0D5C47' }}>AXful Bank</span>
        </Link>

        {/* 우측: 사용자 영역 */}
        {!isLoginPage && (
          <div className="flex items-center gap-2 text-[14px]">
            {user ? (
              <>
                <span className="text-kb-text-muted font-medium">{user.name}님</span>
                <span className="text-kb-border">|</span>
                <Link href="/mypage" className="text-kb-text-muted hover:text-kb-text transition-colors">My AXful</Link>
                <span className="text-kb-border">|</span>
                <span className="text-kb-text-muted">🔒 {formatTime(remaining)}</span>
                <button onClick={handleExtend}
                  className="px-3 py-1 text-[13px] font-semibold rounded-full border transition-colors hover:bg-[#F0FAF7]"
                  style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
                  연장
                </button>
                <button onClick={handleLogout}
                  className="px-3 py-1 text-[13px] font-semibold rounded-full border border-gray-200 text-kb-text-muted hover:bg-gray-50 transition-colors">
                  로그아웃
                </button>
                <Link href="/cert"
                  className="px-3 py-1 text-[13px] font-semibold rounded-full text-white transition-opacity hover:opacity-85"
                  style={{ backgroundColor: '#0D5C47' }}>
                  인증센터
                </Link>
              </>
            ) : (
              <>
                <Link href="/login"
                  className="px-4 py-1 text-[13px] font-semibold rounded-full border transition-colors hover:bg-[#F0FAF7]"
                  style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
                  로그인
                </Link>
                <Link href="/cert"
                  className="px-4 py-1 text-[13px] font-semibold rounded-full text-white transition-opacity hover:opacity-85"
                  style={{ backgroundColor: '#0D5C47' }}>
                  인증센터
                </Link>
              </>
            )}
          </div>
        )}
      </div>

      {/* ===== 2. GNB + 메가메뉴 ===== */}
      {!hideGnb && <nav
        className="relative border-t"
        style={{ backgroundColor: '#F8FFFE', borderColor: '#E2F5EF' }}
        onMouseLeave={() => setActiveMenu(null)}
      >
        <div className="max-w-kb-container mx-auto px-6">
          <ul className="flex items-stretch w-full" style={{ height: '48px' }}>
            {GNB_MENUS.map((menu) => {
              const isActive = pathname === menu.href || pathname.startsWith(menu.href + '/')
              const isOpen = activeMenu === menu.id
              return (
                <li
                  key={menu.id}
                  className="flex grow"
                  onMouseEnter={() => setActiveMenu(menu.id)}
                >
                  <Link
                    href={menu.href}
                    onClick={() => setActiveMenu(null)}
                    className={`
                      flex items-center justify-center w-full px-5
                      text-[16px] font-semibold transition-colors duration-150 whitespace-nowrap relative
                      ${isActive
                        ? 'text-[#0D5C47] font-bold'
                        : isOpen
                        ? 'text-[#0D5C47] bg-[#F0FAF7]'
                        : 'text-kb-text-body hover:text-[#0D5C47] hover:bg-[#F0FAF7]'}
                    `}
                  >
                    {menu.label}
                    {isActive && (
                      <span className="absolute bottom-0 left-1/4 right-1/4 h-[2px] rounded-full" style={{ backgroundColor: '#5BC9A8' }} />
                    )}
                  </Link>
                </li>
              )
            })}
          </ul>
        </div>

        {/* ===== 메가메뉴 드롭다운 ===== */}
        {activeMenu && currentMenu && (
          <div className="absolute top-full left-0 right-0 bg-white border-b border-kb-border shadow-md z-50">
            <div className="max-w-kb-container mx-auto px-6 py-6">
              <div className={`grid ${currentMenu.megaMenu.length >= 4 ? 'grid-cols-4' : 'grid-cols-2'}`}>
                {currentMenu.megaMenu.map((category, ci) => (
                  <div key={category.title}
                    className={ci > 0 ? 'border-l border-kb-border pl-6 text-center' : 'text-center'}>
                    <Link
                      href={category.href}
                      onClick={() => setActiveMenu(null)}
                      className="block text-[17px] font-bold text-kb-text mb-1 hover:text-kb-taupe"
                    >
                      {category.title}
                    </Link>
                    {category.items.length > 0 && (
                      <ul className="space-y-0.5">
                        {category.items.map((item) => (
                          <li key={item.href} className="rounded hover:bg-[#e8f7f2] transition-colors duration-100">
                            <Link
                              href={item.href}
                              onClick={() => setActiveMenu(null)}
                              className="block text-[15px] py-2 text-kb-text-body hover:text-kb-text"
                            >
                              {item.label}
                            </Link>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </nav>}


    </header>
  )
}
