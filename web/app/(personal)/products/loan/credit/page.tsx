'use client'

import Link from 'next/link'
import { useState } from 'react'
import LoanSidebar from '@/components/inquiry/LoanSidebar'

const FILTER_TABS = [
  { label: '신용대출',          href: '/products/loan/credit' },
  { label: '담보대출',          href: '/products/loan/mortgage' },
  { label: '전월세/반환보증',    href: '/products/loan/jeonse' },
  { label: '자동차대출',        href: '/products/loan/auto' },
  { label: '집단중도금/이주비대출', href: '/products/loan/group' },
  { label: '주택도시기금대출',   href: '/products/loan/khfc' },
]

const PRODUCT_TYPES = ['전체', '직장인', '전문직', '사업자', '연금수급자']
const JOIN_METHODS  = ['전체', '인터넷뱅킹', '스타뱅킹', '리브 Next', '영업점', '스마트대출']

const MOCK_PRODUCTS = [
  {
    id: 1,
    badge: '스타뱅킹',
    badgeColor: 'gold',
    name: 'AXful 직장인 신용대출',
    desc: '재직기간 1년 이상 직장인이라면 누구나 간편하게',
    limit: '5,000만원',
    rate: '연 4.5% ~ 8.9%',
  },
  {
    id: 2,
    badge: '인터넷뱅킹',
    badgeColor: 'blue',
    name: 'AXful 비상금대출',
    desc: '소액 긴급 자금, 최대 300만원 즉시 지원',
    limit: '300만원',
    rate: '연 6.0% ~ 12.0%',
  },
  {
    id: 3,
    badge: '스타뱅킹',
    badgeColor: 'gold',
    name: 'AXful 전문직 우대대출',
    desc: '의사·변호사·회계사 등 전문직 고객 전용 우대 금리',
    limit: '1억원',
    rate: '연 3.9% ~ 6.5%',
  },
  {
    id: 4,
    badge: '영업점',
    badgeColor: 'olive',
    name: 'AXful 연금수급자 대출',
    desc: '국민연금·공무원연금 수급자를 위한 안정적인 대출',
    limit: '3,000만원',
    rate: '연 4.0% ~ 7.5%',
  },
  {
    id: 5,
    badge: '스타뱅킹',
    badgeColor: 'gold',
    name: 'AXful 직장인 마이너스통장',
    desc: '한도 내 자유롭게 쓰고 갚는 한도형 신용대출',
    limit: '3,000만원',
    rate: '연 5.0% ~ 9.5%',
  },
  {
    id: 6,
    badge: '인터넷뱅킹',
    badgeColor: 'blue',
    name: 'AXful 사업자 운전자금대출',
    desc: '개인사업자 운영 자금을 위한 신용대출',
    limit: '2억원',
    rate: '연 5.5% ~ 10.0%',
  },
  {
    id: 7,
    badge: '영업점',
    badgeColor: 'olive',
    name: 'AXful 프리미엄 신용대출',
    desc: '우수 고객 전용, 최저 금리 신용대출',
    limit: '2억원',
    rate: '연 3.5% ~ 5.9%',
  },
  {
    id: 8,
    badge: '스마트대출',
    badgeColor: 'teal',
    name: 'AXful 간편 신용대출',
    desc: '비대면으로 5분 이내 신청 완료',
    limit: '1,000만원',
    rate: '연 6.5% ~ 13.0%',
  },
]

const BADGE_STYLE: Record<string, string> = {
  gold:  'bg-[#C09B3A] text-white',
  blue:  'bg-[#1A56DB] text-white',
  olive: 'bg-[#4A7C59] text-white',
  teal:  'bg-[#0D7377] text-white',
}

export default function CreditLoanPage() {
  const [productType, setProductType] = useState('전체')
  const [joinMethod,  setJoinMethod]  = useState('전체')
  const [searchName,  setSearchName]  = useState('')
  const [sortBy,      setSortBy]      = useState('판매순')
  const [page,        setPage]        = useState(1)
  const totalPages = 8

  return (
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6">

        {/* 빵부스러기 */}
        <nav className="text-[12px] text-kb-text-muted mb-4 flex items-center gap-1">
          <Link href="/" className="hover:underline">개인뱅킹</Link>
          <span>›</span>
          <Link href="/products/deposit" className="hover:underline">금융상품</Link>
          <span>›</span>
          <Link href="/products/loan" className="hover:underline">대출</Link>
          <span>›</span>
          <span className="text-kb-text font-medium">대출 상품/신청</span>
          <span>›</span>
          <span className="text-kb-text font-medium">신용대출</span>
        </nav>

        <div className="flex gap-8">
          {/* 사이드바 */}
          <LoanSidebar />

          {/* 본문 */}
          <div className="flex-1 min-w-0">
            <h1 className="text-[26px] font-bold text-kb-text mb-5">신용대출</h1>

            {/* 대출 종류 탭 */}
            <div className="flex border-b mb-6" style={{ borderColor: '#E2F5EF' }}>
              {FILTER_TABS.map(tab => (
                <Link key={tab.href} href={tab.href}
                  className="px-5 py-3 text-[14px] font-medium whitespace-nowrap transition-colors border-b-2 -mb-px"
                  style={tab.href === '/products/loan/credit'
                    ? { borderColor: '#0D5C47', color: '#0D5C47', fontWeight: 700 }
                    : { borderColor: 'transparent', color: '#9CA3AF' }}>
                  {tab.label}
                </Link>
              ))}
            </div>

            {/* 검색 폼 */}
            <div className="rounded-xl p-5 mb-6" style={{ border: '1px solid #E2F5EF', backgroundColor: '#F8FFFE' }}>
              <div className="flex items-center gap-4 mb-4">
                <label className="w-20 text-[13px] font-medium text-kb-text flex-shrink-0">상품명</label>
                <input type="text" value={searchName} onChange={e => setSearchName(e.target.value)}
                  placeholder="상품명을 입력하세요"
                  className="flex-1 border rounded-lg px-3 py-2 text-[13px] outline-none"
                  style={{ borderColor: '#D1D5DB' }} />
              </div>
              <div className="flex items-center gap-4 mb-4">
                <span className="w-20 text-[13px] font-medium text-kb-text flex-shrink-0">상품유형</span>
                <div className="flex gap-5">
                  {PRODUCT_TYPES.map(type => (
                    <label key={type} className="flex items-center gap-1.5 cursor-pointer">
                      <input type="radio" name="productType" value={type} checked={productType === type}
                        onChange={() => setProductType(type)} style={{ accentColor: '#0D5C47' }} />
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

            {/* 결과 헤더 */}
            <div className="flex items-center justify-between mb-3">
              <p className="text-[13px] text-kb-text">
                상품목록 <span className="font-bold" style={{ color: '#0D5C47' }}>{MOCK_PRODUCTS.length}</span>건
              </p>
              <select value={sortBy} onChange={e => setSortBy(e.target.value)}
                className="border rounded-lg text-[13px] px-2 py-1.5 outline-none"
                style={{ borderColor: '#E2F5EF' }}>
                <option>판매순</option><option>금리순</option><option>한도순</option>
              </select>
            </div>

            {/* 상품 목록 */}
            <div className="rounded-xl overflow-hidden" style={{ border: '1px solid #E2F5EF' }}>
              {MOCK_PRODUCTS.map((product, idx) => (
                <div key={product.id} className="py-5 px-5 flex items-center gap-5 hover:bg-[#F8FFFE] transition-colors"
                  style={{ borderBottom: idx < MOCK_PRODUCTS.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1.5">
                      <span className={`text-[11px] font-bold px-2 py-0.5 rounded ${BADGE_STYLE[product.badgeColor]}`}>
                        {product.badge}
                      </span>
                      <Link href={`/products/loan/credit/${product.id}`}
                        className="text-[16px] font-bold text-kb-text hover:underline">
                        {product.name}
                      </Link>
                    </div>
                    <p className="text-[13px] text-kb-text-muted mb-2">{product.desc}</p>
                    <div className="flex items-center gap-4 text-[13px]">
                      <span className="text-kb-text-muted">최고 <span className="font-bold" style={{ color: '#0D5C47' }}>{product.limit}</span></span>
                      <span className="text-kb-text-muted">{product.rate}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <button className="border rounded-lg p-2 hover:bg-[#F0FAF7] transition-colors"
                      style={{ borderColor: '#E2F5EF' }}>
                      <svg viewBox="0 0 20 20" fill="none" className="w-5 h-5 text-kb-text-muted" stroke="currentColor" strokeWidth="1.5">
                        <path d="M1 1h2l2.4 9.6a2 2 0 0 0 2 1.4h7.2a2 2 0 0 0 2-1.4L19 5H5"/>
                        <circle cx="8" cy="18" r="1"/><circle cx="16" cy="18" r="1"/>
                      </svg>
                    </button>
                    <Link href="#"
                      className="px-5 py-2 text-[14px] font-bold rounded-xl border hover:bg-[#F0FAF7] transition-colors whitespace-nowrap"
                      style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
                      비교하기
                    </Link>
                  </div>
                </div>
              ))}
            </div>

            {/* 페이지네이션 */}
            <div className="flex justify-center items-center gap-1 mt-8">
              <button onClick={() => setPage(p => Math.max(1, p - 1))}
                className="w-8 h-8 flex items-center justify-center border rounded-lg text-kb-text-muted hover:bg-[#F0FAF7] transition-colors"
                style={{ borderColor: '#E2F5EF' }}>‹</button>
              {Array.from({ length: totalPages }, (_, i) => i + 1).map(n => (
                <button key={n} onClick={() => setPage(n)}
                  className="w-8 h-8 flex items-center justify-center text-[13px] border rounded-lg transition-colors"
                  style={page === n
                    ? { backgroundColor: '#0D5C47', borderColor: '#0D5C47', color: 'white', fontWeight: 700 }
                    : { borderColor: '#E2F5EF', color: '#6B7280' }}>
                  {n}
                </button>
              ))}
              <button onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                className="w-8 h-8 flex items-center justify-center border rounded-lg text-kb-text-muted hover:bg-[#F0FAF7] transition-colors"
                style={{ borderColor: '#E2F5EF' }}>›</button>
            </div>

          </div>
        </div>
      </div>
    </main>
  )
}
