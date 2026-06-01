'use client'

import Link from 'next/link'
import { useState, useEffect } from 'react'
import { usePathname } from 'next/navigation'

type Child = { label: string; href: string; disabled?: boolean }
type SidebarItem = { label: string; expandable?: boolean; href?: string; children?: Child[] }

const NAV: SidebarItem[] = [
  {
    label: '대출 상품/신청',
    expandable: true,
    children: [
      { label: '신용대출',             href: '/products/loan/credit' },
      { label: '담보대출',             href: '/products/loan/mortgage' },
      { label: '전월세/반환보증',       href: '/products/loan/jeonse' },
      { label: '자동차대출',           href: '/products/loan/auto' },
      { label: '집단중도금/이주비대출', href: '/products/loan/group' },
      { label: '주택도시기금대출',      href: '/products/loan/khfc' },
      { label: '개인사업자대출',        href: '/products/loan/biz' },
    ],
  },
  {
    label: '대출진행현황',
    expandable: true,
    children: [
      { label: '진행현황조회/실행/예약', href: '/products/loan/status' },
    ],
  },
  {
    label: '대출 가이드',
    expandable: true,
    children: [
      { label: '가계대출금리',                          href: '/products/loan/guide/rate' },
      { label: '대출관련 수수료',                       href: '/products/loan/guide/fee' },
      { label: '금리인하요구권',                        href: '/products/loan/guide/rate-cut' },
      { label: '대출연체시 지연배상금액 예시',           href: '/products/loan/guide/late-fee' },
      { label: '부가서비스',                            href: '/products/loan/guide/benefits' },
      { label: '채무조정 지원제도 안내',                 href: '/products/loan/guide/debt-adjustment' },
      { label: '내용증명 우편미수신 주요정보 안내',      href: '/products/loan/guide/notice' },
      { label: '추심관련 권리의무 및 권리구제방법 안내', href: '/products/loan/guide/rights' },
    ],
  },
]

function getDefaultOpen(pathname: string): string[] {
  const matched = NAV
    .filter(item =>
      item.children?.some(c => pathname === c.href || pathname.startsWith(c.href + '/'))
    )
    .map(item => item.label)
  return matched.length > 0 ? matched : ['대출 상품/신청']
}

export default function LoanSidebar() {
  const pathname = usePathname()
  const [openSections, setOpenSections] = useState<string[]>(() => getDefaultOpen(pathname))

  useEffect(() => {
    const matched = NAV
      .filter(item =>
        item.children?.some(c => pathname === c.href || pathname.startsWith(c.href + '/'))
      )
      .map(item => item.label)
    if (matched.length > 0) {
      setOpenSections(prev => {
        const next = [...prev]
        matched.forEach(label => { if (!next.includes(label)) next.push(label) })
        return next
      })
    }
  }, [pathname])

  function toggle(label: string) {
    setOpenSections(prev =>
      prev.includes(label) ? prev.filter(l => l !== label) : [...prev, label]
    )
  }

  function isOpen(label: string) {
    return openSections.includes(label)
  }

  function isActive(href: string) {
    return pathname === href || pathname.startsWith(href + '/')
  }

  return (
    <aside className="w-[200px] flex-shrink-0 border-r border-kb-border min-h-[700px] pt-6 pr-2">
      <h2 className="text-[13px] font-bold mb-4 px-2 pb-2 border-b border-kb-border flex items-center gap-2" style={{ color: '#0D5C47' }}>대출</h2>
      <nav>
        {NAV.map((item) => (
          <div key={item.label}>
            {item.expandable ? (
              <>
                <button
                  onClick={() => toggle(item.label)}
                  className="w-full flex items-center justify-between px-2 py-2 text-[13px] text-kb-text-body hover:text-kb-text hover:bg-[#F0FAF7] transition-colors"
                >
                  <span className="text-left leading-tight">{item.label}</span>
                  <span className="text-[11px] text-kb-text-muted ml-1 flex-shrink-0 font-bold">
                    {isOpen(item.label) ? '∨' : '›'}
                  </span>
                </button>
                {isOpen(item.label) && (
                  <ul className="mb-1">
                    {item.children?.map((child) => (
                      <li key={child.href}>
                        {child.disabled ? (
                          <span className="block pl-5 pr-2 py-1.5 text-[12px] leading-snug text-kb-text-muted cursor-pointer select-none">
                            {child.label}
                          </span>
                        ) : (
                          <Link
                            href={child.href}
                            className={`block py-1.5 text-[12px] leading-snug transition-colors ${
                              isActive(child.href)
                                ? 'pl-[17px] pr-2 border-l-[3px] border-[#5BC9A8] bg-[#F0FAF7] font-semibold text-[#0D5C47]'
                                : 'pl-5 pr-2 text-kb-text-muted hover:text-kb-text hover:bg-[#F0FAF7]'
                            }`}
                          >
                            {child.label}
                          </Link>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
              </>
            ) : (
              <Link
                href={item.href ?? '#'}
                className={`block px-2 py-2 text-[13px] hover:text-kb-text transition-colors ${
                  item.href && isActive(item.href) ? 'text-kb-text font-semibold' : 'text-kb-text-muted'
                }`}
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
