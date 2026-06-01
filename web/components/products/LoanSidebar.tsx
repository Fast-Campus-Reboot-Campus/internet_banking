'use client'

import Link from 'next/link'
import { useState } from 'react'
import { usePathname } from 'next/navigation'

type Child = { label: string; href: string }
type SidebarItem = { label: string; expandable?: boolean; href?: string; children?: Child[] }

const NAV: SidebarItem[] = [
  {
    label: '대출상품/신청',
    expandable: true,
    children: [
      { label: '신용대출',              href: '/products/loan/credit' },
      { label: '담보대출',              href: '#' },
      { label: '전월세/반환보증',        href: '#' },
      { label: '자동차대출',            href: '#' },
      { label: '집단중도금/이주비대출',  href: '#' },
      { label: '주택도시기금대출',       href: '#' },
      { label: '개인사업자대출',         href: '#' },
    ],
  },
  {
    label: '대출진행현황',
    expandable: true,
    children: [
      { label: '진행현황조회/실행/예약',   href: '/products/loan/status' },
      { label: '사후서류제출',             href: '#' },
      { label: '배우자정보제공동의',       href: '#' },
      { label: '세대원정보제공동의',       href: '#' },
      { label: '제3자담보정보제공동의',     href: '#' },
      { label: '부동산담보대출 전자서명',   href: '#' },
    ],
  },
  {
    label: '대출관리',
    expandable: true,
    children: [
      { label: '적용금리조회',                         href: '#' },
      { label: '이자/월부금입금',                       href: '#' },
      { label: '대출상환',                             href: '#' },
      { label: '대출계약철회 예상조회/완제',             href: '#' },
      { label: '대출한도변경/해지',                     href: '#' },
      { label: '기한연장',                             href: '#' },
      { label: '개인대출 금리인하요구권',               href: '#' },
      { label: '해지계좌조회',                         href: '#' },
      { label: '금리산정내역서 조회',                   href: '#' },
      { label: '소멸시효완성에 따른 채무면제 결과조회', href: '#' },
      { label: '통장자동대출 이자납입내역 조회',         href: '#' },
      { label: '개인대출 통지서비스 변경',               href: '#' },
      { label: '개인대출 할부금(이자) 납입방법 변경',   href: '#' },
    ],
  },
  {
    label: '대출 가이드',
    expandable: true,
    children: [
      { label: '가계대출금리',                         href: '#' },
      { label: '대출관련 수수료',                       href: '#' },
      { label: '금리인하요구권',                        href: '#' },
      { label: '대출연체시 지연배상금액 예시',           href: '#' },
      { label: '부가서비스',                           href: '#' },
      { label: '내용증명 우편미수신 주요정보 안내',      href: '#' },
      { label: '추심관련 권리의무 및 권리구제방법 안내', href: '#' },
    ],
  },
  {
    label: '신용평가 및 여신심사 자료제출',
    expandable: true,
    children: [
      { label: '「업체현황 및 사업계획서」조회 및 작성', href: '#' },
      { label: '「FATI (재무 및 세무자료)」제출',       href: '#' },
      { label: '「FATI (재무 및 세무자료)」제출내역조회', href: '#' },
    ],
  },
  { label: '🔒 인증센터', href: '/cert' },
  { label: '신규상담',    href: '#' },
]

export default function LoanSidebar() {
  const pathname = usePathname()

  const defaultOpen = new Set(
    NAV.filter(item => item.children?.some(c => c.href !== '#' && pathname.startsWith(c.href)))
       .map(item => item.label)
  )
  if (defaultOpen.size === 0) defaultOpen.add('대출상품/신청')

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
      <h2 className="text-base font-bold text-kb-text mb-3 px-1">대출</h2>
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
