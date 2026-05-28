'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import { AdminUser, AdminRole, ROLE_LABELS } from '@/lib/admin-mock-data'

type NavItem    = { label: string; href: string; roles?: AdminRole[] }
type NavSection = { section: string; dot: string; roles: AdminRole[]; items: NavItem[] }

const NAV: NavSection[] = [
  {
    section: '고객', dot: 'bg-blue-300',
    roles: ['ROLE_HQ_AUDIT', 'ROLE_HQ_REVIEW', 'ROLE_HQ_RISK', 'ROLE_HQ_MARKETING', 'ROLE_PRIMARY_OWNER', 'ROLE_BRANCH_STAFF'],
    items: [
      { label: '고객 조회', href: '/admin/customers' },
      { label: '감사 로그', href: '/admin/audit-log',
        roles: ['ROLE_HQ_AUDIT', 'ROLE_HQ_REVIEW', 'ROLE_PRIMARY_OWNER', 'ROLE_BRANCH_STAFF'] },
    ],
  },
  {
    section: '심사', dot: 'bg-orange-400',
    roles: ['ROLE_HQ_AUDIT', 'ROLE_HQ_REVIEW', 'ROLE_HQ_RISK'],
    items: [
      { label: '제재대상 Hit 검토', href: '/admin/screening' },
      { label: 'EDD 심사·승인',    href: '/admin/edd' },
      { label: '중복고객 검토',     href: '/admin/duplicates' },
      { label: '증표 위변조 검토',  href: '/admin/id-verify' },
      { label: '얼굴인증 라우팅',   href: '/admin/face-routing' },
      { label: '대리인 검토',       href: '/admin/agent' },
      { label: '미성년 검토',       href: '/admin/minor' },
    ],
  },
  {
    section: '회원관리', dot: 'bg-sky-400',
    roles: ['ROLE_HQ_AUDIT', 'ROLE_HQ_REVIEW', 'ROLE_HQ_RISK', 'ROLE_PRIMARY_OWNER', 'ROLE_BRANCH_STAFF'],
    items: [
      { label: '회원 목록',      href: '/admin/members' },
      { label: '회원 상태 관리', href: '/admin/member-status' },
    ],
  },
  {
    section: '정책', dot: 'bg-purple-400',
    roles: ['ROLE_HQ_AUDIT', 'ROLE_HQ_REVIEW', 'ROLE_HQ_RISK'],
    items: [
      { label: '약관 관리',     href: '/admin/terms' },
      { label: '동의이력 조회', href: '/admin/consent-log' },
      { label: 'FATCA/CRS',    href: '/admin/fatca' },
    ],
  },
  {
    section: 'AI 감사', dot: 'bg-red-400',
    roles: ['ROLE_HQ_AUDIT'],
    items: [
      { label: '감사 대시보드', href: '/admin/audit' },
      { label: '격리 관리',     href: '/admin/audit/quarantine' },
    ],
  },
  {
    section: '모니터링', dot: 'bg-kb-yellow',
    roles: ['ROLE_HQ_AUDIT', 'ROLE_HQ_RISK'],
    items: [
      { label: '가입 대시보드', href: '/admin/join-stats' },
    ],
  },
  {
    section: '마케팅', dot: 'bg-pink-400',
    roles: ['ROLE_HQ_AUDIT', 'ROLE_HQ_MARKETING'],
    items: [
      { label: '이벤트 목록', href: '/admin/events' },
      { label: '이벤트 등록', href: '/admin/events/new' },
      { label: '응모자 관리', href: '/admin/applicants' },
      { label: '당첨자 관리', href: '/admin/winners' },
      { label: '배너 관리',   href: '/admin/banners' },
      { label: '발송 관리',   href: '/admin/campaigns' },
      { label: '마케팅 통계', href: '/admin/marketing-stats' },
    ],
  },
]

const ROLE_BADGE: Record<AdminRole, string> = {
  ROLE_HQ_AUDIT:      'bg-red-500/20 text-red-300 border-red-400/40',
  ROLE_HQ_REVIEW:     'bg-orange-500/20 text-orange-300 border-orange-400/40',
  ROLE_HQ_RISK:       'bg-yellow-500/20 text-yellow-300 border-yellow-400/40',
  ROLE_HQ_MARKETING:  'bg-purple-500/20 text-purple-300 border-purple-400/40',
  ROLE_PRIMARY_OWNER: 'bg-blue-500/20 text-blue-300 border-blue-400/40',
  ROLE_BRANCH_STAFF:  'bg-kb-yellow/20 text-kb-yellow border-kb-yellow/40',
  ROLE_OTHER_BRANCH:  'bg-white/10 text-white/50 border-white/20',
}

export default function AdminSidebar() {
  const pathname = usePathname()
  const router = useRouter()
  const [user, setUser] = useState<AdminUser | null>(null)

  useEffect(() => {
    try {
      const s = localStorage.getItem('admin_user')
      if (s) setUser(JSON.parse(s))
    } catch {}
  }, [])

  function logout() {
    localStorage.removeItem('admin_role')
    localStorage.removeItem('admin_user')
    router.push('/admin/login')
  }

  const role = user?.role
  const visibleNav = role
    ? NAV
        .filter((g) => g.roles.includes(role))
        .map((g) => ({
          ...g,
          items: g.items.filter((item) => !item.roles || item.roles.includes(role)),
        }))
        .filter((g) => g.items.length > 0)
    : []

  return (
    <aside className="w-52 flex-shrink-0 flex flex-col min-h-screen" style={{ backgroundColor: '#1B3A6B' }}>

      {/* 로고 */}
      <div className="flex items-stretch gap-2.5 px-4 py-4" style={{ backgroundColor: '#122550', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
        <div className="w-[3px] rounded-full bg-kb-yellow self-stretch" />
        <div className="flex flex-col leading-none gap-1">
          <span className="text-[15px] font-bold text-white tracking-wide">AX풀뱅크</span>
          <span className="text-[9px] font-medium tracking-[0.18em] uppercase" style={{ color: 'rgba(255,255,255,0.45)' }}>Admin Console</span>
        </div>
      </div>

      {/* 유저 정보 */}
      {user && (
        <div className="flex items-center gap-2.5 px-4 py-3" style={{ backgroundColor: '#152E58', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
          <div className="w-7 h-7 rounded-full bg-kb-yellow-dark flex items-center justify-center flex-shrink-0">
            <span className="text-xs font-bold text-white">{user.name[0]}</span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-[12px] font-semibold text-white truncate">{user.name}</p>
            <span className={`text-[10px] border px-1.5 py-px font-medium rounded-sm inline-block mt-0.5 ${ROLE_BADGE[user.role]}`}>
              {ROLE_LABELS[user.role].split(' ')[0]}
            </span>
          </div>
          <button
            onClick={logout}
            title="로그아웃"
            className="text-[11px] transition-colors flex-shrink-0"
            style={{ color: 'rgba(255,255,255,0.35)' }}
            onMouseEnter={(e) => (e.currentTarget.style.color = '#F87171')}
            onMouseLeave={(e) => (e.currentTarget.style.color = 'rgba(255,255,255,0.35)')}
          >✕</button>
        </div>
      )}

      {/* 대시보드 */}
      <div className="px-3 pt-3 pb-1">
        <Link
          href="/admin/dashboard"
          className={`flex items-center gap-2 px-3 py-2 rounded-kb text-[12px] font-semibold transition-colors
            ${pathname === '/admin/dashboard'
              ? 'bg-kb-yellow-dark text-white'
              : 'text-white/80 hover:bg-white/10 hover:text-white'
            }`}
        >
          <svg width="13" height="13" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 1L1 7h2v7h4v-4h2v4h4V7h2L8 1z"/>
          </svg>
          대시보드
        </Link>
      </div>

      {/* 섹션별 네비게이션 */}
      <nav className="flex-1 overflow-y-auto pb-4">
        {visibleNav.map((group) => (
          <div key={group.section} className="mt-4">
            <div className="flex items-center gap-1.5 px-4 pb-1.5">
              <span className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${group.dot}`} />
              <p className="text-[10px] font-bold uppercase tracking-wider" style={{ color: 'rgba(255,255,255,0.4)' }}>
                {group.section}
              </p>
            </div>
            {group.items.map((item) => {
              const active = pathname === item.href || pathname.startsWith(item.href + '/')
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`block px-6 py-[7px] text-[12px] transition-colors border-l-2
                    ${active
                      ? 'border-kb-yellow text-white font-semibold'
                      : 'border-transparent text-white/60 hover:text-white hover:bg-white/8'
                    }`}
                  style={active ? { backgroundColor: 'rgba(91,201,168,0.15)' } : undefined}
                >
                  {item.label}
                </Link>
              )
            })}
          </div>
        ))}

        {role === 'ROLE_OTHER_BRANCH' && (
          <div className="mx-3 mt-4 px-3 py-3 rounded text-[11px] leading-relaxed" style={{ backgroundColor: 'rgba(239,68,68,0.15)', color: '#FCA5A5', border: '1px solid rgba(239,68,68,0.3)' }}>
            접근 가능한 메뉴가 없습니다.<br />
            임시 권한이 필요한 경우<br />
            관리자에게 문의하세요.
          </div>
        )}
      </nav>

      {/* 하단 로그아웃 */}
      <div className="px-4 py-3" style={{ borderTop: '1px solid rgba(255,255,255,0.08)' }}>
        <button
          onClick={logout}
          className="w-full text-left text-[11px] transition-colors"
          style={{ color: 'rgba(255,255,255,0.35)' }}
          onMouseEnter={(e) => (e.currentTarget.style.color = '#FCA5A5')}
          onMouseLeave={(e) => (e.currentTarget.style.color = 'rgba(255,255,255,0.35)')}
        >
          ← 로그아웃
        </button>
      </div>
    </aside>
  )
}
