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

const PRODUCT_TYPES = ['전체', '전세자금대출', '월세자금대출', '구입자금대출']
const JOIN_METHODS  = ['전체', '인터넷뱅킹', '스타뱅킹', '영업점']

const MOCK_PRODUCTS = [
  { id: 1, badge: '영업점',    badgeColor: 'olive', name: '내집마련디딤돌대출',          desc: '무주택 국민의 내집마련을 지원하는',                          limit: '2.5억원', rate: '연 2.35% ~ 3.95%' },
  { id: 2, badge: '영업점',    badgeColor: 'olive', name: '버팀목 전세자금대출',          desc: '무주택 국민의 주거안정을 지원하는',                          limit: '1.2억원', rate: '연 2.1% ~ 2.9%' },
  { id: 3, badge: '영업점',    badgeColor: 'olive', name: '청년 버팀목 전세자금대출',      desc: '만 19세~34세 청년 무주택자를 위한 전세자금',                limit: '2억원',   rate: '연 1.8% ~ 2.7%' },
  { id: 4, badge: '영업점',    badgeColor: 'olive', name: '신혼부부 전용 전세자금대출',    desc: '혼인 7년 이내 신혼부부 우대 전세자금대출',                  limit: '3억원',   rate: '연 1.5% ~ 2.5%' },
  { id: 5, badge: '영업점',    badgeColor: 'olive', name: '월세대출',                    desc: '저소득 청년 및 주거급여 수급자 월세 보증금 지원',            limit: '960만원', rate: '연 1.5% ~ 2.0%' },
  { id: 6, badge: '인터넷뱅킹', badgeColor: 'blue',  name: 'AXful 구입자금대출(전자)', desc: '주택도시기금을 활용한 비대면 구입자금대출',                  limit: '2억원',   rate: '연 2.7% ~ 3.5%' },
]

const BADGE_STYLE: Record<string, string> = {
  gold:  'bg-[#C09B3A] text-white',
  blue:  'bg-[#1A56DB] text-white',
  olive: 'bg-[#4A7C59] text-white',
  teal:  'bg-[#0D7377] text-white',
}

export default function KhfcLoanPage() {
  const [productType, setProductType] = useState('전체')
  const [joinMethod,  setJoinMethod]  = useState('전체')
  const [searchName,  setSearchName]  = useState('')
  const [sortBy,      setSortBy]      = useState('판매순')
  const [page,        setPage]        = useState(1)
  const totalPages = 3

  return (
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6">
        <nav className="text-[12px] text-kb-text-muted mb-4 flex items-center gap-1">
          <Link href="/personal" className="hover:underline">개인뱅킹</Link><span>›</span>
          <Link href="/products/deposit" className="hover:underline">금융상품</Link><span>›</span>
          <Link href="/products/loan" className="hover:underline">대출</Link><span>›</span>
          <span className="text-kb-text font-medium">대출상품/신청</span><span>›</span>
          <span className="text-kb-text font-medium">주택도시기금대출</span>
        </nav>
        <div className="flex gap-8">
          <LoanSidebar />
          <div className="flex-1 min-w-0">
            <h1 className="text-[26px] font-bold text-kb-text mb-5">주택도시기금대출</h1>
            <div className="flex border-b border-kb-border mb-6">
              {FILTER_TABS.map(tab => (
                <Link key={tab.href} href={tab.href}
                  className={`px-5 py-3 text-[14px] font-medium whitespace-nowrap transition-colors border-b-2 -mb-px ${tab.href === '/products/loan/khfc' ? 'border-kb-text text-kb-text font-bold' : 'border-transparent text-kb-text-muted hover:text-kb-text'}`}>
                  {tab.label}
                </Link>
              ))}
            </div>
            <div className="border border-kb-border p-5 mb-6">
              <div className="flex items-center gap-4 mb-4">
                <label className="w-20 text-[13px] font-medium text-kb-text flex-shrink-0">상품명</label>
                <input type="text" value={searchName} onChange={e => setSearchName(e.target.value)} placeholder="상품명을 입력하세요" className="flex-1 border border-kb-border px-3 py-2 text-[13px] focus:outline-none focus:border-kb-text" />
              </div>
              <div className="flex items-center gap-4 mb-4">
                <span className="w-20 text-[13px] font-medium text-kb-text flex-shrink-0">상품유형</span>
                <div className="flex gap-5 flex-wrap">
                  {PRODUCT_TYPES.map(type => (
                    <label key={type} className="flex items-center gap-1.5 cursor-pointer">
                      <input type="radio" name="productType" value={type} checked={productType === type} onChange={() => setProductType(type)} className="accent-kb-text" />
                      <span className="text-[13px] text-kb-text-body">{type}</span>
                    </label>
                  ))}
                </div>
              </div>
              <div className="flex items-center gap-4 mb-5">
                <span className="w-20 text-[13px] font-medium text-kb-text flex-shrink-0">가입방법</span>
                <div className="flex gap-5 flex-wrap">
                  {JOIN_METHODS.map(method => (
                    <label key={method} className="flex items-center gap-1.5 cursor-pointer">
                      <input type="radio" name="joinMethod" value={method} checked={joinMethod === method} onChange={() => setJoinMethod(method)} className="accent-kb-text" />
                      <span className="text-[13px] text-kb-text-body">{method}</span>
                    </label>
                  ))}
                </div>
              </div>
              <div className="flex justify-center">
                <button className="px-12 py-2.5 text-[14px] font-bold text-white" style={{ backgroundColor: '#3D3D3D' }}>조회</button>
              </div>
            </div>
            <div className="flex items-center justify-between mb-3">
              <p className="text-[13px] text-kb-text">상품목록 <span className="font-bold">{MOCK_PRODUCTS.length}</span>건</p>
              <select value={sortBy} onChange={e => setSortBy(e.target.value)} className="border border-kb-border text-[13px] px-2 py-1.5 focus:outline-none">
                <option>판매순</option><option>금리순</option><option>한도순</option>
              </select>
            </div>
            <div className="border-t border-kb-text divide-y divide-kb-border">
              {MOCK_PRODUCTS.map(product => (
                <div key={product.id} className="py-5 flex items-center gap-5">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1.5">
                      <span className={`text-[11px] font-bold px-2 py-0.5 ${BADGE_STYLE[product.badgeColor]}`}>{product.badge}</span>
                      <span className="text-[16px] font-bold text-kb-text">{product.name}</span>
                    </div>
                    <p className="text-[13px] text-kb-text-muted mb-2">{product.desc}</p>
                    <div className="flex items-center gap-4 text-[13px]">
                      <span className="text-kb-text-muted">최고 <span className="font-bold text-kb-text">{product.limit}</span></span>
                      <span className="text-kb-text-muted">{product.rate}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <button className="border border-kb-border p-2 hover:bg-kb-beige-light transition-colors">
                      <svg viewBox="0 0 20 20" fill="none" className="w-5 h-5 text-kb-text-muted" stroke="currentColor" strokeWidth="1.5">
                        <path d="M1 1h2l2.4 9.6a2 2 0 0 0 2 1.4h7.2a2 2 0 0 0 2-1.4L19 5H5"/><circle cx="8" cy="18" r="1"/><circle cx="16" cy="18" r="1"/>
                      </svg>
                    </button>
                    <Link href="#" className="px-5 py-2 text-[14px] font-bold text-kb-text bg-kb-yellow hover:bg-kb-yellow-dark transition-colors whitespace-nowrap">바로하기</Link>
                  </div>
                </div>
              ))}
            </div>
            <div className="flex justify-center items-center gap-1 mt-8">
              <button onClick={() => setPage(p => Math.max(1, p - 1))} className="w-8 h-8 flex items-center justify-center border border-kb-border text-kb-text-muted hover:bg-kb-beige-light">‹</button>
              {Array.from({ length: totalPages }, (_, i) => i + 1).map(n => (
                <button key={n} onClick={() => setPage(n)} className={`w-8 h-8 flex items-center justify-center text-[13px] border transition-colors ${page === n ? 'border-kb-text bg-kb-text text-white font-bold' : 'border-kb-border text-kb-text-muted hover:bg-kb-beige-light'}`}>{n}</button>
              ))}
              <button onClick={() => setPage(p => Math.min(totalPages, p + 1))} className="w-8 h-8 flex items-center justify-center border border-kb-border text-kb-text-muted hover:bg-kb-beige-light">›</button>
            </div>
          </div>
        </div>
      </div>
    </main>
  )
}
