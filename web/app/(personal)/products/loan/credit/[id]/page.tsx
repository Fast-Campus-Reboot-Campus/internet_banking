'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useParams } from 'next/navigation'
import LoanSidebar from '@/components/inquiry/LoanSidebar'
import CartModal from '@/components/products/CartModal'

const PRODUCTS: Record<string, {
  badge: string; badgeColor: string; subtitle: string; name: string
  period: string; repayMethod: string; limit: string; rate: string
}> = {
  '1': { badge: '스타뱅킹', badgeColor: '#C09B3A', subtitle: '재직기간 1년 이상 직장인이라면 누구나 간편하게', name: 'AXful 직장인 신용대출', period: '최대 5년', repayMethod: '원리금균등/분할상환', limit: '5,000만원', rate: '연 4.5% ~ 8.9%' },
  '2': { badge: '인터넷뱅킹', badgeColor: '#1A56DB', subtitle: '소액 긴급 자금, 최대 300만원 즉시 지원', name: 'AXful 비상금대출', period: '최대 1년', repayMethod: '만기일시상환', limit: '300만원', rate: '연 6.0% ~ 12.0%' },
  '3': { badge: '스타뱅킹', badgeColor: '#C09B3A', subtitle: '의사·변호사·회계사 등 전문직 고객 전용 우대 금리', name: 'AXful 전문직 우대대출', period: '최대 5년', repayMethod: '원리금균등/분할상환', limit: '1억원', rate: '연 3.9% ~ 6.5%' },
  '4': { badge: '영업점', badgeColor: '#4A7C59', subtitle: '국민연금·공무원연금 수급자를 위한 안정적인 대출', name: 'AXful 연금수급자 대출', period: '최대 3년', repayMethod: '원리금균등상환', limit: '3,000만원', rate: '연 4.0% ~ 7.5%' },
  '5': { badge: '스타뱅킹', badgeColor: '#C09B3A', subtitle: '한도 내 자유롭게 쓰고 갚는 한도형 신용대출', name: 'AXful 직장인 마이너스통장', period: '최대 1년(연장가능)', repayMethod: '한도대출', limit: '3,000만원', rate: '연 5.0% ~ 9.5%' },
  '6': { badge: '인터넷뱅킹', badgeColor: '#1A56DB', subtitle: '개인사업자 운영 자금을 위한 신용대출', name: 'AXful 사업자 운전자금대출', period: '최대 1년', repayMethod: '만기일시상환', limit: '2억원', rate: '연 5.5% ~ 10.0%' },
  '7': { badge: '영업점', badgeColor: '#4A7C59', subtitle: '우수 고객 전용, 최저 금리 신용대출', name: 'AXful 프리미엄 신용대출', period: '최대 5년', repayMethod: '원리금균등/분할상환', limit: '2억원', rate: '연 3.5% ~ 5.9%' },
  '8': { badge: '스마트대출', badgeColor: '#0D7377', subtitle: '비대면으로 5분 이내 신청 완료', name: 'AXful 간편 신용대출', period: '최대 1년', repayMethod: '만기일시상환', limit: '1,000만원', rate: '연 6.5% ~ 13.0%' },
}

const CALC_TABS = ['원리금균등상환', '원금균등상환', '원금만기일시상환']
const DETAIL_TABS = ['상품안내', '금리 및 이율', '이용안내', '유의사항 및 기타', '다운로드']
const AMT_BTNS  = ['+100만', '+500만', '+1000만', '+5000만', '초기화']
const YEAR_BTNS = ['+1년', '+2년', '+5년', '+10년', '초기화']
const RATE_BTNS = ['+0.1%', '+1%', '+5%', '초기화']

type CalcResult = { principal: number; interest: number; total: number }

export default function LoanCreditDetailPage() {
  const params = useParams()
  const id = (params.id as string) ?? '1'
  const product = PRODUCTS[id] ?? PRODUCTS['1']

  const [calcTab, setCalcTab] = useState(0)
  const [principal, setPrincipal] = useState('')
  const [years, setYears] = useState('')
  const [rate, setRate] = useState('')
  const [calcResult, setCalcResult] = useState<CalcResult | null>(null)
  const [focusedInput, setFocusedInput] = useState<'principal' | 'years' | 'rate' | null>(null)
  const [detailTab, setDetailTab] = useState('상품안내')
  const [cartOpen, setCartOpen] = useState(false)
  const [consultOpen, setConsultOpen] = useState(false)

  function handleAmtBtn(b: string) {
    const map: Record<string, number> = { '+100만': 100, '+500만': 500, '+1000만': 1000, '+5000만': 5000 }
    if (b === '초기화') { setPrincipal(''); return }
    const cur = parseFloat(principal.replace(/,/g, '')) || 0
    setPrincipal((cur + (map[b] ?? 0)).toLocaleString('ko-KR'))
  }
  function handleYearBtn(b: string) {
    const map: Record<string, number> = { '+1년': 1, '+2년': 2, '+5년': 5, '+10년': 10 }
    if (b === '초기화') { setYears(''); return }
    setYears(y => String((parseInt(y) || 0) + (map[b] ?? 0)))
  }
  function handleRateBtn(b: string) {
    const map: Record<string, number> = { '+0.1%': 0.1, '+1%': 1, '+5%': 5 }
    if (b === '초기화') { setRate(''); return }
    setRate(r => String(Math.round(((parseFloat(r) || 0) + (map[b] ?? 0)) * 10) / 10))
  }

  function handleCalc() {
    const p = (parseFloat(principal.replace(/,/g, '')) || 0) * 10000
    const m = (parseInt(years) || 0) * 12
    const r = (parseFloat(rate) || 0) / 100 / 12
    if (!p || !m || !r) return
    if (calcTab === 0) {
      const total = Math.round(p * r / (1 - Math.pow(1 + r, -m)))
      const interest = Math.round(p * r)
      setCalcResult({ principal: total - interest, interest, total })
    } else if (calcTab === 1) {
      const mp = Math.round(p / m)
      const interest = Math.round(p * r)
      setCalcResult({ principal: mp, interest, total: mp + interest })
    } else {
      const interest = Math.round(p * r)
      setCalcResult({ principal: 0, interest, total: interest })
    }
  }

  function fmtN(n: number) { return n.toLocaleString('ko-KR') }

  return (
    <>
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6">
        <nav className="text-[12px] text-kb-text-muted mb-4 flex items-center gap-1">
          <Link href="/personal" className="hover:underline">개인뱅킹</Link>
          <span>›</span>
          <Link href="/products/loan" className="hover:underline">금융상품</Link>
          <span>›</span>
          <Link href="/products/loan" className="hover:underline">대출</Link>
          <span>›</span>
          <Link href="/products/loan/credit" className="hover:underline">대출상품/신청</Link>
          <span>›</span>
          <Link href="/products/loan/credit" className="hover:underline">신용대출</Link>
        </nav>

        <div className="flex gap-8">
          <LoanSidebar />

          <div className="flex-1 min-w-0">
            {/* 상품 헤더 */}
            <div className="border border-kb-border p-6">
              <span className="inline-block text-[11px] font-bold px-2 py-0.5 text-white mb-3"
                style={{ backgroundColor: product.badgeColor }}>
                {product.badge}
              </span>
              <p className="text-[13px] text-kb-text-muted mb-1">{product.subtitle}</p>
              <h1 className="text-[24px] font-bold text-kb-text mb-6">{product.name}</h1>

              {/* 핵심 정보 */}
              <div className="flex gap-10 mb-6">
                {[
                  { icon: (
                    <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="white" strokeWidth="1.8">
                      <rect x="3" y="4" width="18" height="17" rx="2"/>
                      <line x1="3" y1="9" x2="21" y2="9"/>
                      <line x1="8" y1="2" x2="8" y2="6"/>
                      <line x1="16" y1="2" x2="16" y2="6"/>
                    </svg>
                  ), color: '#4A90D9', label: '기간', value: product.period },
                  { icon: (
                    <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="white" strokeWidth="1.8">
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-4H9v-2h2V9h2v2h2v2h-2v4z"/>
                    </svg>
                  ), color: '#4A90D9', label: '상환방법', value: product.repayMethod },
                  { icon: (
                    <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="white" strokeWidth="2">
                      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                    </svg>
                  ), color: '#E05C5C', label: '최고', value: product.limit },
                ].map(item => (
                  <div key={item.label} className="flex items-center gap-2">
                    <div className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0"
                      style={{ backgroundColor: item.color }}>
                      {item.icon}
                    </div>
                    <div>
                      <p className="text-[11px] text-kb-text-muted">{item.label}</p>
                      <p className="text-[15px] font-bold text-kb-text">{item.value}</p>
                    </div>
                  </div>
                ))}
              </div>

              {/* 액션 버튼 */}
              <div className="flex items-center gap-2 mb-3">
                <Link href="/loans/apply"
                  className="px-5 py-2.5 text-[14px] font-bold text-kb-text bg-kb-yellow hover:bg-kb-yellow-dark transition-colors whitespace-nowrap">
                  AXful 스타뱅킹에서 가입
                </Link>
                <button onClick={() => setCartOpen(true)} className="px-4 py-2.5 text-[13px] border border-kb-border text-kb-text-body hover:bg-kb-beige-light transition-colors">장바구니</button>
                <button onClick={() => setConsultOpen(true)} className="px-4 py-2.5 text-[13px] border border-kb-border text-kb-text-body hover:bg-kb-beige-light transition-colors">상담신청</button>
                <Link href="/support/consultation/branch" className="px-4 py-2.5 text-[13px] border border-kb-border text-kb-text-body hover:bg-kb-beige-light transition-colors">영업점 방문예약</Link>
              </div>
              <p className="text-[12px] text-kb-text-muted">※ 자세한 내용은 아래 상품안내를 참조하시기 바랍니다.</p>
            </div>

            {/* 대출 계산기 */}
            <div className="border border-t-0 border-kb-border p-5 mb-3">
              <p className="text-[13px] font-bold text-kb-text mb-3">대출 계산기</p>
              <div className="flex border-b border-kb-border mb-4">
                {CALC_TABS.map((tab, i) => (
                  <button key={tab}
                    onClick={() => { setCalcTab(i); setCalcResult(null) }}
                    className={`px-5 py-2 text-[13px] border-b-2 -mb-px transition-colors ${
                      calcTab === i ? 'border-kb-text text-kb-text font-bold' : 'border-transparent text-kb-text-muted hover:text-kb-text'
                    }`}>
                    {tab}
                  </button>
                ))}
              </div>
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-[13px] text-kb-text-muted font-bold">대출금액</span>
                <input type="text" value={principal} onChange={e => setPrincipal(e.target.value)} placeholder="금액"
                  onFocus={() => setFocusedInput('principal')}
                  onBlur={() => setFocusedInput(null)}
                  className={`border px-3 py-1.5 w-24 outline-none text-right text-[13px] transition-colors ${focusedInput === 'principal' ? 'border-[#C09B3A] bg-[#C09B3A]/10' : 'border-kb-border'}`} />
                <span className="text-[13px] text-kb-text">만원을</span>
                <span className="text-[13px] text-kb-text-muted font-bold ml-2">기간</span>
                <input type="text" value={years} onChange={e => setYears(e.target.value)} placeholder="기간"
                  onFocus={() => setFocusedInput('years')}
                  onBlur={() => setFocusedInput(null)}
                  className={`border px-3 py-1.5 w-16 outline-none text-right text-[13px] transition-colors ${focusedInput === 'years' ? 'border-[#C09B3A] bg-[#C09B3A]/10' : 'border-kb-border'}`} />
                <span className="text-[13px] text-kb-text">년 동안</span>
                <span className="text-[13px] text-kb-text-muted font-bold ml-2">이자</span>
                <input type="text" value={rate} onChange={e => setRate(e.target.value)} placeholder="금리"
                  onFocus={() => setFocusedInput('rate')}
                  onBlur={() => setFocusedInput(null)}
                  className={`border px-3 py-1.5 w-16 outline-none text-right text-[13px] transition-colors ${focusedInput === 'rate' ? 'border-[#C09B3A] bg-[#C09B3A]/10' : 'border-kb-border'}`} />
                <span className="text-[13px] text-kb-text">%로 대출 받으면?</span>
                <button onClick={handleCalc}
                  className="ml-auto text-white text-[12px] font-bold px-5 py-1.5 hover:opacity-90 transition-opacity"
                  style={{ backgroundColor: '#5A504A' }}>
                  결과보기
                </button>
              </div>
              {/* 간편입력 버튼 */}
              <div className="mt-2 min-h-[26px]" onMouseDown={e => e.preventDefault()}>
                {focusedInput === 'principal' && (
                  <div className="flex gap-1 flex-wrap">
                    {AMT_BTNS.map(b => (
                      <button key={b} onClick={() => handleAmtBtn(b)}
                        className="px-2.5 py-1 text-[11px] border border-kb-border text-kb-text-body hover:bg-kb-beige-light transition-colors">
                        {b}
                      </button>
                    ))}
                  </div>
                )}
                {focusedInput === 'years' && (
                  <div className="flex gap-1 flex-wrap">
                    {YEAR_BTNS.map(b => (
                      <button key={b} onClick={() => handleYearBtn(b)}
                        className="px-2.5 py-1 text-[11px] border border-kb-border text-kb-text-body hover:bg-kb-beige-light transition-colors">
                        {b}
                      </button>
                    ))}
                  </div>
                )}
                {focusedInput === 'rate' && (
                  <div className="flex gap-1 flex-wrap">
                    {RATE_BTNS.map(b => (
                      <button key={b} onClick={() => handleRateBtn(b)}
                        className="px-2.5 py-1 text-[11px] border border-kb-border text-kb-text-body hover:bg-kb-beige-light transition-colors">
                        {b}
                      </button>
                    ))}
                  </div>
                )}
              </div>
              {calcResult && (
                <div className="mt-4">
                  <p className="text-[14px] text-kb-text mb-2">
                    {CALC_TABS[calcTab]} 기준 매월 약{' '}
                    <span className="font-bold text-[#C05050]">
                      {calcTab === 2 ? fmtN(calcResult.total) : fmtN(calcResult.total)}원
                    </span>
                    {calcTab === 2 ? '씩 이자를 납부하시면 됩니다.' : '씩 상환하시면 됩니다.'}
                  </p>
                </div>
              )}
              {calcResult && (
                <div className="mt-1 border border-kb-border">
                  <table className="w-full text-[13px]">
                    <thead>
                      <tr className="bg-[#F5F0E8]">
                        {calcTab === 2
                          ? <><th className="py-2 px-4 text-left font-bold text-kb-text">월 이자</th><th className="py-2 px-4 text-left font-bold text-kb-text">만기 상환원금</th></>
                          : <><th className="py-2 px-4 text-left font-bold text-kb-text">월 상환원금</th><th className="py-2 px-4 text-left font-bold text-kb-text">월 이자</th><th className="py-2 px-4 text-left font-bold text-kb-text">월 납부금액</th></>
                        }
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        {calcTab === 2
                          ? <>
                              <td className="py-3 px-4 text-kb-text">{fmtN(calcResult.total)}원</td>
                              <td className="py-3 px-4 font-bold text-[#C05050]">{fmtN((parseFloat(principal.replace(/,/g,''))||0)*10000)}원</td>
                            </>
                          : <>
                              <td className="py-3 px-4 text-kb-text">{fmtN(calcResult.principal)}원</td>
                              <td className="py-3 px-4 text-kb-text">{fmtN(calcResult.interest)}원</td>
                              <td className="py-3 px-4 font-bold text-[#C05050]">{fmtN(calcResult.total)}원</td>
                            </>
                        }
                      </tr>
                    </tbody>
                  </table>
                </div>
              )}
              <p className="text-[11px] text-kb-text-muted mt-2">※ 원하시는 정보를 입력하신 후, 예상계산결과를 확인하세요.</p>
            </div>

            {/* 공유 버튼 */}
            <div className="flex justify-end gap-1.5 mb-4">
              {['f', 't', 'k', 'n'].map(s => (
                <button key={s} className="w-7 h-7 rounded-full border border-kb-border text-[11px] text-kb-text-muted hover:bg-kb-beige-light">{s}</button>
              ))}
              <button className="text-[12px] text-kb-text-muted border border-kb-border px-2 py-0.5 hover:bg-kb-beige-light">✉ 추천메일</button>
            </div>

            {/* 상세 탭 */}
            <div className="border border-kb-border">
              <div className="flex border-b border-kb-border">
                {DETAIL_TABS.map(tab => (
                  <button key={tab} onClick={() => setDetailTab(tab)}
                    className={`flex-1 py-3 text-[13px] transition-colors ${
                      detailTab === tab
                        ? 'bg-kb-yellow font-bold text-kb-text'
                        : 'text-kb-text-muted hover:text-kb-text hover:bg-kb-beige-light'
                    }`}>
                    {tab}
                  </button>
                ))}
              </div>

              <div className="p-6">
                {detailTab === '상품안내' && (
                  <table className="w-full text-[13px]">
                    <tbody className="divide-y divide-kb-border">
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text w-40 align-top">상품특징</td>
                        <td className="py-4 text-kb-text-body">
                          AXful 은행에서 재직기간 1년 이상 직장인을 대상으로 신용도 및 소득에 따라 대출한도 및 대출금리를 결정하는 신용대출
                        </td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">대출신청자격</td>
                        <td className="py-4 text-kb-text-body">재직기간 1년 이상의 직장인(정규직, 계약직 포함)</td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">대출금액</td>
                        <td className="py-4 text-kb-text-body">
                          최고 {product.limit}<br />
                          <span className="text-kb-text-muted text-[12px]">※ 대출한도는 신용평가결과에 따라 차등 적용됩니다.</span>
                        </td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">대출기간 및 상환 방법</td>
                        <td className="py-4 text-kb-text-body">
                          {product.period}<br />
                          ☞ {product.repayMethod}
                        </td>
                      </tr>
                    </tbody>
                  </table>
                )}

                {detailTab === '금리 및 이율' && (
                  <table className="w-full text-[13px]">
                    <tbody className="divide-y divide-kb-border">
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text w-40 align-top">대출금리</td>
                        <td className="py-4 text-kb-text-body">
                          * 기준금리에 가산금리를 더하여 적용합니다.<br />
                          ☞ {product.rate} (2026.05.25 기준, 우대금리 포함)
                        </td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">중도상환수수료</td>
                        <td className="py-4 text-kb-text-body">
                          * 중도상환수수료 = 중도상환금액 × 수수료율(0.8%) × 잔존일수 ÷ 대출기간<br />
                          단, 고정금리기간 3년 이상이거나 금리변동주기와 대출기간이 동일한 경우 수수료율(0.5%) 적용
                        </td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">연체이자(지연배상금)에 관한 사항</td>
                        <td className="py-4 text-kb-text-body leading-relaxed">
                          ① 연체이자율: 최고 연 15% (차주별 대출이자율 + 연체가산이자율)<br />
                          단, 대출이자율이 최고 연체이자율 이상인 경우 대출이자율 + 연 2.0%p<br />
                          ☞ &ldquo;연체가산이자율&rdquo;은 연 3.0%를 적용합니다.
                        </td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">금리인하요구권 대상여부</td>
                        <td className="py-4 text-kb-text-body">본 상품은 금리인하요구권 신청대상입니다.</td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">대출계약철회권</td>
                        <td className="py-4 text-kb-text-body">
                          * 계약서류 수령일, 계약 체결일, 대출금 수령일 중 나중에 발생한 날부터 14일(기간의 말일이 휴일인 경우 다음 영업일)까지 은행에 서면, 전화, 컴퓨터 통신으로 철회의사를 표시하고 원금, 이자 및 부대비용을 전액 반환한 경우 대출계약을 철회할 수 있습니다.
                        </td>
                      </tr>
                    </tbody>
                  </table>
                )}

                {detailTab === '이용안내' && (
                  <table className="w-full text-[13px]">
                    <tbody className="divide-y divide-kb-border">
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text w-40 align-top">담보</td>
                        <td className="py-4 text-kb-text-body">무보증</td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">부대비용</td>
                        <td className="py-4 text-kb-text-body">
                          * 대출 신규 시 고객이 부담해야 하는 비용은 다음과 같습니다.<br />
                          (1) 인지세: 「인지세법」에 의해 대출약정 체결을 할 때 납부하는 세금으로 대출금액에 따라 세액이 차등 적용되며, 각 50%씩 고객과 은행이 부담
                          <div className="mt-2 border border-kb-border text-[12px] overflow-x-auto">
                            <table className="w-full">
                              <thead className="bg-[#F5F0E8]">
                                <tr>
                                  <th className="py-2 px-3 text-center border-r border-kb-border">대출금액</th>
                                  <th className="py-2 px-3 text-center border-r border-kb-border">5천만원 이하</th>
                                  <th className="py-2 px-3 text-center border-r border-kb-border">5천만원 초과~1억원 이하</th>
                                  <th className="py-2 px-3 text-center border-r border-kb-border">1억원 초과~10억원 이하</th>
                                  <th className="py-2 px-3 text-center">10억원 초과</th>
                                </tr>
                              </thead>
                              <tbody>
                                <tr>
                                  <td className="py-2 px-3 text-center border-r border-kb-border font-medium">인지세액</td>
                                  <td className="py-2 px-3 text-center border-r border-kb-border">비과세</td>
                                  <td className="py-2 px-3 text-center border-r border-kb-border">7만원<br />(각각 3만5천원)</td>
                                  <td className="py-2 px-3 text-center border-r border-kb-border">15만원<br />(각각 7만5천원)</td>
                                  <td className="py-2 px-3 text-center">35만원<br />(각각 17만5천원)</td>
                                </tr>
                              </tbody>
                            </table>
                          </div>
                          <p className="mt-1 text-kb-text-muted text-[12px]">* 대출 이용 중 또는 상환 시 고객이 부담해야 하는 비용은 없습니다.</p>
                        </td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">대출상환 관련 안내</td>
                        <td className="py-4 text-kb-text-body leading-relaxed">
                          * 이자 계산 방법: 이자는 원금에 소정이자율과 기간을 곱한 후 약정이자율이 연리에 의한 경우 일단위는 365(윤년은 366), 월 단위는 12로 나누어 계산합니다.<br /><br />
                          * 원금 및 이자의 상환시기<br />
                          (1) 원리금균등상환: 매월 이자지급일에 동일한 원리금을 납부합니다.<br />
                          (2) 원금균등 분할상환: 매월 이자지급일에 동일한 할부금을 상환합니다.<br />
                          (3) 만기일시상환: 대출기간 중에는 이자만 납부하고, 대출기간 만료일에 대출원금을 전액 상환합니다.
                        </td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">기한연장 관련 안내</td>
                        <td className="py-4 text-kb-text-body">
                          * 일시상환방식 대출의 기한연장은 대출만기일 1개월 이전부터 가능하며, 만기일 전까지 영업점을 방문하셔서 필요한 절차(기한연장, 재대출, 대출상환 등)를 진행하셔야 대출금에 대한 연체이자 발생 등 불이익이 발생하지 않습니다.<br />
                          ※ 분할상환을 선택하는 경우 기한연장은 불가합니다.
                        </td>
                      </tr>
                    </tbody>
                  </table>
                )}

                {detailTab === '유의사항 및 기타' && (
                  <table className="w-full text-[13px]">
                    <tbody className="divide-y divide-kb-border">
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text w-40 align-top">만기경과 후 기한의 이익 상실에 대한 안내</td>
                        <td className="py-4 text-kb-text-body leading-relaxed">
                          * 만기일 경과 후 대출금액을 전액 상환하지 않거나 기한 연장하지 않는 경우, 은행여신거래기본약관에 따라 기한의 이익이 상실되어 대출잔액에 대한 지연배상금이 부과됩니다.<br />
                          * 연체가 계속되는 경우, 연체기간에 따라 「신용정보의 이용 및 보호에 관한 법률」과 「일반신용정보관리규약」에 따라 &ldquo;연체정보 등&rdquo;으로 등록되어 금융 불이익을 받을 수 있음.
                        </td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">본 공시내용의 유효기간</td>
                        <td className="py-4 text-kb-text-body">2026.01.01 ~ 2027.11.30</td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">고객에서 알아두어야 할 사항</td>
                        <td className="py-4 text-kb-text-body leading-relaxed">
                          * 대출신청인이 신용도단단정보 등록자(신용회복지원 또는 배드뱅크포함)이거나 은행의 신용평가 신용등급이 낮은 고객의 경우 대출이 제한될 수 있습니다.<br />
                          * 상환능력에 비해 대출금, 신용카드 사용액이 과도할 경우 개인신용평점 하락할 수 있으며, 개인신용평점 하락은 금융거래와 관련된 불이익이 발생할 수 있습니다.
                        </td>
                      </tr>
                      <tr>
                        <td className="py-4 pr-6 font-bold text-kb-text align-top">필요서류</td>
                        <td className="py-4 text-kb-text-body">
                          * 본인신분증 (주민등록증 등 본인확인증표)<br />
                          * 재직확인서류 (재직증명서 등)<br />
                          * 소득확인서류 (근로소득원천징수영수증 등)
                        </td>
                      </tr>
                    </tbody>
                  </table>
                )}

                {detailTab === '다운로드' && (
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      '주가약정서(고객 신용대출의 사후 용도관리 강화 관련 주가약정용)',
                      '대출이동시스템을 통한 대환대출서비스 이용약관',
                      '가계대출상품설명서',
                      '은행여신거래기본약관(가계용)',
                      '대출거래약정서(가계용)',
                    ].map(doc => (
                      <button key={doc}
                        className="flex items-center gap-2 border border-kb-border px-4 py-3 text-[13px] text-kb-text-body hover:bg-kb-beige-light text-left transition-colors">
                        <svg viewBox="0 0 20 20" fill="none" className="w-5 h-5 flex-shrink-0 text-kb-text-muted" stroke="currentColor" strokeWidth="1.5">
                          <path d="M13 2H6a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V7l-3-5z"/>
                          <polyline points="13 2 13 7 18 7"/>
                          <line x1="10" y1="11" x2="10" y2="16"/>
                          <polyline points="7 14 10 17 13 14"/>
                        </svg>
                        <span>{doc}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* 하단 버튼 */}
            <div className="flex justify-center gap-3 mt-6">
              <Link href="/products/loan/credit"
                className="px-10 py-2.5 border border-kb-border text-[14px] text-kb-text-body hover:bg-kb-beige-light transition-colors">
                목록
              </Link>
              <button onClick={() => window.print()}
                className="px-10 py-2.5 border border-kb-border text-[14px] text-kb-text-body hover:bg-kb-beige-light transition-colors">
                인쇄
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>

    {/* 상담신청 모달 */}
    {consultOpen && (
      <div className="fixed inset-0 z-50 flex items-center justify-center">
        <div className="absolute inset-0 bg-black/50" onClick={() => setConsultOpen(false)} />
        <div className="relative bg-white w-[760px] shadow-lg">
          {/* 헤더 */}
          <div className="bg-kb-yellow px-5 py-3 flex items-center justify-between">
            <span className="text-[15px] font-bold text-kb-text">상담신청</span>
            <span className="text-[13px] font-bold text-kb-text flex items-center gap-1">
              <svg viewBox="0 0 20 20" fill="none" className="w-4 h-4" stroke="currentColor" strokeWidth="2">
                <path d="M10 2L3 7v6c0 4 2.5 7 7 8 4.5-1 7-4 7-8V7L10 2z" fill="#1A1A1A" stroke="none" />
              </svg>
              AX풀뱅크
            </span>
          </div>
          {/* 카드 영역 */}
          <div className="p-6 grid grid-cols-3 gap-4">
            {/* 전화상담 */}
            <div className="border border-kb-border p-4 bg-[#FAFAF8]">
              <p className="text-[13px] font-bold text-kb-text mb-3 flex items-center gap-1.5">
                <span>☎</span> 전화상담
              </p>
              <p className="text-[15px] font-bold text-[#1A56DB] mb-1">1588-9999</p>
              <p className="text-[11px] text-kb-text-muted mb-0.5">09:00~16:00 (은행휴무일 제외)</p>
              <p className="text-[11px] text-kb-text-muted mb-3">* 펀드/신탁 상담시간(09:00~18:00)</p>
              <p className="text-[12px] text-[#1A56DB] font-bold leading-snug mb-3">
                상품이 어렵게 느껴 지시나요?<br />
                전문상담직원이 상품관련<br />
                궁금증을 해결해드립니다.
              </p>
              <p className="text-[11px] text-kb-text-muted leading-relaxed">
                * 예금/대출/펀드/신탁 이외의 문의사항은 상담에 제한이 있으므로 1588-9999로 이용해주시기 바랍니다.<br />
                * 개인정보 보호관련으로 고객님의 계좌번호를 미리 준비해주시면 보다 신속하게 상담을 도와드릴 수 있습니다.
              </p>
            </div>
            {/* 채팅상담 */}
            <div className="border border-kb-border p-4 bg-[#FAFAF8]">
              <p className="text-[13px] font-bold text-kb-text mb-3 flex items-center gap-1.5">
                <span>💬</span> 채팅상담
              </p>
              <p className="text-[15px] font-bold text-[#1A56DB] mb-1">24시간 365일</p>
              <p className="text-[12px] text-kb-text-muted mb-4">언제든지 신청가능</p>
              <p className="text-[12px] text-kb-text-body mb-4">상담직원과 실시간 채팅상담을 하실 수 있습니다.</p>
              <button className="border border-kb-border px-4 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light transition-colors">
                채팅상담하기
              </button>
            </div>
            {/* 이메일상담 */}
            <div className="border border-kb-border p-4 bg-[#FAFAF8]">
              <p className="text-[13px] font-bold text-kb-text mb-3 flex items-center gap-1.5">
                <span>✉</span> 이메일상담
              </p>
              <p className="text-[15px] font-bold text-[#1A56DB] mb-1">24시간 365일</p>
              <p className="text-[12px] text-kb-text-muted mb-4">언제든지 신청가능</p>
              <p className="text-[12px] text-kb-text-body mb-4">문의하신 내용은 이메일로 답변드립니다.</p>
              <div className="flex flex-col gap-2">
                <button className="border border-kb-border px-4 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light transition-colors">
                  고객상담 FAQ
                </button>
                <button className="border border-kb-border px-4 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light transition-colors">
                  이메일상담하기
                </button>
              </div>
            </div>
          </div>
          {/* 닫기 */}
          <div className="flex justify-end px-6 pb-4">
            <button
              onClick={() => setConsultOpen(false)}
              className="text-[12px] text-kb-text-muted hover:text-kb-text transition-colors"
            >
              닫기 ×
            </button>
          </div>
        </div>
      </div>
    )}

    {cartOpen && <CartModal productName={product.name} onClose={() => setCartOpen(false)} />}
    </>
  )
}
