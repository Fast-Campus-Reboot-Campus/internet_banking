'use client'

import Link from 'next/link'
import { useState, useRef, useEffect } from 'react'
import ConsultModal from '@/components/layout/ConsultModal'

// ── 드롭다운 데이터 ───────────────────────────────────────────

const NETWORK_ITEMS = [
  'AXful금융', 'AXful손해보험', 'AXful국민카드', 'AXful자산운용',
  'AXful캐피탈', 'AXful라이프생명', 'AXful저축은행', 'AXful인베스트먼트',
  'AXful데이터시스템', 'AXful신용정보', 'AXful경영연구소',
]

const PHONE_ITEMS = [
  { label: '대표전화', value: '1588-0000, 1599-0000, 1644-0000' },
  { label: '해외에서 국내로 걸 때', value: '+82-2-6300-0000', small: true },
  { label: '상담전화', value: '1800-0000', note: '(공휴일 및 사고신고 제외)' },
  { label: '기업전화', value: '1599-0499' },
  { label: '기업(B2B)', value: '1566-0944' },
  { label: '외국인전화', value: '1599-0477' },
  { label: '어르신전화', value: '1644-0308' },
]

const COMPARE_ITEMS = [
  { label: '손해금리비교', href: '#' },
  { label: '금융상품 한눈에', href: '#' },
  { label: '금융소비자정보포털 파인', href: '#' },
]

const FOOTER_LINKS_TOP = [
  { label: '보호금융상품등록부', bold: false },
  { label: '전자민원접수', bold: true },
  { label: '전자금융거래기본약관', bold: true },
  { label: '개인정보 처리방침', bold: true },
  { label: '신용정보활용체제', bold: true },
  { label: '위치기반서비스 이용약관', bold: true },
  { label: '경영공시', bold: true },
]

const FOOTER_LINKS_BOTTOM = [
  '이용상담', '보안프로그램', '사고신고', '그룹 내 고객정보 제공안내',
  '스튜어드십 코드', 'AXful인증서 제휴문의', 'AXful뱅킹 Ads',
]

// ── 드롭다운 컴포넌트 ────────────────────────────────────────

function Dropdown({ label, children }: { label: string; children: React.ReactNode }) {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen(v => !v)}
        className={`flex items-center gap-1.5 border px-3 py-1.5 text-[13px] bg-white transition-colors
          ${open ? 'border-kb-primary text-kb-primary' : 'border-kb-border text-kb-text-body hover:border-kb-primary'}`}
      >
        {label}
        <svg viewBox="0 0 10 6" fill="none" className={`w-2.5 h-2.5 transition-transform ${open ? 'rotate-180' : ''}`}>
          <path d="M1 1l4 4 4-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      </button>

      {open && (
        <div className="absolute bottom-full mb-1 left-0 bg-white border border-kb-border shadow-lg z-50 min-w-[200px]">
          {children}
        </div>
      )}
    </div>
  )
}

// ── 푸터 ─────────────────────────────────────────────────────

export default function Footer() {
  const [showConsult, setShowConsult] = useState(false)

  return (
    <>
    <footer className="border-t border-kb-border bg-kb-beige-light">
      <div className="max-w-kb-container mx-auto px-6 py-5">

        {/* 상단 링크 */}
        <div className="flex flex-wrap gap-x-1 gap-y-1 mb-1">
          {FOOTER_LINKS_TOP.map((link, i) => (
            <span key={link.label} className="flex items-center gap-1">
              {i > 0 && <span className="text-kb-border text-[12px]">|</span>}
              <Link href="#"
                className={`text-[12px] hover:underline text-kb-text ${link.bold ? 'font-semibold' : ''}`}>
                {link.label}
              </Link>
            </span>
          ))}
        </div>

        {/* 하단 링크 */}
        <div className="flex flex-wrap gap-x-1 gap-y-1 mb-3">
          {FOOTER_LINKS_BOTTOM.map((link, i) => (
            <span key={link} className="flex items-center gap-1">
              {i > 0 && <span className="text-kb-border text-[12px]">|</span>}
              <Link href="#" className="text-[12px] text-kb-text hover:underline">{link}</Link>
            </span>
          ))}
        </div>

        {/* 사업자 정보 */}
        <p className="text-[12px] text-kb-text-body">
          사업자 등록번호 : 000-00-00000&nbsp;&nbsp;|&nbsp;&nbsp;서울특별시 중구 태평로1길 1(AXful동)&nbsp;&nbsp;|&nbsp;&nbsp;대표 : 홍대표
        </p>
      </div>

      {/* 드롭다운 바 */}
      <div className="border-t border-kb-border bg-kb-beige-light">
        <div className="max-w-kb-container mx-auto px-6 py-3 flex items-center gap-2 flex-wrap">

          {/* AXful금융그룹네트워크 */}
          <Dropdown label="AXful금융그룹네트워크">
            <div className="py-1">
              {NETWORK_ITEMS.map(item => (
                <Link key={item} href="#"
                  className="block px-4 py-2 text-[13px] text-kb-text-body hover:bg-kb-primary-bg hover:text-kb-primary transition-colors">
                  {item}
                </Link>
              ))}
            </div>
          </Dropdown>

          {/* 대표전화 */}
          <Dropdown label="대표전화 1588-0000">
            <div className="py-2 px-4 space-y-2.5 min-w-[260px]">
              {PHONE_ITEMS.map(item => (
                <div key={item.label} className={item.small ? 'pl-2' : ''}>
                  <div className="flex items-baseline justify-between gap-4">
                    <span className={`text-[12px] text-kb-text-muted flex-shrink-0 ${item.small ? '' : 'font-semibold text-kb-text'}`}>
                      {item.label}
                    </span>
                    <span className="text-[13px] font-bold text-kb-text text-right">{item.value}</span>
                  </div>
                  {item.note && (
                    <p className="text-[11px] text-kb-text-muted">{item.note}</p>
                  )}
                </div>
              ))}
            </div>
          </Dropdown>

          {/* 챗봇/채팅/이메일상담 */}
          <button
            onClick={() => setShowConsult(true)}
            className="flex items-center gap-1.5 border border-kb-border px-3 py-1.5 text-[13px] text-kb-text-body bg-white hover:border-kb-primary hover:text-kb-primary transition-colors">
            챗봇/채팅/이메일상담(24시간)
          </button>

          {/* 비교조회서비스 */}
          <Dropdown label="비교조회서비스">
            <div className="py-1">
              {COMPARE_ITEMS.map(item => (
                <Link key={item.label} href={item.href}
                  className="block px-4 py-2 text-[13px] text-kb-text-body hover:bg-kb-primary-bg hover:text-kb-primary transition-colors">
                  {item.label}
                </Link>
              ))}
            </div>
          </Dropdown>

        </div>

        {/* 카피라이트 */}
        <div className="max-w-kb-container mx-auto px-6 pb-6">
          <p className="text-[12px] text-kb-text-muted">Copyright AXful Bank. All Rights Reserved.</p>
        </div>
      </div>
    </footer>

    {showConsult && <ConsultModal onClose={() => setShowConsult(false)} />}
    </>
  )
}
