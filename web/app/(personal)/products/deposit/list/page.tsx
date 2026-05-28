'use client'

import Link from 'next/link'
import { useState, useEffect } from 'react'
import CartModal from '@/components/products/CartModal'
import DepositSidebar from '@/components/products/DepositSidebar'
import { fetchDepositProducts, getDepositSlugByProductId, toDepositProductCard } from '@/lib/deposit-api'

type Product = {
  id: string
  name: string
  channel: string
  desc?: string
  period?: string
  rate?: string
  isNew?: boolean
  canApply?: boolean
}

const DEPOSIT_PRODUCTS: Product[] = [
  {
    id: 'axful-regular',
    name: 'AXful 정기예금',
    channel: '인터넷·스타뱅킹',
    desc: 'Digital AXful의 대표 정기예금',
    period: '1~36개월',
    rate: '연 2.4%~2.9%',
    canApply: true,
  },
  {
    id: 'axful-super',
    name: 'AXful 수퍼정기예금(개인)',
    channel: '영업점',
    desc: '가입 조건을 직접 설계하는',
    period: '36개월 기준',
    rate: '연 2.2%~2.3%',
    canApply: true,
  },
  {
    id: 'regular',
    name: '일반정기예금',
    channel: '영업점',
    desc: '목돈 모아 안정수익 마음든든',
    period: '36개월 기준',
    rate: '연 2.25%~2.25%',
    canApply: true,
  },
  {
    id: 'axful-youth',
    name: 'AXful 청년도약계좌',
    channel: '인터넷·스타뱅킹',
    desc: '청년의 자산형성을 응원합니다',
    period: '60개월',
    rate: '연 3.5%~6.0%',
    canApply: true,
  },
]

// 자유적금 (saving_type = FREE)
const FREE_SAVINGS_PRODUCTS: Product[] = [
  {
    id: 'axful-free',
    name: 'AXful 내맘대로적금',
    channel: '인터넷·스타뱅킹',
    desc: '누구나 쉽게 자유롭게 DIY',
    period: '36개월 기준',
    rate: '연 2.95%~3.55%',
    canApply: true,
  },
  {
    id: 'axful-dollar',
    name: 'AXful 달러자적금',
    channel: '스타뱅킹',
    desc: '달러 가치상승 응원하는 두배이율',
    period: '6개월 기준',
    rate: '연 1%~7.2%',
    isNew: true,
    canApply: true,
  },
  {
    id: 'axful-green',
    name: 'AXful 맑은하늘적금',
    channel: '인터넷·스타뱅킹',
    desc: '맑은하늘 인증코드 금리도 Up',
    period: '36개월 기준',
    rate: '연 2.85%~3.85%',
    canApply: true,
  },
  {
    id: 'axful-star-savings',
    name: 'AXful 특★한 적금',
    channel: '스타뱅킹',
    desc: '고객 모두의 높은 수익을 위한 특별한 준비',
    period: '1개월 기준',
    rate: '연 2%~6%',
    canApply: true,
  },
]

// 정기적금 (saving_type = REGULAR)
const REGULAR_SAVINGS_PRODUCTS: Product[] = [
  {
    id: 'axful-soldier',
    name: 'AXful 장병내일준비적금',
    channel: '스타뱅킹',
    desc: '국군장병 미래대비 앞날준비',
    period: '24개월 기준',
    rate: '연 5%~10.5%',
    canApply: true,
  },
  {
    id: 'axful-work',
    name: 'AXful 직장인우대적금',
    channel: '인터넷·스타뱅킹',
    desc: '급여이체 고객 우대금리 제공',
    period: '12~36개월',
    rate: '연 3.2%~4.5%',
    canApply: true,
  },
  {
    id: 'axful-dream',
    name: 'AXful 꿈적금',
    channel: '인터넷·스타뱅킹',
    desc: '목표금액 설정으로 꿈을 향해 꾸준히',
    period: '12~36개월',
    rate: '연 3.0%~4.2%',
    canApply: true,
  },
  {
    id: 'axful-together',
    name: 'AXful 함께적금',
    channel: '스타뱅킹',
    desc: '가족·연인과 함께 모으는 공동 적금',
    period: '6~24개월',
    rate: '연 2.8%~4.0%',
    isNew: true,
    canApply: true,
  },
]

const CHECKING_PRODUCTS: Product[] = [
  {
    id: 'axful-sok',
    name: 'AXful 쏙머니통장',
    channel: '영업점',
    desc: '쇼핑용 아껴 쏙머니가 쏙~',
    isNew: true,
    canApply: true,
  },
  {
    id: 'election',
    name: '당선통장',
    channel: '영업점',
    desc: '각종 공직선거 입후보자 및 입후보예정자 선거자금 관리 통장',
    isNew: true,
  },
  {
    id: 'axful-living',
    name: 'AXful 생계비계좌',
    channel: '스타뱅킹',
    desc: '생계 유지에 필요한 자금을 최대 250만원까지 보호하는 압류방지 전용통장',
    isNew: true,
  },
  {
    id: 'axful-gs',
    name: 'AXful GS Pay통장',
    channel: '스타뱅킹',
    desc: 'GS25와의 만남으로 더 풍성해진 혜택',
    isNew: true,
  },
  {
    id: 'monimo-daily',
    name: '모니모 AXful 매일이자 통장',
    channel: '영업점',
    desc: '하루만 넣어도 이자가 쌓이는',
    canApply: true,
  },
  {
    id: 'axful-moim',
    name: 'AXful 모임금고',
    channel: '스타뱅킹',
    desc: '고인 여유자금을 연 2.0%(최대 1천만원)로 불리는',
  },
  {
    id: 'axful-star-account',
    name: 'AXful 스타통장',
    channel: '스타뱅킹',
    desc: 'Digital AXful의 대표 통장',
  },
  {
    id: 'axful-wallet',
    name: 'AXful 지갑통장',
    channel: '인터넷·스타뱅킹',
    desc: '일상의 모든 지출을 한 곳에서 관리',
    isNew: true,
  },
  {
    id: 'axful-free-account',
    name: 'AXful 자유입출금통장',
    channel: '인터넷·스타뱅킹',
    desc: '언제든 자유롭게 입출금 가능한 기본 통장',
  },
  {
    id: 'axful-youth-account',
    name: 'AXful 청년우대통장',
    channel: '인터넷·스타뱅킹',
    desc: '만 19~34세 청년을 위한 우대금리 제공',
    canApply: true,
  },
]

const HOUSING_PRODUCTS: Product[] = [
  {
    id: 'housing-savings',
    name: '주택청약종합저축',
    channel: '인터넷·스타뱅킹',
    period: '24개월 기준',
    rate: '연 3.1%',
    canApply: true,
  },
  {
    id: 'youth-housing',
    name: '청년 주택드림 청약통장',
    channel: '스타뱅킹',
    period: '24개월 기준',
    rate: '연 3.1%~4.5%',
    canApply: true,
  },
]

type Tab = '예금' | '정기적금' | '자유적금' | '입출금자유' | '주택청약'
const TABS: Tab[] = ['예금', '정기적금', '자유적금', '입출금자유', '주택청약']

const DEPOSIT_PRODUCT_TYPES = ['전체', '정기예금', '지수연동예금', '시장성예금']
const JOIN_METHODS = ['전체', '인터넷뱅킹', '스타뱅킹', 'AXful Next', '영업점']
const JOIN_PERIODS = ['전체', '3개월 미만', '3-6개월 미만', '6-12개월 미만', '12-24개월 미만', '24개월 이상']

export default function DepositListPage() {
  const [tab, setTab] = useState<Tab>('예금')
  const [apiProductsMap, setApiProductsMap] = useState<Partial<Record<Tab, Product[]>>>({})

  // URL ?tab= 파라미터로 초기 탭 설정 (클라이언트 전용)
  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const raw = params.get('tab') as Tab | null
    if (raw && (TABS as readonly string[]).includes(raw)) {
      setTab(raw)
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    async function loadProducts() {
      try {
        const products = await fetchDepositProducts({ productStatus: 'SELLING' })
        if (cancelled) return

        const next: Partial<Record<Tab, Product[]>> = {
          '예금': [],
          '정기적금': [],
          '자유적금': [],
          '입출금자유': [],
          '주택청약': [],
        }

        products.forEach(product => {
          const card = toDepositProductCard(product)
          const slug = getDepositSlugByProductId(product.productId)
          if (product.productType === 'SUBSCRIPTION') {
            next['주택청약']?.push(card)
          } else if (product.productType === 'SAVINGS') {
            if (['axful-soldier', 'axful-work', 'axful-dream', 'axful-together'].includes(slug)) {
              next['정기적금']?.push(card)
            } else {
              next['자유적금']?.push(card)
            }
          } else if (product.productName.includes('통장')) {
            next['입출금자유']?.push(card)
          } else {
            next['예금']?.push(card)
          }
        })

        setApiProductsMap(next)
      } catch {
        setApiProductsMap({})
      }
    }

    loadProducts()
    return () => {
      cancelled = true
    }
  }, [])
  const [productType, setProductType] = useState('전체')
  const [joinMethod, setJoinMethod] = useState('전체')
  const [joinPeriod, setJoinPeriod] = useState('전체')
  const [searchName, setSearchName] = useState('')
  const [cartProduct, setCartProduct] = useState<string | null>(null)

  const productsMap: Record<Tab, Product[]> = {
    '예금': DEPOSIT_PRODUCTS,
    '정기적금': REGULAR_SAVINGS_PRODUCTS,
    '자유적금': FREE_SAVINGS_PRODUCTS,
    '입출금자유': CHECKING_PRODUCTS,
    '주택청약': HOUSING_PRODUCTS,
  }
  const products = (apiProductsMap[tab]?.length ? apiProductsMap[tab] : productsMap[tab])
    .filter(product => !searchName.trim() || product.name.includes(searchName.trim()))

  const showPeriodFilter = tab === '예금' || tab === '정기적금' || tab === '자유적금'
  const showProductTypeFilter = tab === '예금'
  const showHousingNote = tab === '주택청약'

  function handleTabChange(t: Tab) {
    setTab(t)
    setProductType('전체')
    setJoinMethod('전체')
    setJoinPeriod('전체')
    setSearchName('')
  }

  return (
    <>
    {cartProduct && (
      <CartModal productName={cartProduct} onClose={() => setCartProduct(null)} />
    )}
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">
        <DepositSidebar />

        {/* 본문 */}
        <main className="flex-1 pl-8 pt-4 pb-12">
          {/* 브레드크럼 */}
          <div className="flex justify-end mb-2 text-[12px] text-kb-text-muted gap-1 items-center">
            <span>개인뱅킹</span><span>&gt;</span>
            <span>금융상품</span><span>&gt;</span>
            <span>예금</span><span>&gt;</span>
            <span className="font-semibold text-kb-text">예금 상품/가입</span>
            <span>&gt;</span>
            <Link href="#" className="text-kb-blue hover:underline">도움말</Link>
          </div>

          <h1 className="text-[20px] font-bold text-kb-text mb-4">예금 상품/가입</h1>

          {/* 탭 */}
          <div className="flex border-b border-kb-border mb-5">
            {TABS.map(t => (
              <button
                key={t}
                onClick={() => handleTabChange(t)}
                className={`px-8 py-3 text-[14px] font-medium transition-colors
                  ${tab === t
                    ? 'border-b-2 border-kb-text text-kb-text bg-white -mb-px'
                    : 'text-kb-text-muted hover:text-kb-text'
                  }`}
              >
                {t}
              </button>
            ))}
          </div>

          {/* 필터 */}
          <div className="border border-kb-border p-5 mb-5 bg-kb-beige-light">
            <div className="grid grid-cols-[100px_1fr] gap-y-3 text-[13px] items-center">
              <span className="text-kb-text font-semibold">• 상품명</span>
              <input
                type="text"
                value={searchName}
                onChange={e => setSearchName(e.target.value)}
                className="border border-kb-border px-3 py-1.5 text-[13px] w-64 bg-white"
              />

              {showProductTypeFilter && (
                <>
                  <span className="text-kb-text font-semibold">• 상품유형</span>
                  <div className="flex items-center gap-5">
                    {DEPOSIT_PRODUCT_TYPES.map(v => (
                      <label key={v} className="flex items-center gap-1.5 cursor-pointer">
                        <input type="radio" name="productType" checked={productType === v}
                          onChange={() => setProductType(v)} className="accent-kb-yellow" />
                        {v}
                      </label>
                    ))}
                  </div>
                </>
              )}

              <span className="text-kb-text font-semibold">• 가입방법</span>
              <div className="flex items-center gap-5">
                {JOIN_METHODS.map(v => (
                  <label key={v} className="flex items-center gap-1.5 cursor-pointer">
                    <input type="radio" name="joinMethod" checked={joinMethod === v}
                      onChange={() => setJoinMethod(v)} className="accent-kb-yellow" />
                    {v}
                  </label>
                ))}
              </div>

              {showPeriodFilter && (
                <>
                  <span className="text-kb-text font-semibold">• 가입기간</span>
                  <div className="flex items-center gap-4 flex-wrap">
                    {JOIN_PERIODS.map(v => (
                      <label key={v} className="flex items-center gap-1.5 cursor-pointer">
                        <input type="radio" name="joinPeriod" checked={joinPeriod === v}
                          onChange={() => setJoinPeriod(v)} className="accent-kb-yellow" />
                        {v}
                      </label>
                    ))}
                  </div>
                </>
              )}
            </div>
            <div className="flex justify-center mt-4">
              <button className="bg-kb-text text-white px-12 py-2 text-[14px] font-bold hover:bg-kb-taupe">조회</button>
            </div>
          </div>

          {/* 목록 헤더 */}
          <div className="flex justify-between items-center mb-2">
            <p className="text-[13px] text-kb-text">
              상품목록 <span className="text-kb-red font-bold">{products.length}</span>건
            </p>
            <select className="border border-kb-border px-2 py-1 text-[12px]">
              <option>금리순</option>
              <option>기간순</option>
              <option>상품명순</option>
            </select>
          </div>

          {/* 상품 목록 */}
          <div className="divide-y divide-kb-border border-t border-kb-border-dark">
            {products.map(product => (
              <div key={product.id} className="py-5 hover:bg-kb-beige-light transition-colors px-2">
                <div className="flex items-center justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-[11px] border border-kb-border px-1.5 py-0.5 text-kb-text-muted">
                        {product.channel}
                      </span>
                      {product.isNew && (
                        <span className="text-[11px] bg-kb-red text-white px-1.5 py-0.5 font-bold">NEW</span>
                      )}
                    </div>
                    <Link href={`/products/deposit/${product.id}`}
                      className="text-[16px] font-bold text-kb-text hover:underline">
                      {product.name}
                    </Link>
                    {product.desc && (
                      <p className="text-[13px] text-kb-text-muted mt-0.5">{product.desc}</p>
                    )}
                    {(product.period || product.rate) && (
                      <p className="text-[13px] mt-1">
                        {product.period && <span className="text-kb-text-muted">{product.period}, </span>}
                        {product.rate && <span className="font-bold text-orange-600">{product.rate}</span>}
                      </p>
                    )}
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <button
                      onClick={() => setCartProduct(product.name)}
                      className="border border-kb-border px-3 py-1.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light"
                    >
                      🛒
                    </button>
                    <button className="border border-kb-border px-4 py-1.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                      비교하기
                    </button>
                    {product.canApply && (
                      <Link href={`/products/deposit/join/${product.id}`}
                        className="bg-kb-yellow px-5 py-1.5 text-[13px] font-bold text-kb-text hover:bg-kb-yellow-dark">
                        가입하기
                      </Link>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* 주택청약 주석 */}
          {showHousingNote && (
            <p className="text-[12px] text-kb-text-muted text-right mt-4">
              * 2026.05.25 기준, 세금공제전, 우대금리포함
            </p>
          )}

          {/* 페이지네이션 */}
          <div className="flex justify-center mt-8 gap-1">
            <button className="w-7 h-7 text-[13px] border border-kb-yellow bg-kb-yellow text-kb-text font-bold">1</button>
          </div>
        </main>
      </div>
    </div>
    </>
  )
}
