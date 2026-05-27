'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'

const LOAN_PRODUCTS = [
  { id: 'salary', name: 'AXful 직장인든든 대출', rate: '연 4.5~8.9%', maxAmount: 30_000 },
  { id: 'credit', name: 'AXful 신용대출', rate: '연 5.2~12.5%', maxAmount: 10_000 },
  { id: 'charter', name: 'AXful 전세자금 대출', rate: '연 3.8~6.2%', maxAmount: 50_000 },
  { id: 'housing', name: 'AXful 주택담보 대출', rate: '연 3.2~5.8%', maxAmount: 200_000 },
]

const PURPOSES = ['생활비', '교육비', '의료비', '주택구입', '전세자금', '사업자금', '부채상환', '기타']
const EMPLOYMENT_TYPES = ['직장인(정규직)', '직장인(계약직)', '자영업자', '공무원/교직원', '전문직', '무직/기타']
const PERIODS = [6, 12, 24, 36, 48, 60]

const STEPS = ['대출상품 선택', '신청정보 입력', '결과 확인']

export default function LoanApplyPage() {
  const router = useRouter()
  const [selectedProduct, setSelectedProduct] = useState('')
  const [amount, setAmount] = useState('')
  const [period, setPeriod] = useState(12)
  const [purpose, setPurpose] = useState('')
  const [employmentType, setEmploymentType] = useState('')
  const [annualIncome, setAnnualIncome] = useState('')
  const [agreed, setAgreed] = useState(false)

  function handleAmountQuick(v: number) {
    setAmount(String(v * 10000))
  }

  function formatAmountDisplay(raw: string) {
    const n = parseInt(raw.replace(/,/g, ''), 10)
    if (isNaN(n)) return ''
    return n.toLocaleString('ko-KR')
  }

  function handleSubmit() {
    if (!selectedProduct || !amount || !purpose || !employmentType || !agreed) return
    const product = LOAN_PRODUCTS.find(p => p.id === selectedProduct)!
    const params = new URLSearchParams({
      product: product.name,
      rate: product.rate,
      amount,
      period: String(period),
      purpose,
    })
    router.push(`/loans/apply/result?${params.toString()}`)
  }

  const canSubmit = selectedProduct && amount && purpose && employmentType && agreed

  return (
    <div className="max-w-kb-container mx-auto px-6 py-4 pb-16">
      {/* 브레드크럼 */}
      <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
        <span>개인뱅킹</span><span>&gt;</span>
        <span>대출</span><span>&gt;</span>
        <span className="font-semibold text-kb-text">대출 신청</span>
      </div>

      <h1 className="text-[22px] font-bold text-kb-text mb-6 pb-2 border-b-2 border-kb-text">대출 신청</h1>

      {/* 스텝 */}
      <div className="flex items-center justify-center gap-0 mb-8">
        {STEPS.map((step, i) => (
          <div key={step} className="flex items-center">
            <div className="flex flex-col items-center">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-[13px] font-bold
                ${i === 0 ? 'bg-kb-yellow text-kb-text' : 'border border-kb-border text-kb-text-muted bg-white'}`}>
                {i + 1}
              </div>
              <span className={`text-[11px] mt-1 font-medium ${i === 0 ? 'text-kb-text' : 'text-kb-text-muted'}`}>
                {step}
              </span>
            </div>
            {i < STEPS.length - 1 && <div className="w-16 h-px bg-kb-border mx-2 mb-4" />}
          </div>
        ))}
      </div>

      {/* 대출 상품 선택 */}
      <section className="mb-6">
        <h2 className="text-lg font-bold text-kb-text mb-5 pb-2 border-b border-kb-border">
          대출 상품 선택 <span className="text-kb-red text-[11px] font-normal ml-1">* 필수</span>
        </h2>
        <div className="grid grid-cols-2 gap-4">
          {LOAN_PRODUCTS.map(p => (
            <button
              key={p.id}
              onClick={() => setSelectedProduct(p.id)}
              className={`border rounded-xl p-6 text-left transition-colors ${
                selectedProduct === p.id
                  ? 'border-kb-taupe bg-kb-yellow/20'
                  : 'border-kb-border hover:bg-kb-beige-light'
              }`}
            >
              <p className="text-[14px] font-bold text-kb-text mb-1">{p.name}</p>
              <p className="text-[12px] text-kb-blue font-medium">{p.rate}</p>
              <p className="text-[12px] text-kb-text-muted mt-1">최대 {p.maxAmount.toLocaleString()}만원</p>
            </button>
          ))}
        </div>
      </section>

      {/* 신청 정보 입력 */}
      <section className="mb-6">
        <h2 className="text-lg font-bold text-kb-text mb-5 pb-2 border-b border-kb-border">신청 정보 입력</h2>
        <div className="border border-kb-border-dark rounded-xl divide-y divide-kb-border overflow-hidden">

          {/* 신청 금액 */}
          <div className="flex items-start px-5 py-4 gap-4">
            <label className="w-36 text-[13px] font-medium text-kb-text flex-shrink-0 pt-2">
              신청 금액 <span className="text-kb-red">*</span>
            </label>
            <div className="flex-1 space-y-2">
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={formatAmountDisplay(amount)}
                  onChange={e => setAmount(e.target.value.replace(/,/g, ''))}
                  className="flex-1 border border-kb-border px-3 py-2 text-[13px] focus:outline-none focus:border-kb-taupe text-right"
                  placeholder="0"
                />
                <span className="text-[13px] text-kb-text">원</span>
              </div>
              <div className="flex gap-1.5 flex-wrap">
                {[100, 300, 500, 1000, 3000, 5000].map(v => (
                  <button
                    key={v}
                    onClick={() => handleAmountQuick(v)}
                    className="border border-kb-border px-3 py-1 text-[11px] text-kb-text-body hover:bg-kb-beige-light"
                  >
                    +{v}만
                  </button>
                ))}
                <button
                  onClick={() => setAmount('')}
                  className="border border-kb-border px-3 py-1 text-[11px] text-kb-text-muted hover:bg-kb-beige-light"
                >
                  초기화
                </button>
              </div>
            </div>
          </div>

          {/* 대출 기간 */}
          <div className="flex items-center px-5 py-4 gap-4">
            <label className="w-36 text-[13px] font-medium text-kb-text flex-shrink-0">
              대출 기간 <span className="text-kb-red">*</span>
            </label>
            <div className="flex gap-1.5">
              {PERIODS.map(p => (
                <button
                  key={p}
                  onClick={() => setPeriod(p)}
                  className={`border px-4 py-1.5 text-[12px] rounded-lg transition-colors ${
                    period === p
                      ? 'bg-kb-yellow border-kb-taupe font-bold text-kb-text'
                      : 'border-kb-border text-kb-text-muted hover:bg-kb-beige-light'
                  }`}
                >
                  {p}개월
                </button>
              ))}
            </div>
          </div>

          {/* 대출 목적 */}
          <div className="flex items-center px-5 py-4 gap-4">
            <label className="w-36 text-[13px] font-medium text-kb-text flex-shrink-0">
              대출 목적 <span className="text-kb-red">*</span>
            </label>
            <select
              value={purpose}
              onChange={e => setPurpose(e.target.value)}
              className="border border-kb-border px-3 py-2 text-[13px] focus:outline-none focus:border-kb-taupe w-48"
            >
              <option value="">선택하세요</option>
              {PURPOSES.map(p => <option key={p} value={p}>{p}</option>)}
            </select>
          </div>

          {/* 고용 형태 */}
          <div className="flex items-center px-5 py-4 gap-4">
            <label className="w-36 text-[13px] font-medium text-kb-text flex-shrink-0">
              고용 형태 <span className="text-kb-red">*</span>
            </label>
            <select
              value={employmentType}
              onChange={e => setEmploymentType(e.target.value)}
              className="border border-kb-border px-3 py-2 text-[13px] focus:outline-none focus:border-kb-taupe w-48"
            >
              <option value="">선택하세요</option>
              {EMPLOYMENT_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>

          {/* 연간 소득 */}
          <div className="flex items-center px-5 py-4 gap-4">
            <label className="w-36 text-[13px] font-medium text-kb-text flex-shrink-0">연간 소득</label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={annualIncome}
                onChange={e => setAnnualIncome(e.target.value.replace(/[^\d]/g, ''))}
                className="border border-kb-border px-3 py-2 text-[13px] w-36 focus:outline-none text-right"
                placeholder="0"
              />
              <span className="text-[13px] text-kb-text">만원</span>
            </div>
          </div>
        </div>
      </section>

      {/* 약관 동의 */}
      <section className="mb-6">
        <div className="border border-kb-border rounded-xl p-6 bg-kb-beige-light">
          <p className="text-[13px] text-kb-text-body leading-relaxed mb-3">
            · 본 대출 신청은 심사 후 승인 결과가 달라질 수 있습니다.<br />
            · 대출 거절 시 신용등급에 영향을 줄 수 있습니다.<br />
            · 과도한 빚은 당신에게 큰 불행을 안겨줄 수 있습니다.
          </p>
          <label className="flex items-center gap-2 cursor-pointer">
            <button
              type="button"
              onClick={() => setAgreed(v => !v)}
              className={`w-4 h-4 border flex-shrink-0 flex items-center justify-center transition-colors
                ${agreed ? 'bg-kb-yellow border-kb-taupe' : 'bg-white border-kb-border'}`}
            >
              {agreed && (
                <svg viewBox="0 0 12 10" fill="none" className="w-3 h-3">
                  <path d="M1 5l3 3 7-7" stroke="#333" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              )}
            </button>
            <span className="text-[13px] text-kb-text">
              위 내용을 확인하였으며, <span className="font-bold">개인신용정보 조회 및 대출 신청에 동의합니다.</span>
            </span>
          </label>
        </div>
      </section>

      {/* 버튼 */}
      <div className="flex justify-center gap-3">
        <Link
          href="/products/loan"
          className="px-14 py-3 border border-kb-border text-[14px] text-kb-text hover:bg-kb-beige-light transition-colors"
        >
          취소
        </Link>
        <button
          onClick={handleSubmit}
          disabled={!canSubmit}
          className={`px-14 py-3 text-[14px] font-bold transition-all ${
            canSubmit
              ? 'bg-kb-yellow text-kb-text hover:brightness-95'
              : 'bg-gray-200 text-gray-400 cursor-not-allowed'
          }`}
        >
          대출 신청하기
        </button>
      </div>
    </div>
  )
}
