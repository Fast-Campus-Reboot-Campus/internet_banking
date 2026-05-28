'use client'

import Link from 'next/link'
import { useState } from 'react'
import { usePathname } from 'next/navigation'

type Child = { label: string; href: string }
type SidebarItem = { label: string; expandable?: boolean; href?: string; children?: Child[] }

const NAV: SidebarItem[] = [
  { label: '예금 상품/가입', href: '/products/deposit/list' },
  {
    label: '판매중지상품',
    expandable: true,
    children: [
      { label: '일반 예금상품', href: '#' },
      { label: '지수연동예금',  href: '#' },
    ],
  },
  {
    label: '예금 조회/해지',
    expandable: true,
    children: [
      { label: '신규결과/내역 조회',              href: '/products/deposit/inquiry/new' },
      { label: '현금인출카드 조회',               href: '#' },
      { label: '예금해지',                        href: '/products/deposit/inquiry/terminate' },
      { label: '분할인출',                        href: '#' },
      { label: '해지예상 조회',                   href: '#' },
      { label: '해지결과/내역 조회',              href: '/products/deposit/inquiry/terminate-result' },
      { label: '청약예·부금 이자지급 조회/이체',  href: '#' },
    ],
  },
  {
    label: '예금 관리',
    expandable: true,
    children: [
      { label: '예금전환',                              href: '/products/deposit/manage/convert' },
      { label: '통장자동전환 서비스',                    href: '#' },
      { label: '세금우대/비과세종합 저축한도 조회/변경', href: '#' },
      { label: '자동재예치(재가입) 및 통지여부 변경',   href: '#' },
      { label: '만기 자동해지 신청',                    href: '#' },
      { label: '만기해지방법 변경',                     href: '#' },
      { label: '예금잔액조회 통보',                     href: '#' },
      { label: '재형저축한도 변경',                     href: '#' },
      { label: '상품만기알림서비스 신청/해지',           href: '#' },
      { label: '비대면 예적금 해지 제한',               href: '#' },
    ],
  },
  {
    label: '예금 가이드',
    expandable: true,
    children: [
      { label: '예금금리 안내',   href: '#' },
      { label: '예금관련 수수료', href: '#' },
    ],
  },
  { label: '🔒 인증센터', href: '/cert' },
  { label: '신규상담',    href: '#' },
]

export default function DepositSidebar() {
  const pathname = usePathname()

  const defaultOpen = new Set(
    NAV.filter(item => item.children?.some(c => c.href !== '#' && pathname.startsWith(c.href)))
       .map(item => item.label)
  )
  if (defaultOpen.size === 0) defaultOpen.add('예금 조회/해지')

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
      <h2 className="text-base font-bold text-kb-text mb-3 px-1">예금</h2>
      <nav>
        {NAV.map((item) => (
          <div key={item.label}>
            {item.expandable ? (
              <>
                <button
                  onClick={() => toggle(item.label)}
                  className="w-full flex items-center justify-between px-2 py-2 text-[13px] text-kb-text-body hover:text-kb-text"
                >
                  <span className="text-left leading-tight">{item.label}</span>
                  <span className="text-[10px] text-kb-text-muted ml-1 flex-shrink-0">
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
                            className={`block px-3 py-1.5 text-[12px] leading-snug ${
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
