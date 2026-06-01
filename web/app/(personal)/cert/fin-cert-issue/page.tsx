'use client'

import Link from 'next/link'
import { useState } from 'react'

const SIDEBAR_MENUS = [
  {
    label: '금융인증서',
    expanded: true,
    items: [
      { label: '인증서 발급/재발급', active: true, href: '/cert/fin-cert-issue' },
      { label: '타행·타기관 인증서 등록', href: '#' },
      { label: '타행·타기관 인증서 해제', href: '#' },
      { label: '인증서 갱신', href: '#' },
      { label: '인증서 폐기', href: '#' },
      { label: '인증서 관리', href: '#' },
      { label: '이용안내', href: '#' },
    ],
  },
]

const TERMS = [
  { label: '전자금융거래기본약관', required: true },
  { label: '전자금융서비스이용약관', required: true },
  { label: '전자금융서비스설명서', required: true },
  { label: '금융결제원 인증서 이용약관', required: true },
  { label: '개인정보 수집 및 이용고지', required: true },
  { label: '개인정보 제3자 제공동의', required: true },
  { label: '개인(신용)정보 수집·이용 동의서', required: true },
  { label: '고유식별정보 수집·이용 동의서', required: true },
]

export default function FinCertIssuePage() {
  const [expandedTerms, setExpandedTerms] = useState<Set<number>>(new Set())
  const [checkedTerms, setCheckedTerms] = useState<Set<number>>(new Set())
  const [allChecked, setAllChecked] = useState(false)
  const [userId, setUserId] = useState('')
  const [rrnFront, setRrnFront] = useState('')
  const [mouseInput, setMouseInput] = useState(false)

  function toggleTerm(i: number) {
    setExpandedTerms((prev) => {
      const next = new Set(prev)
      if (next.has(i)) { next.delete(i) } else { next.add(i) }
      return next
    })
  }

  function checkTerm(i: number) {
    setCheckedTerms((prev) => {
      const next = new Set(prev)
      if (next.has(i)) { next.delete(i) } else { next.add(i) }
      const allNow = next.size === TERMS.length
      setAllChecked(allNow)
      return next
    })
  }

  function handleAllCheck() {
    if (allChecked) {
      setCheckedTerms(new Set())
      setAllChecked(false)
    } else {
      setCheckedTerms(new Set(TERMS.map((_, i) => i)))
      setAllChecked(true)
    }
  }

  return (
    <div className="max-w-kb-container mx-auto px-6 py-8 flex gap-8">

      {/* 좌측 사이드바 */}
      <aside className="w-52 flex-shrink-0">
        <div className="bg-white border border-kb-border">
          <div className="bg-kb-text px-4 py-3">
            <p className="text-body font-bold text-white">인증센터(개인)</p>
          </div>
          {SIDEBAR_MENUS.map((section) => (
            <div key={section.label}>
              <div className="px-4 py-3 border-b border-kb-border bg-kb-beige-light">
                <p className="text-body font-bold text-kb-text">{section.label}</p>
              </div>
              {section.items.map((item) => (
                <Link
                  key={item.label}
                  href={item.href}
                  className={`block px-5 py-2.5 text-caption border-b border-kb-border transition-colors
                    ${item.active
                      ? 'bg-kb-yellow font-bold text-kb-text'
                      : 'text-kb-text-body hover:bg-kb-beige-light'
                    }`}
                >
                  {item.label}
                </Link>
              ))}
            </div>
          ))}
        </div>
      </aside>

      {/* 우측 메인 */}
      <main className="flex-1 min-w-0 space-y-8">

        {/* 브레드크럼 */}
        <div className="flex items-center gap-2 text-caption text-kb-text-muted">
          <Link href="/cert" className="hover:text-kb-text">인증센터(개인)</Link>
          <span>&gt;</span>
          <span>금융인증서</span>
          <span>&gt;</span>
          <span className="text-kb-text font-medium">인증서 발급/재발급</span>
        </div>

        {/* 페이지 제목 */}
        <h2 className="text-[22px] font-bold text-kb-text border-b-2 border-kb-text pb-3">
          인증서 발급/재발급
        </h2>

        {/* 금융인증서 서비스 안내 */}
        <div className="border border-kb-border bg-kb-beige-light p-6 space-y-3">
          <p className="text-body font-bold text-kb-text">금융인증서 서비스 안내</p>
          <ul className="space-y-2 text-caption text-kb-text-body list-disc list-inside">
            <li>금융인증서는 금융결제원의 클라우드에 저장되어 PC·스마트폰 등 기기에 관계없이 이용 가능합니다.</li>
            <li>유효기간은 3년이며 만료 전 갱신을 통해 계속 사용 가능합니다.</li>
            <li>발급 수수료는 무료입니다.</li>
          </ul>
        </div>

        {/* 약관 및 서비스설명서 */}
        <section>
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-body font-bold text-kb-text">약관 및 서비스설명서</h3>
            <label className="flex items-center gap-2 cursor-pointer text-caption text-kb-text-body select-none">
              <CheckBox checked={allChecked} onChange={handleAllCheck} />
              전체동의
            </label>
          </div>

          <div className="border border-kb-border divide-y divide-kb-border">
            {TERMS.map((term, i) => (
              <div key={term.label}>
                <div className="flex items-center px-4 py-3 gap-3">
                  <CheckBox checked={checkedTerms.has(i)} onChange={() => checkTerm(i)} />
                  <span className="text-caption text-kb-blue font-medium mr-1">[필수]</span>
                  <span className="flex-1 text-caption text-kb-text">{term.label}</span>
                  <button
                    onClick={() => toggleTerm(i)}
                    className="text-caption text-kb-text-muted hover:text-kb-text transition-colors px-2"
                    aria-expanded={expandedTerms.has(i)}
                  >
                    {expandedTerms.has(i) ? '▲' : '▼'}
                  </button>
                </div>
                {expandedTerms.has(i) && (
                  <div className="px-6 py-4 bg-kb-beige-light text-caption text-kb-text-muted leading-relaxed border-t border-kb-border">
                    {term.label}에 관한 약관 내용입니다. 서비스 이용에 동의해주시면 금융인증서 발급 서비스를 이용하실 수 있습니다.
                  </div>
                )}
              </div>
            ))}
          </div>
        </section>

        {/* 사용자 확인 */}
        <section>
          <h3 className="text-body font-bold text-kb-text mb-4">사용자 확인</h3>
          <div className="border border-kb-border divide-y divide-kb-border">

            {/* 사용자 ID */}
            <div className="flex items-center px-6 py-4 gap-4">
              <label className="w-36 text-caption font-medium text-kb-text flex-shrink-0">
                사용자 ID
              </label>
              <div className="flex-1 flex items-center gap-4">
                <input
                  type="text"
                  value={userId}
                  onChange={(e) => setUserId(e.target.value)}
                  className="flex-1 border border-kb-border px-3 py-2 text-caption text-kb-text focus:outline-none focus:border-kb-taupe"
                  placeholder="사용자 ID 입력"
                />
                <Link href="#" className="text-caption text-kb-blue hover:underline whitespace-nowrap">
                  ID를 모르시는 경우
                </Link>
              </div>
            </div>

            {/* 주민등록번호 */}
            <div className="flex items-center px-6 py-4 gap-4">
              <label className="w-36 text-caption font-medium text-kb-text flex-shrink-0">
                주민등록번호
              </label>
              <div className="flex-1 flex items-center gap-3">
                <input
                  type="text"
                  value={rrnFront}
                  onChange={(e) => setRrnFront(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  maxLength={6}
                  className="w-28 border border-kb-border px-3 py-2 text-caption text-kb-text focus:outline-none focus:border-kb-taupe text-center tracking-widest"
                  placeholder="생년월일"
                />
                <span className="text-kb-text-muted">-</span>
                {/* 뒷자리: 마우스 입력 전용 */}
                <div className="flex items-center gap-1">
                  {Array.from({ length: 7 }).map((_, j) => (
                    <div
                      key={j}
                      className="w-7 h-8 border border-kb-border bg-kb-beige-light flex items-center justify-center"
                    >
                      <span className="text-kb-text-muted text-body">●</span>
                    </div>
                  ))}
                </div>
                <label className="flex items-center gap-1.5 cursor-pointer select-none ml-2">
                  <CheckBox checked={mouseInput} onChange={() => setMouseInput(!mouseInput)} />
                  <span className="text-caption text-kb-text-body">마우스로 입력</span>
                </label>
              </div>
            </div>
          </div>
        </section>

        {/* 버튼 */}
        <div className="flex justify-center gap-3 pt-2">
          <Link
            href="/cert"
            className="px-14 py-3 border border-kb-border text-body text-kb-text hover:bg-kb-beige-light transition-colors"
          >
            취소
          </Link>
          <button className="px-14 py-3 bg-kb-yellow text-body font-bold text-kb-text hover:brightness-95 transition-all">
            확인
          </button>
        </div>

        {/* OTP 안내 */}
        <div className="border border-kb-border bg-white p-6 flex items-start gap-4">
          <div className="w-12 h-12 rounded-full bg-kb-beige-light flex items-center justify-center flex-shrink-0">
            <span className="text-2xl">🔑</span>
          </div>
          <div className="space-y-1">
            <p className="text-body font-bold text-kb-text">보안카드/OTP가 없으신가요?</p>
            <p className="text-caption text-kb-text-muted leading-relaxed">
              AXful인증서(기업)를 이용하시면 OTP 없이도 안전하게 금융거래를 하실 수 있습니다.
            </p>
            <Link href="/cert-biz" className="text-caption text-kb-blue hover:underline">
              AXful인증서(기업) 발급받기 &gt;
            </Link>
          </div>
        </div>

      </main>
    </div>
  )
}

function CheckBox({ checked, onChange }: { checked: boolean; onChange: () => void }) {
  return (
    <button
      type="button"
      onClick={onChange}
      className={`w-4 h-4 border flex-shrink-0 flex items-center justify-center transition-colors
        ${checked ? 'bg-kb-yellow border-kb-taupe' : 'bg-white border-kb-border'}`}
    >
      {checked && (
        <svg viewBox="0 0 12 10" fill="none" className="w-3 h-3">
          <path d="M1 5l3 3 7-7" stroke="#333" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      )}
    </button>
  )
}
