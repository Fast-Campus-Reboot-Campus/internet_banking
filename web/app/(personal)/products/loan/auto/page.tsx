'use client'

import Link from 'next/link'
import { useState } from 'react'
import LoanSidebar from '@/components/inquiry/LoanSidebar'

const FILTER_TABS = [
  { label: '신용대출',             href: '/products/loan/credit' },
  { label: '담보대출',             href: '/products/loan/mortgage' },
  { label: '전월세/반환보증',       href: '/products/loan/jeonse' },
  { label: '자동차대출',           href: '/products/loan/auto' },
  { label: '집단중도금/이주비대출',  href: '/products/loan/group' },
  { label: '주택도시기금대출',      href: '/products/loan/khfc' },
]

const JOIN_METHODS = ['전체', '인터넷뱅킹', '스타뱅킹', '리브 Next', '영업점', '스마트대출']

const MOCK_PRODUCTS = [
  { id: 1, badge: '스타뱅킹', badgeColor: 'gold',  name: 'AXful 친환경 매직카대출(신차 구매用)', desc: '신차 구매 자금도 은행방문 없어!',       limit: '6천만원',       rate: '연 4.5% ~ 7.9%' },
  { id: 2, badge: '스타뱅킹', badgeColor: 'gold',  name: 'AXful 매직카대출(중고차 구매用)',      desc: '중고차 구매 자금도 은행방문 없어!',     limit: '4천만원',       rate: '연 5.0% ~ 8.5%' },
  { id: 3, badge: '스타뱅킹', badgeColor: 'gold',  name: 'AXful 전기차 특별대출',               desc: '전기차·수소차 구매 고객 우대 금리 적용', limit: '7천만원',       rate: '연 3.9% ~ 6.5%' },
  { id: 4, badge: '영업점',   badgeColor: 'olive', name: 'AXful 자동차담보대출',                desc: '보유 차량을 담보로 운전 자금 지원',     limit: '차량가액의 80%', rate: '연 5.5% ~ 9.0%' },
]

const BADGE_STYLE: Record<string, string> = {
  gold:  'bg-[#C09B3A] text-white',
  blue:  'bg-[#1A56DB] text-white',
  olive: 'bg-[#4A7C59] text-white',
  teal:  'bg-[#0D7377] text-white',
}

export default function AutoLoanPage() {
  const [joinMethod,  setJoinMethod]  = useState('전체')
  const [searchName,  setSearchName]  = useState('')
  const [sortBy,      setSortBy]      = useState('판매순')
  const [page,        setPage]        = useState(1)
  const totalPages = 2

  return (
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6">
        <nav className="text-[12px] text-kb-text-muted mb-4 flex items-center gap-1">
          <Link href="/" className="hover:underline">개인뱅킹</Link><span>›</span>
          <Link href="/products/deposit" className="hover:underline">금융상품</Link><span>›</span>
          <Link href="/products/loan" className="hover:underline">대출</Link><span>›</span>
          <span className="text-kb-text font-medium">대출 상품/신청</span><span>›</span>
          <span className="text-kb-text font-medium">자동차대출</span>
        </nav>
        <div className="flex gap-8">
          <LoanSidebar />
          <div className="flex-1 min-w-0">
            <h1 className="text-[26px] font-bold text-kb-text mb-5">자동차대출</h1>
            <div className="flex border-b mb-6" style={{ borderColor: '#E2F5EF' }}>
              {FILTER_TABS.map(tab => (
                <Link key={tab.href} href={tab.href}
                  className="px-5 py-3 text-[14px] font-medium whitespace-nowrap transition-colors border-b-2 -mb-px"
                  style={tab.href === '/products/loan/auto'
                    ? { borderColor: '#0D5C47', color: '#0D5C47', fontWeight: 700 }
                    : { borderColor: 'transparent', color: '#9CA3AF' }}>
                  {tab.label}
                </Link>
              ))}
            </div>
            <div className="rounded-xl p-5 mb-6" style={{ border: '1px solid #E2F5EF', backgroundColor: '#F8FFFE' }}>
              <div className="flex items-center gap-4 mb-4">
                <label className="w-20 text-[13px] font-medium text-kb-text flex-shrink-0">상품명</label>
                <input type="text" value={searchName} onChange={e => setSearchName(e.target.value)} placeholder="상품명을 입력하세요"
                  className="flex-1 border rounded-lg px-3 py-2 text-[13px] outline-none" style={{ borderColor: '#D1D5DB' }} />
              </div>
              <div className="flex items-center gap-4 mb-5">
                <span className="w-20 text-[13px] font-medium text-kb-text flex-shrink-0">가입방법</span>
                <div className="flex gap-5 flex-wrap">
                  {JOIN_METHODS.map(method => (
                    <label key={method} className="flex items-center gap-1.5 cursor-pointer">
                      <input type="radio" name="joinMethod" value={method} checked={joinMethod === method}
                        onChange={() => setJoinMethod(method)} style={{ accentColor: '#0D5C47' }} />
                      <span className="text-[13px] text-kb-text-body">{method}</span>
                    </label>
                  ))}
                </div>
              </div>
              <div className="flex justify-center">
                <button className="px-16 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity"
                  style={{ backgroundColor: '#0D5C47' }}>조회</button>
              </div>
            </div>
            <div className="flex items-center justify-between mb-3">
              <p className="text-[13px] text-kb-text">상품목록 <span className="font-bold" style={{ color: '#0D5C47' }}>{MOCK_PRODUCTS.length}</span>건</p>
              <select value={sortBy} onChange={e => setSortBy(e.target.value)}
                className="border rounded-lg text-[13px] px-2 py-1.5 outline-none" style={{ borderColor: '#E2F5EF' }}>
                <option>판매순</option><option>금리순</option><option>한도순</option>
              </select>
            </div>
            <div className="rounded-xl overflow-hidden" style={{ border: '1px solid #E2F5EF' }}>
              {MOCK_PRODUCTS.map((product, idx) => (
                <div key={product.id} className="py-5 px-5 flex items-center gap-5 hover:bg-[#F8FFFE] transition-colors"
                  style={{ borderBottom: idx < MOCK_PRODUCTS.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1.5">
                      <span className={`text-[11px] font-bold px-2 py-0.5 rounded ${BADGE_STYLE[product.badgeColor]}`}>{product.badge}</span>
                      <span className="text-[16px] font-bold text-kb-text">{product.name}</span>
                    </div>
                    <p className="text-[13px] text-kb-text-muted mb-2">{product.desc}</p>
                    <div className="flex items-center gap-4 text-[13px]">
                      <span className="text-kb-text-muted">최고 <span className="font-bold" style={{ color: '#0D5C47' }}>{product.limit}</span></span>
                      <span className="text-kb-text-muted">{product.rate}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <button className="border rounded-lg p-2 hover:bg-[#F0FAF7] transition-colors" style={{ borderColor: '#E2F5EF' }}>
                      <svg viewBox="0 0 20 20" fill="none" className="w-5 h-5 text-kb-text-muted" stroke="currentColor" strokeWidth="1.5">
                        <path d="M1 1h2l2.4 9.6a2 2 0 0 0 2 1.4h7.2a2 2 0 0 0 2-1.4L19 5H5"/><circle cx="8" cy="18" r="1"/><circle cx="16" cy="18" r="1"/>
                      </svg>
                    </button>
                    <Link href="#" className="px-5 py-2 text-[14px] font-bold rounded-xl border hover:bg-[#F0FAF7] transition-colors whitespace-nowrap"
                      style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>바로하기</Link>
                  </div>
                </div>
              ))}
            </div>
            <div className="flex justify-center items-center gap-1 mt-8">
              <button onClick={() => setPage(p => Math.max(1, p - 1))} className="w-8 h-8 flex items-center justify-center border rounded-lg text-kb-text-muted hover:bg-[#F0FAF7] transition-colors" style={{ borderColor: '#E2F5EF' }}>‹</button>
              {Array.from({ length: totalPages }, (_, i) => i + 1).map(n => (
                <button key={n} onClick={() => setPage(n)} className="w-8 h-8 flex items-center justify-center text-[13px] border rounded-lg transition-colors"
                  style={page === n ? { backgroundColor: '#0D5C47', borderColor: '#0D5C47', color: 'white', fontWeight: 700 } : { borderColor: '#E2F5EF', color: '#6B7280' }}>{n}</button>
              ))}
              <button onClick={() => setPage(p => Math.min(totalPages, p + 1))} className="w-8 h-8 flex items-center justify-center border rounded-lg text-kb-text-muted hover:bg-[#F0FAF7] transition-colors" style={{ borderColor: '#E2F5EF' }}>›</button>
            </div>
          </div>
        </div>
      </div>
    </main>
  )
}
