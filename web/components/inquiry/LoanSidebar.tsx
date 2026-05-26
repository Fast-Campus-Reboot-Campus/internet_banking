'use client'

import Link from 'next/link'
import { useState, useEffect } from 'react'
import { usePathname } from 'next/navigation'

type Child = { label: string; href: string; disabled?: boolean }
type SidebarItem = { label: string; expandable?: boolean; href?: string; children?: Child[] }

const NAV: SidebarItem[] = [
  {
    label: '대출상품/신청',
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
      { label: '사후서류제출',           href: '/products/loan/status/docs' },
      { label: '배우자정보제공동의',     href: '/products/loan/status/spouse',     disabled: true },
      { label: '세대원정보제공동의',     href: '/products/loan/status/household',  disabled: true },
      { label: '제3자담보정보제공동의',   href: '/products/loan/status/collateral', disabled: true },
      { label: '부동산담보대출 전자서명', href: '/products/loan/status/sign' },
    ],
  },
  {
    label: '대출관리',
    expandable: true,
    children: [
      { label: '적용금리조회',                         href: '/products/loan/manage/rate' },
      { label: '이자/월부금입금',                       href: '/products/loan/manage/payment' },
      { label: '대출금상환',                            href: '/products/loan/manage/repay' },
      { label: '대출계약철회 예상조회/완제',             href: '/products/loan/manage/withdraw' },
      { label: '대출한도변경/해지',                     href: '/products/loan/manage/limit' },
      { label: '기한연장',                             href: '/products/loan/manage/extend' },
      { label: '개인대출 금리인하요구권',               href: '/products/loan/manage/rate-cut' },
      { label: '해지계좌조회',                         href: '/products/loan/manage/closed' },
      { label: '금리산정내역서 조회',                   href: '/products/loan/manage/rate-detail' },
      { label: '소멸시효완성에 따른 채무면제 결과조회', href: '/products/loan/manage/debt-relief' },
      { label: '통장자동대출 이자납입내역 조회',         href: '/products/loan/manage/auto-interest' },
      { label: '개인대출 통지서비스 변경',               href: '/products/loan/manage/notify' },
      { label: '개인대출 할부금(이자) 납입방법 변경',   href: '/products/loan/manage/payment-method' },
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
      { label: '내용증명 우편미수신 주요정보 안내',      href: '/products/loan/guide/notice' },
      { label: '추심관련 권리의무 및 권리구제방법 안내', href: '/products/loan/guide/rights' },
    ],
  },
  {
    label: '신용평가 및 여신심사 자료제출',
    expandable: true,
    children: [
      { label: '「업체현황 및 사업계획서」조회 및 작성',   href: '/products/loan/credit-eval/biz-plan' },
      { label: '「FATI (재무 및 세무자료)」제출',         href: '/products/loan/credit-eval/fati-submit' },
      { label: '「FATI (재무 및 세무자료)」제출내역조회', href: '/products/loan/credit-eval/fati-history' },
    ],
  },
  { label: '🔒 인증센터', href: '/cert' },
]

function getDefaultOpen(pathname: string): string[] {
  const matched = NAV
    .filter(item =>
      item.children?.some(c => pathname === c.href || pathname.startsWith(c.href + '/'))
    )
    .map(item => item.label)
  return matched.length > 0 ? matched : ['대출상품/신청']
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
    <aside className="w-[180px] flex-shrink-0 border-r border-kb-border min-h-[700px] pt-6 pr-2">
      <h2 className="text-base font-bold text-kb-text mb-3 px-1">대출</h2>
      <nav>
        {NAV.map((item) => (
          <div key={item.label}>
            {item.expandable ? (
              <>
                <button
                  onClick={() => toggle(item.label)}
                  className="w-full flex items-center justify-between px-2 py-2 text-[13px] text-kb-text-body hover:text-kb-text hover:bg-kb-beige-light transition-colors"
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
                            className={`block pl-5 pr-2 py-1.5 text-[12px] leading-snug transition-colors ${
                              isActive(child.href)
                                ? 'bg-kb-yellow font-semibold text-kb-text'
                                : 'text-kb-text-muted hover:text-kb-text hover:bg-kb-beige-light'
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
