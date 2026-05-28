'use client'

import Link from 'next/link'
import { useState } from 'react'
import { usePathname } from 'next/navigation'

type Child = { label: string; href: string }
type SidebarItem = { label: string; expandable?: boolean; href?: string; children?: Child[] }

const SIDEBAR_INQUIRY: SidebarItem[] = [
  {
    label: '계좌조회',
    expandable: true,
    children: [
      { label: 'AX풀뱅크 계좌조회', href: '/inquiry/accounts' },
      { label: '다른금융 조회', href: '#' },
      { label: 'AXful금융그룹통합 조회', href: '#' },
      { label: '휴면계좌 조회', href: '#' },
      { label: '전자통장 조회', href: '#' },
      { label: '장기미거래신탁계좌 조회', href: '#' },
      { label: '계좌종합관리 계좌조회', href: '#' },
      { label: '착오송금 반환 동의', href: '#' },
      { label: '오픈뱅킹 착오송금 반환 신청', href: '#' },
      { label: '공채 본인부담금 조회', href: '#' },
    ],
  },
  {
    label: '거래내역 조회',
    expandable: true,
    children: [
      { label: '거래내역 조회', href: '/inquiry/transactions' },
      { label: '다른금융 통합거래내역조회', href: '#' },
    ],
  },
  {
    label: '전자어음 조회',
    expandable: true,
    children: [
      { label: '전자어음 조회', href: '#' },
      { label: '전자어음 발행조회', href: '#' },
    ],
  },
  {
    label: '에스크로 조회',
    expandable: true,
    children: [
      { label: '에스크로 거래조회', href: '#' },
      { label: '에스크로 입금확인', href: '#' },
    ],
  },
  {
    label: '수표어음 조회',
    expandable: true,
    children: [
      { label: '수표조회', href: '#' },
      { label: '어음조회', href: '#' },
    ],
  },
  { label: '어카운트인포', href: '#' },
  { label: '계약서류 관리', href: '#' },
]

export default function InquirySidebar() {
  const pathname = usePathname()

  const defaultOpen = new Set(
    SIDEBAR_INQUIRY
      .filter(item => item.children?.some(c => pathname.startsWith(c.href) && c.href !== '#'))
      .map(item => item.label)
  )
  if (defaultOpen.size === 0) defaultOpen.add('계좌조회')

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
      <h2 className="text-base font-bold text-kb-text mb-3 px-1">조회</h2>
      <nav>
        {SIDEBAR_INQUIRY.map((item) => (
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
                          <Link
                            href={child.href}
                            className={`block px-3 py-1.5 text-[13px] ${
                              active
                                ? 'bg-kb-yellow font-semibold text-kb-text'
                                : 'text-kb-text-muted hover:text-kb-text hover:bg-kb-beige-light'
                            }`}
                          >
                            {child.label}
                          </Link>
                        </li>
                      )
                    })}
                  </ul>
                )}
              </>
            ) : (
              <Link
                href={item.href ?? '#'}
                className="block px-2 py-2 text-[13px] text-kb-text-muted hover:text-kb-text"
              >
                {item.label}
              </Link>
            )}
          </div>
        ))}
      </nav>
    </aside>
  )
}
