'use client'

import Link from 'next/link'
import { useState } from 'react'
import { usePathname } from 'next/navigation'

type Child = { label: string; href: string }
type SidebarItem = { label: string; expandable?: boolean; href?: string; children?: Child[] }

const NAV: SidebarItem[] = [
  {
    label: '예금',
    expandable: true,
    children: [
      { label: '예금 상품/가입',  href: '/products/deposit' },
      { label: '예금 리스트',     href: '/products/deposit/list' },
      { label: '판매중지상품',    href: '#' },
      { label: '예금 조회/해지',  href: '#' },
      { label: '예금 관리',       href: '#' },
      { label: '예금 가이드',     href: '#' },
    ],
  },
  {
    label: '대출',
    expandable: true,
    children: [
      { label: '대출 상품',  href: '/products/loan' },
      { label: '대출 신청',  href: '/loans/apply' },
      { label: '대출현황',   href: '/products/loan/status' },
    ],
  },
  { label: '펀드',     href: '#' },
  { label: '신탁/ISA', href: '#' },
  { label: '외화/골드', href: '#' },
  { label: '퇴직연금', href: '#' },
]

export default function ProductSidebar() {
  const pathname = usePathname()

  const defaultOpen = new Set(
    NAV.filter(item => item.children?.some(c => c.href !== '#' && pathname.startsWith(c.href)))
       .map(item => item.label)
  )
  if (defaultOpen.size === 0) defaultOpen.add('예금')

  const [openSections, setOpenSections] = useState<Set<string>>(defaultOpen)

  function toggle(label: string) {
    setOpenSections(prev => {
      const next = new Set(prev)
      next.has(label) ? next.delete(label) : next.add(label)
      return next
    })
  }

  return (
    <aside className="w-[180px] flex-shrink-0 border-r border-kb-border min-h-[700px] pt-6 pr-2">
      <h2 className="text-base font-bold text-kb-text mb-3 px-1">금융상품</h2>
      <nav>
        {NAV.map((item) => (
          <div key={item.label}>
            {item.expandable ? (
              <>
                <button
                  onClick={() => toggle(item.label)}
                  className="w-full flex items-center justify-between px-2 py-2 text-[13px] text-kb-text-body hover:text-kb-text"
                >
                  <span>{item.label}</span>
                  <span className="text-[10px] text-kb-text-muted">
                    {openSections.has(item.label) ? '˄' : '˅'}
                  </span>
                </button>
                {openSections.has(item.label) && item.children && (
                  <ul className="mb-1">
                    {item.children.map((child) => {
                      const active = child.href !== '#' && pathname === child.href
                      return (
                        <li key={child.label}>
                          <Link href={child.href}
                            className={`block px-3 py-1.5 text-[13px] ${
                              active
                                ? 'bg-kb-yellow font-semibold text-kb-text'
                                : 'text-kb-text-muted hover:text-kb-text hover:bg-kb-beige-light'
                            }`}>
                            {child.label}
                          </Link>
                        </li>
                      )
                    })}
                  </ul>
                )}
              </>
            ) : (
              <Link href={item.href ?? '#'}
                className="block px-2 py-2 text-[13px] text-kb-text-muted hover:text-kb-text">
                {item.label}
              </Link>
            )}
          </div>
        ))}
      </nav>
    </aside>
  )
}
