'use client'

import Link from 'next/link'
import { useState } from 'react'
import { usePathname } from 'next/navigation'

type Child = { label: string; href: string }
type SidebarItem = { label: string; expandable?: boolean; href?: string; children?: Child[] }

const NAV: SidebarItem[] = [
  {
    label: '제증명발급',
    expandable: true,
    children: [
      { label: '연말정산증명서',           href: '/manage/certificates/year-end' },
      { label: '통장사본',                 href: '#' },
      { label: '예금잔액증명서',            href: '#' },
      { label: '금융거래확인서',            href: '#' },
      { label: '원천징수영수증 발급',        href: '#' },
      { label: '금융소득종합과세 조회',       href: '#' },
      { label: '증명서/서류목록',           href: '#' },
    ],
  },
  { label: '계좌관리',         href: '#' },
  {
    label: '인터넷 뱅킹관리',
    expandable: true,
    children: [
      { label: '첫 방문 고객을 위한 안내', href: '/banking/first-visit' },
      { label: '이체한도 변경',           href: '/banking/transfer-limit' },
      { label: '인터넷뱅킹 FAQ',         href: '#' },
      { label: '이용시간 안내',           href: '#' },
      { label: '인터넷뱅킹 이용안내',     href: '#' },
      { label: '이용수수료 안내',         href: '#' },
    ],
  },
  { label: '계좌종합관리서비스', href: '#' },
]

export default function ManageSidebar() {
  const pathname = usePathname()

  const defaultOpen = new Set(
    NAV.filter(item => item.children?.some(c => c.href !== '#' && pathname.startsWith(c.href)))
       .map(item => item.label)
  )
  if (defaultOpen.size === 0) defaultOpen.add('제증명발급')

  const [openSections, setOpenSections] = useState<Set<string>>(defaultOpen)

  function toggle(label: string) {
    setOpenSections(prev => {
      const next = new Set(prev)
      if (next.has(label)) { next.delete(label) } else { next.add(label) }
      return next
    })
  }

  return (
    <aside className="w-[180px] flex-shrink-0 border-r border-kb-border min-h-[700px] pt-6 pr-2">
      <h2 className="text-base font-bold text-kb-text mb-3 px-1">뱅킹관리</h2>
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
                      const active = child.href !== '#' && pathname.startsWith(child.href)
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
