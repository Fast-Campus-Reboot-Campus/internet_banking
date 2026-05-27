'use client'

import Link from 'next/link'
import { useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import DepositSidebar from '@/components/products/DepositSidebar'

const PRODUCT_NAMES: Record<string, string> = {
  'axful-regular': 'AXful 정기예금',
  'axful-super': 'AXful 수퍼정기예금(개인)',
  'regular': '일반정기예금',
  'axful-youth': 'AXful 청년도약계좌',
}

const TERMS = [
  '예금거래기본약관',
  '거치식예금약관',
  'AXful Star 정기예금 특약',
  'AXful Star 정기예금 상품설명서',
]

const MATURITY_OPTIONS = [
  '자동재예치(원금+이자)',
  '자동재예치(원금)',
  '자동해지',
]

/* ─── 아코디언 아이템 ─── */
function AccItem({ title, required, children, defaultOpen = false }: {
  title: string; required?: boolean; children: React.ReactNode; defaultOpen?: boolean
}) {
  const [open, setOpen] = useState(defaultOpen)
  return (
    <div className="border-b border-kb-border">
      <button
        onClick={() => setOpen(v => !v)}
        className="flex items-center justify-between w-full px-4 py-3 hover:bg-[#fafafa] transition-colors">
        <span className="flex items-center gap-2 text-[13px]">
          <svg viewBox="0 0 20 20" fill="none" className="w-4 h-4 flex-shrink-0">
            <circle cx="10" cy="10" r="9" stroke="#5BC9A8" strokeWidth="1.5"/>
            <polyline points="6,10 9,13 14,7" stroke="#5BC9A8" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          {required && <span className="text-[11px] font-bold text-[#5BC9A8] border border-[#5BC9A8] px-1.5 py-0.5 rounded-sm">필수</span>}
          <span className="font-semibold text-kb-text">{title}</span>
        </span>
        <span className="text-kb-text-muted text-xs">{open ? '∧' : '›'}</span>
      </button>
      {open && <div className="px-6 py-3 bg-[#FAFAFA] text-[12px] text-kb-text-body leading-relaxed">{children}</div>}
    </div>
  )
}

/* ─── 섹션 헤더 ─── */
function SectionHeader({ title }: { title: string }) {
  return (
    <div className="flex items-center gap-2 bg-[#F5F5F5] border border-kb-border px-4 py-3 mb-0">
      <svg viewBox="0 0 20 20" fill="none" className="w-4 h-4 flex-shrink-0">
        <circle cx="10" cy="10" r="9" stroke="#5BC9A8" strokeWidth="1.5"/>
        <polyline points="6,10 9,13 14,7" stroke="#5BC9A8" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
      <span className="text-[13px] font-bold text-kb-text">{title}</span>
    </div>
  )
}

/* ─── 정보입력 행 ─── */
function FormRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="border-b border-kb-border py-4">
      <p className="text-[13px] font-semibold text-kb-text mb-2">{label}</p>
      {children}
    </div>
  )
}

export default function DepositJoinPage() {
  const params = useParams()
  const router = useRouter()
  const id = typeof params.id === 'string' ? params.id : 'axful-regular'
  const productName = PRODUCT_NAMES[id] ?? 'AXful 정기예금'

  const [step, setStep] = useState<1 | 2 | 3>(1)

  /* ─── Step 1 state ─── */
  const [allChecked, setAllChecked] = useState(false)

  /* ─── Step 2 state ─── */
  const [period, setPeriod] = useState('')
  const [periodPreset, setPeriodPreset] = useState<string | null>(null)
  const [amount, setAmount] = useState('')
  const [couponType, setCouponType] = useState<'coupon' | 'point' | 'none'>('none')
  const [taxExempt, setTaxExempt] = useState(false)
  const [passwordType, setPasswordType] = useState<'same' | 'new'>('same')
  const [maturity, setMaturity] = useState('자동재예치(원금+이자)')
  const [lms, setLms] = useState<'yes' | 'no' | null>(null)
  const [docMethod, setDocMethod] = useState<'email' | 'lms' | null>(null)

  /* ─── Step 3 state ─── */
  const [confirmPw, setConfirmPw] = useState('')
  const [mouseInput, setMouseInput] = useState(false)

  const STEP_LABELS = ['약관동의', '정보입력', '정보확인']

  function addAmount(val: number) {
    const cur = parseInt(amount.replace(/,/g, '') || '0')
    setAmount((cur + val * 10000).toLocaleString())
  }

  function handleStep1Next() {
    if (!allChecked) { alert('필수 약관에 모두 동의해 주세요.'); return }
    setStep(2)
  }

  function handleStep2Next() {
    const m = parseInt(period)
    if (!m || m < 1 || m > 36) { alert('가입기간을 올바르게 입력해주세요. (1~36개월)'); return }
    const a = parseInt(amount.replace(/,/g, ''))
    if (!a || a < 1000000) { alert('가입금액은 최소 100만원 이상이어야 합니다.'); return }
    setStep(3)
  }

  function handleFinalConfirm() {
    if (!confirmPw && !mouseInput) { alert('계좌 비밀번호를 입력해주세요.'); return }
    alert(`${productName} 가입이 완료되었습니다!\n신규일자: 2026.05.25\n가입금액: ${amount}원`)
    router.push('/products/deposit')
  }

  const months = parseInt(period) || 0
  const maturityDate = months > 0
    ? new Date(2026, 5 - 1 + months, 25).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\. /g, '.').replace(/\.$/, '')
    : '-'

  return (
    <div className="max-w-kb-container mx-auto px-6 py-6">
      <div className="flex justify-end mb-3 text-[12px] text-kb-text-muted gap-1 items-center">
        <span>개인뱅킹</span><span>›</span>
        <span>금융상품</span><span>›</span>
        <span>예금</span><span>›</span>
        <Link href="/products/deposit" className="hover:underline">예금 상품/가입</Link>
        <span>›</span>
        <Link href="#" className="text-kb-blue hover:underline">도움말</Link>
      </div>

      <div className="flex gap-6">
        <DepositSidebar />

        <main className="flex-1 min-w-0">
          {/* 제목 + 스텝 */}
          <div className="flex items-center justify-between mb-5">
            <h1 className="text-[20px] font-bold text-kb-text">{productName}</h1>
            <div className="flex gap-1">
              {STEP_LABELS.map((s, i) => (
                <button key={s}
                  className={`px-4 py-1.5 text-[12px] transition-colors ${
                    i + 1 === step
                      ? 'font-bold text-white'
                      : 'text-kb-text-body border border-kb-border bg-white'
                  }`}
                  style={i + 1 === step ? { backgroundColor: '#5BC9A8' } : {}}>
                  {i + 1}. {s}
                </button>
              ))}
            </div>
          </div>

          {/* ══════════ STEP 1: 약관동의 ══════════ */}
          {step === 1 && (
            <div className="space-y-0">
              {/* 약관 및 상품설명서 */}
              <div className="border border-kb-border mb-4">
                <SectionHeader title="약관 및 상품설명서" />
                <div>
                  <AccItem title="약관 필수 동의">
                    {TERMS.map(t => (
                      <button key={t}
                        className="flex items-center justify-between w-full py-2 border-b border-kb-border last:border-0 hover:text-kb-blue transition-colors">
                        <span>{t}</span>
                        <span className="text-xs text-kb-text-muted">›</span>
                      </button>
                    ))}
                  </AccItem>
                </div>
              </div>

              {/* 확인 및 안내사항 */}
              <div className="border border-kb-border mb-4">
                <SectionHeader title="확인 및 안내사항" />
                <AccItem title="불법·탈법 자명거래 금지 설명 확인" required>
                  금융실명거래 및 비밀보장에 관한법률, 제 3조 제3항에 따라 누구든지 출발재산의 은닉, 자금세탁행위, 공중협박자금조달 행위 및 강제집행의 면탈, 그 밖의 탈법행위를 목적으로 타인의 실명으로 금융거래를 해서는 아니되며, 이를 위반시 5년 이하의 징역 또는 5천만원 이하의 벌금에 처할 수 있습니다.
                </AccItem>
                <AccItem title="예금자보호법 설명확인" required>
                  본인은 AX풀뱅크로부터 가입하는 금융상품의 예금자보호여부(보호 또는 비보호) 및 보호한도에 대하여 설명 받고 이해하였음을 확인합니다.
                </AccItem>
              </div>

              {/* 금융상품의 중요사항 안내 */}
              <div className="border border-kb-border mb-4">
                <SectionHeader title="금융상품의 중요사항 안내" />
                <AccItem title="우선설명 사항" required>
                  <p className="text-[#E05555]">이자율(중도해지이율) 만기후이율) 및 산출근거</p>
                </AccItem>
                <AccItem title="부담정보 및 금융소비자의 권리 사항" required>
                  <ul className="space-y-1">
                    {['중도 해지에 따른 불이익', '금리변동형 상품 안내', '자료열람요구권 행사에 관한 사항', '위법계약해지권 행사에 관한 사항',
                      '금융상품 판기 전후 안내(남물만기 알림 서비스)', '푸면대금 및 출연(계약의 거래유지)',
                      '예금자보호법에 관한 사항(예금자보호 여부 및 그 내용)', '민원처리 및 분쟁조정 절차'].map(item => (
                      <li key={item} className="text-[#E05555] flex gap-1.5 before:content-['·'] before:flex-shrink-0">{item}</li>
                    ))}
                  </ul>
                </AccItem>
                <AccItem title="예금성 상품 및 연계·제류 서비스" required>
                  <ul className="space-y-1 mb-2">
                    {['예금상품의 내용(계약기간, 이자의 지급시기 및 지급제한 사유)', '계약의 해제·해지',
                      '연계제류 서비스의 내용, 제공받을 수 있는 요건, 제공기간, 이행특칙, 변경시 변경내용 및 그 사유 등을 사전에 알린다는 사실 및 알리는 방법'].map(item => (
                      <li key={item} className="text-[#E05555] flex gap-1.5 before:content-['·'] before:flex-shrink-0">{item}</li>
                    ))}
                  </ul>
                  <p className="text-kb-text-muted">※ 금융상품의 중요사항에 대한 일반인 안내사항은 세부내용금융상품설명을 통해 확인하실 수 있습니다.</p>
                  <p className="text-kb-text-muted mt-1">※ 금융소비자는 해당상품 또는 서비스에 대해 설명을 받을 권리가 있습니다. 궁금한 내용이 있으시면 점포/채점상담(☎1588-9999), 영업점 직원에게 직접 문의해주시기 바랍니다.</p>
                  <p className="text-kb-text-muted mt-1">① 금융소비자보호법 제19조(설명의무) 항목에서 규정하고 있는 금융상품의 중요한 사항입니다.</p>
                </AccItem>
              </div>

              {/* 최종 동의 */}
              <div className="border border-kb-border p-4 mb-6">
                <label className="flex items-start gap-3 cursor-pointer">
                  <input type="checkbox" checked={allChecked} onChange={e => setAllChecked(e.target.checked)}
                    className="mt-0.5 w-4 h-4 accent-[#5BC9A8]" />
                  <div>
                    <p className="text-[13px] text-kb-text">본인은 위 예금상품의 약관 과 상품설명서에 대해 예금상품의 중요사항을 충분히 이해하여 본 상품에 가입함을 확인합니다.</p>
                    <p className="text-[12px] text-[#E05555] mt-1">※ 설명내용을 제대로 이해하지 못하였음에도 설명을 이해했다는 확인을 하는 경우, 추후 권리구제가 어려울 수 있습니다.</p>
                  </div>
                </label>
              </div>

              <div className="flex justify-center gap-2">
                <Link href={`/products/deposit/${id}`}
                  className="border border-kb-border px-10 py-2.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  이전
                </Link>
                <button onClick={handleStep1Next}
                  className="bg-kb-yellow px-10 py-2.5 text-[13px] font-bold text-kb-text hover:bg-kb-yellow-dark">
                  다음
                </button>
              </div>
            </div>
          )}

          {/* ══════════ STEP 2: 정보입력 ══════════ */}
          {step === 2 && (
            <div>
              <p className="text-[14px] font-bold text-[#5BC9A8] border-b-2 border-[#5BC9A8] inline-block pb-1 mb-4">정보입력</p>

              <div className="border border-kb-border px-5 py-2 mb-6 space-y-0">
                {/* 가입기간 */}
                <FormRow label="가입기간">
                  <div className="flex items-center gap-2 flex-wrap">
                    <p className="text-[12px] text-kb-text-muted mr-2">1~36개월, 월단위</p>
                    <input type="text" value={period} onChange={e => setPeriod(e.target.value.replace(/[^0-9]/g, ''))}
                      placeholder="기간"
                      className="border border-kb-border px-3 py-1.5 text-[13px] w-20 outline-none" />
                    <span className="text-[13px]">개월</span>
                    {[6, 12, 24, 36].map(m => (
                      <button key={m}
                        onClick={() => { setPeriod(String(m)); setPeriodPreset(String(m)) }}
                        className={`px-4 py-1.5 text-[12px] border transition-colors ${
                          periodPreset === String(m)
                            ? 'border-[#5BC9A8] text-[#5BC9A8] font-bold bg-white'
                            : 'border-kb-border text-kb-text-body hover:bg-kb-beige-light'
                        }`}>
                        {m}개월
                      </button>
                    ))}
                  </div>
                </FormRow>

                {/* 가입금액 */}
                <FormRow label="가입금액">
                  <div className="flex items-center gap-2 flex-wrap">
                    <p className="text-[12px] text-kb-text-muted mr-2">최소 100만원 이상, 원단위</p>
                    <input type="text" value={amount} onChange={e => setAmount(e.target.value)}
                      placeholder="0"
                      className="border border-kb-border px-3 py-1.5 text-[13px] w-32 outline-none text-right" />
                    <span className="text-[13px]">원</span>
                    {[1000, 500, 300, 100].map(v => (
                      <button key={v}
                        onClick={() => addAmount(v)}
                        className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                        {v >= 1000 ? `${v / 100}천만` : `${v}만`}
                      </button>
                    ))}
                  </div>
                </FormRow>

                {/* 쿠폰/포인트 */}
                <FormRow label="AXful금융쿠폰/포인트리 사용">
                  <div className="flex gap-6">
                    {([['coupon', 'AXful금융쿠폰(0)'], ['point', '포인트리'], ['none', '사용안함']] as const).map(([val, label]) => (
                      <label key={val} className="flex items-center gap-2 text-[13px] cursor-pointer">
                        <input type="radio" name="coupon" checked={couponType === val}
                          onChange={() => setCouponType(val)} className="accent-[#5BC9A8]" />
                        {label}
                      </label>
                    ))}
                  </div>
                </FormRow>

                {/* 비과세 */}
                <FormRow label="비과세종합저축으로 가입">
                  <div className="flex items-center gap-3">
                    <label className="flex items-center gap-2 text-[13px] cursor-pointer">
                      <input type="checkbox" checked={taxExempt} onChange={e => setTaxExempt(e.target.checked)}
                        className="w-4 h-4 accent-[#5BC9A8]" />
                      비과세종합저축 적용
                    </label>
                    <button className="text-[12px] text-kb-blue hover:underline">자세히보기 ›</button>
                  </div>
                </FormRow>

                {/* 출금계좌 */}
                <FormRow label="출금계좌번호">
                  <div className="flex items-center gap-2 mb-1">
                    <select className="border border-kb-border px-3 py-1.5 text-[13px] outline-none bg-white">
                      <option>AX풀뱅크</option>
                    </select>
                    <select className="border border-kb-border px-3 py-1.5 text-[13px] outline-none bg-white flex-1 max-w-[280px]">
                      <option>531089-04-274618(AX풀뱅크)</option>
                    </select>
                  </div>
                  <p className="text-[12px]">출금가능금액 <span className="text-[#E05555] font-bold">1,007,807</span>원</p>
                </FormRow>

                {/* 비밀번호 */}
                <FormRow label="비밀번호 입력">
                  <div className="flex gap-6">
                    {([['same', '출금계좌와 동일하게 설정'], ['new', '신규 설정']] as const).map(([val, label]) => (
                      <label key={val} className="flex items-center gap-2 text-[13px] cursor-pointer">
                        <input type="radio" name="pw" checked={passwordType === val}
                          onChange={() => setPasswordType(val)} className="accent-[#5BC9A8]" />
                        {label}
                      </label>
                    ))}
                  </div>
                </FormRow>

                {/* 만기 해지방법 */}
                <FormRow label="만기 해지방법">
                  <select value={maturity} onChange={e => setMaturity(e.target.value)}
                    className="border border-kb-border px-3 py-1.5 text-[13px] outline-none bg-white w-60">
                    {MATURITY_OPTIONS.map(o => <option key={o}>{o}</option>)}
                  </select>
                  <p className="text-[12px] text-kb-text-muted mt-1">* 만기 해지방법은 만기일 전까지 변경할 수 있습니다.</p>
                </FormRow>

                {/* LMS */}
                <FormRow label="상품만기알림(LMS) 서비스 신청">
                  <div className="flex gap-6 mb-2">
                    {([['yes', '예'], ['no', '아니오']] as const).map(([val, label]) => (
                      <label key={val} className="flex items-center gap-2 text-[13px] cursor-pointer">
                        <input type="radio" name="lms" checked={lms === val}
                          onChange={() => setLms(val)} className="accent-[#5BC9A8]" />
                        {label}
                      </label>
                    ))}
                  </div>
                  <p className="text-[12px] text-kb-text-muted">LMS를 통해 예·적금상품의 만기를 사전 안내해드리는 서비스</p>
                </FormRow>

                {/* 권유직원 */}
                <FormRow label="권유직원선택">
                  <select className="border border-kb-border px-3 py-1.5 text-[13px] outline-none bg-white w-48">
                    <option>권유직원없음</option>
                  </select>
                </FormRow>

                {/* 서류 수령 방법 */}
                <FormRow label="예금상품 계약서, 약관(특약), 상품설명서 제공">
                  <div className="flex gap-6 mb-2">
                    {([['email', '이메일주소로 받기'], ['lms', '문자메시지(LMS) 받기']] as const).map(([val, label]) => (
                      <label key={val} className="flex items-center gap-2 text-[13px] cursor-pointer">
                        <input type="radio" name="doc" checked={docMethod === val}
                          onChange={() => setDocMethod(val)} className="accent-[#5BC9A8]" />
                        {label}
                      </label>
                    ))}
                  </div>
                  <p className="text-[12px] text-kb-text-muted">※ 금융소비자보호법에 따라 이메일 및 문자메시지(LMS) 수신 거부 여부와 관계없이 발송됩니다.</p>
                </FormRow>
              </div>

              {/* 안내 박스 */}
              <div className="border border-kb-border bg-[#FAFAFA] px-4 py-3 mb-6 text-[12px] text-kb-text-body space-y-1">
                {[
                  '본 상품을 인터넷/AXful뱅킹으로 해지할 경우 휴대전화의 인증을 통해 해지가능합니다. 인증 후 본 상품에 가입합니다.',
                  '최신전화 휴대전화는 인증이 되지 않습니다.',
                  <span key="r" className="font-semibold">만기해지방법은 만기일 전까지 변경이 가능합니다.</span>,
                  <span key="r2" className="font-semibold">비과세종합저축 선택 시 만기해지방법은 자동해지만 가능합니다.</span>,
                ].map((n, i) => (
                  <p key={i} className="flex gap-1.5"><span className="flex-shrink-0">·</span><span>{n}</span></p>
                ))}
              </div>

              <div className="flex justify-center gap-2">
                <button onClick={() => setStep(1)}
                  className="border border-kb-border px-10 py-2.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  이전
                </button>
                <button onClick={handleStep2Next}
                  className="bg-kb-yellow px-10 py-2.5 text-[13px] font-bold text-kb-text hover:bg-kb-yellow-dark">
                  다음
                </button>
                <button className="border border-kb-border px-10 py-2.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  임시저장
                </button>
              </div>
            </div>
          )}

          {/* ══════════ STEP 3: 정보확인 ══════════ */}
          {step === 3 && (
            <div>
              <p className="text-[14px] font-bold text-[#5BC9A8] border-b-2 border-[#5BC9A8] inline-block pb-1 mb-4">정보 확인</p>

              <table className="w-full border-collapse text-[13px] border-t-2 border-kb-text mb-3">
                <tbody>
                  {[
                    { label: '신규일자', value: '2026.05.25' },
                    { label: '가입기간', value: `${maturityDate} (${period}개월)` },
                    { label: '가입금액', value: `${amount}원` },
                    { label: '이자지급방법', value: '공기일시지급 근식' },
                    { label: '적용금리', value: '2.1 + 0.75(%)' },
                    { label: '적용과세', value: taxExempt ? '비과세' : '일반' },
                    { label: '출금계좌', value: 'AX풀뱅크 531089-04-274618' },
                    { label: '상품만기알림(LMS) 서비스 신청', value: lms === 'yes' ? '신청' : '미신청' },
                    { label: '연계·제류서비스', value: '해당사항 없음' },
                  ].map(row => (
                    <tr key={row.label}>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text w-[180px] whitespace-nowrap">
                        {row.label}
                      </td>
                      <td className="border border-kb-border px-4 py-3 text-kb-text-body">{row.value}</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <div className="text-[12px] text-kb-text-muted mb-5 space-y-1">
                <p>※ 본 상품정보 교회를 이용하실 시 상품 이전을 참조하기 위한 것입니다. 자세한 내용은 <button className="text-kb-blue hover:underline">상품설명서 ›</button>를 참조하시기 바랍니다.</p>
                <p>※ 가입 후에는 '계약서류' 관련 약관을 통해 상품설명문서를 확인할 수 있습니다.</p>
              </div>

              {/* 계좌 비밀번호 확인 */}
              <div className="border border-t-2 border-kb-text pt-0">
                <p className="text-[14px] font-bold text-[#5BC9A8] border-b-2 border-[#5BC9A8] inline-block pb-1 mb-3">공규계좌 및 비밀번호 확인</p>
                <table className="w-full border-collapse text-[13px]">
                  <tbody>
                    <tr>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text w-[120px]">계좌번호</td>
                      <td className="border border-kb-border px-4 py-3 text-kb-text-body">531089-04-274618</td>
                    </tr>
                    <tr>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text">계좌 비밀번호</td>
                      <td className="border border-kb-border px-4 py-3">
                        <div className="flex items-center gap-3">
                          <input
                            type={mouseInput ? 'text' : 'password'}
                            value={confirmPw}
                            onChange={e => setConfirmPw(e.target.value)}
                            maxLength={4}
                            className="border border-kb-border px-3 py-1.5 text-[13px] w-28 outline-none"
                          />
                          <label className="flex items-center gap-1.5 text-[12px] cursor-pointer">
                            <input type="checkbox" checked={mouseInput} onChange={e => setMouseInput(e.target.checked)} />
                            마우스로 입력
                          </label>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div className="flex justify-center gap-2 mt-6">
                <button onClick={() => setStep(2)}
                  className="border border-kb-border px-10 py-2.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  이전
                </button>
                <button onClick={handleFinalConfirm}
                  className="bg-kb-yellow px-10 py-2.5 text-[13px] font-bold text-kb-text hover:bg-kb-yellow-dark">
                  확인
                </button>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
