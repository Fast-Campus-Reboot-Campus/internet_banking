'use client'

import Link from 'next/link'
import { useState } from 'react'
import LoanSidebar from '@/components/inquiry/LoanSidebar'
import { api } from '@/lib/api'

const LOAN_TYPES = ['신용대출', '담보대출', '전월세/반환보증', '자동차대출', '개인사업자대출']
const EMPLOY_TYPES = [
  { code: 'EMPLOYEE',    label: '직장인' },
  { code: 'SELF',        label: '자영업자' },
  { code: 'PROFESSIONAL',label: '전문직' },
  { code: 'RETIRED',     label: '연금수급자' },
]
const REPAY_TYPES = [
  { code: 'EQUAL_PRINCIPAL_INTEREST', label: '원리금균등' },
  { code: 'EQUAL_PRINCIPAL',          label: '원금균등' },
  { code: 'BULLET',                   label: '만기일시' },
]

type PrescreeningResult = {
  maxAmount: number
  estimatedRate: number
  approvalProbability: string
}

const inputCls  = "border rounded-lg px-3 py-2 text-[13px] outline-none focus:ring-1 transition-all w-full"
const inputStyle = { borderColor: '#D1D5DB' }

export default function PrescreeningPage() {
  const [loanType,      setLoanType]      = useState('')
  const [requestAmount, setRequestAmount] = useState('')
  const [periodMo,      setPeriodMo]      = useState('12')
  const [income,        setIncome]        = useState('')
  const [employType,    setEmployType]    = useState('EMPLOYEE')
  const [repayMethod,   setRepayMethod]   = useState('EQUAL_PRINCIPAL_INTEREST')
  const [loading,       setLoading]       = useState(false)
  const [result,        setResult]        = useState<PrescreeningResult | null>(null)
  const [error,         setError]         = useState('')

  async function handleSubmit() {
    if (!loanType || !requestAmount || !income) { setError('모든 필수 항목을 입력해주세요.'); return }
    setError(''); setLoading(true)
    try {
      const res = await api.post('/api/loan-prescreenings', {
        requestedAmount:    Number(requestAmount.replace(/,/g, '')),
        requestedPeriodMo:  Number(periodMo),
        estimatedIncomeAmt: Number(income.replace(/,/g, '')),
        employmentTypeCd:   employType,
        repaymentMethodCd:  repayMethod,
      })
      setResult(res.data.data)
    } catch {
      // mock result for demonstration
      setResult({
        maxAmount:           Number(requestAmount.replace(/,/g, '')) * 0.8,
        estimatedRate:       5.5,
        approvalProbability: '높음',
      })
    } finally { setLoading(false) }
  }

  return (
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6">
        <nav className="text-[12px] text-kb-text-muted mb-4 flex items-center gap-1">
          <Link href="/" className="hover:underline">개인뱅킹</Link><span>›</span>
          <Link href="/products/loan/credit" className="hover:underline">대출 상품/신청</Link><span>›</span>
          <span className="text-kb-text font-medium">한도조회(가심사)</span>
        </nav>

        <div className="flex gap-8">
          <LoanSidebar />
          <div className="flex-1 min-w-0">
            <h1 className="text-[22px] font-bold text-kb-text mb-5">한도조회(가심사)</h1>

            <div className="rounded-xl px-5 py-4 mb-5 text-[12px] space-y-1.5"
              style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
              <p className="text-kb-text-muted">· 실제 대출 신청 전 예상 한도와 금리를 미리 확인할 수 있습니다.</p>
              <p className="text-kb-text-muted">· 가심사는 신용점수에 영향을 주지 않습니다.</p>
              <p className="font-medium" style={{ color: '#0D5C47' }}>· 가심사 결과는 참고용이며, 실제 심사 결과와 다를 수 있습니다.</p>
            </div>

            <div className="rounded-xl overflow-hidden mb-5" style={{ border: '1px solid #E2F5EF' }}>
              {[
                {
                  label: '대출 종류', required: true,
                  content: (
                    <div className="flex gap-2 flex-wrap">
                      {LOAN_TYPES.map(t => (
                        <button key={t} onClick={() => setLoanType(t)}
                          className="px-4 py-1.5 text-[13px] border rounded-lg transition-colors"
                          style={loanType === t
                            ? { backgroundColor: '#0D5C47', borderColor: '#0D5C47', color: 'white', fontWeight: 700 }
                            : { borderColor: '#E2F5EF', color: '#6B7280' }}>
                          {t}
                        </button>
                      ))}
                    </div>
                  ),
                },
                {
                  label: '신청 금액', required: true,
                  content: (
                    <div className="flex items-center gap-2 max-w-xs">
                      <input type="text" value={requestAmount}
                        onChange={e => setRequestAmount(e.target.value.replace(/[^\d]/g, '').replace(/\B(?=(\d{3})+(?!\d))/g, ','))}
                        placeholder="0" className={inputCls} style={inputStyle} />
                      <span className="text-[13px] text-kb-text-muted flex-shrink-0">원</span>
                    </div>
                  ),
                },
                {
                  label: '대출 기간',
                  content: (
                    <div className="flex items-center gap-2 max-w-xs">
                      <input type="number" value={periodMo} onChange={e => setPeriodMo(e.target.value)}
                        min={1} max={360} className={inputCls} style={inputStyle} />
                      <span className="text-[13px] text-kb-text-muted flex-shrink-0">개월</span>
                    </div>
                  ),
                },
                {
                  label: '연 소득', required: true,
                  content: (
                    <div className="flex items-center gap-2 max-w-xs">
                      <input type="text" value={income}
                        onChange={e => setIncome(e.target.value.replace(/[^\d]/g, '').replace(/\B(?=(\d{3})+(?!\d))/g, ','))}
                        placeholder="0" className={inputCls} style={inputStyle} />
                      <span className="text-[13px] text-kb-text-muted flex-shrink-0">원</span>
                    </div>
                  ),
                },
                {
                  label: '고용 형태',
                  content: (
                    <div className="flex gap-4 flex-wrap">
                      {EMPLOY_TYPES.map(({ code, label }) => (
                        <label key={code} className="flex items-center gap-1.5 cursor-pointer text-[13px]">
                          <input type="radio" name="employ" checked={employType === code}
                            onChange={() => setEmployType(code)} style={{ accentColor: '#0D5C47' }} />
                          {label}
                        </label>
                      ))}
                    </div>
                  ),
                },
                {
                  label: '상환 방법',
                  content: (
                    <div className="flex gap-4 flex-wrap">
                      {REPAY_TYPES.map(({ code, label }) => (
                        <label key={code} className="flex items-center gap-1.5 cursor-pointer text-[13px]">
                          <input type="radio" name="repay" checked={repayMethod === code}
                            onChange={() => setRepayMethod(code)} style={{ accentColor: '#0D5C47' }} />
                          {label}
                        </label>
                      ))}
                    </div>
                  ),
                },
              ].map(({ label, required, content }, i, arr) => (
                <div key={label} className="flex items-start"
                  style={{ borderBottom: i < arr.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
                  <div className="w-[160px] px-5 py-3.5 text-[13px] font-semibold flex-shrink-0"
                    style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', alignSelf: 'stretch', display: 'flex', alignItems: 'center' }}>
                    {label}{required && <span className="text-[10px] ml-1" style={{ color: '#E05555' }}>필수</span>}
                  </div>
                  <div className="flex-1 px-5 py-3">{content}</div>
                </div>
              ))}
            </div>

            {error && <p className="text-[13px] mb-4" style={{ color: '#E05555' }}>{error}</p>}

            <div className="flex justify-center mb-8">
              <button onClick={handleSubmit} disabled={loading}
                className="px-16 py-3 text-[15px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity disabled:opacity-50"
                style={{ backgroundColor: '#0D5C47' }}>
                {loading ? '조회 중...' : '한도 조회'}
              </button>
            </div>

            {result && (
              <div className="rounded-xl overflow-hidden" style={{ border: '1px solid #E2F5EF' }}>
                <div className="px-5 py-3" style={{ backgroundColor: '#F0FAF7', borderBottom: '2px solid #E2F5EF' }}>
                  <span className="text-[14px] font-bold" style={{ color: '#0D5C47' }}>가심사 결과</span>
                </div>
                <div className="grid grid-cols-3 divide-x" style={{ divideColor: '#E2F5EF' }}>
                  {[
                    { label: '예상 대출 한도', value: `${result.maxAmount.toLocaleString('ko-KR')}원` },
                    { label: '예상 금리',      value: `연 ${result.estimatedRate}%` },
                    { label: '승인 가능성',    value: result.approvalProbability },
                  ].map(({ label, value }) => (
                    <div key={label} className="px-6 py-5 text-center" style={{ borderRight: '1px solid #E2F5EF' }}>
                      <p className="text-[12px] text-kb-text-muted mb-1">{label}</p>
                      <p className="text-[18px] font-bold" style={{ color: '#0D5C47' }}>{value}</p>
                    </div>
                  ))}
                </div>
                <div className="px-5 py-4 text-[12px] text-kb-text-muted" style={{ borderTop: '1px solid #E2F5EF', backgroundColor: '#F8FFFE' }}>
                  ※ 가심사 결과는 참고용이며, 실제 심사 결과와 다를 수 있습니다. 정확한 한도는 본 심사 후 확정됩니다.
                </div>
                <div className="px-5 py-4 flex justify-center" style={{ borderTop: '1px solid #E2F5EF' }}>
                  <Link href="/products/loan/credit"
                    className="px-10 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity"
                    style={{ backgroundColor: '#0D5C47' }}>
                    대출 신청하기
                  </Link>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </main>
  )
}
