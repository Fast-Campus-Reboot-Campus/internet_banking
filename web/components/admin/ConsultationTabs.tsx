'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useEffect, useState } from 'react'

const ALL_TABS = [
  { label: '상담 이력 조회',    href: '/admin/consultation/history', adminOnly: false },
  { label: '만족도 / 통계',     href: '/admin/consultation/stats',   adminOnly: false },
  { label: '상담원 계정 관리',  href: '/admin/consultation/agents',  adminOnly: true  },
]

export default function ConsultationTabs() {
  const pathname = usePathname()
  const [isAdmin, setIsAdmin] = useState(false)

  useEffect(() => {
    const role = localStorage.getItem('agentRole')
    const adminRoles: string[] = JSON.parse(localStorage.getItem('admin_roles') || '[]')
    setIsAdmin(role === 'ADMIN' || role === 'SUPERVISOR' || adminRoles.includes('ROLE_ADMIN'))
  }, [])

  const tabs = ALL_TABS.filter(t => !t.adminOnly || isAdmin)

  return (
    <div className="flex gap-0 border-b border-kb-border bg-white px-6">
      {tabs.map(t => {
        const active = pathname.startsWith(t.href)
        return (
          <Link
            key={t.href}
            href={t.href}
            className={`px-5 py-3 text-xs font-medium border-b-2 transition-colors whitespace-nowrap
              ${active
                ? 'border-kb-primary text-kb-primary'
                : 'border-transparent text-kb-text-muted hover:text-gray-700'}`}
          >
            {t.label}
          </Link>
        )
      })}
    </div>
  )
}
