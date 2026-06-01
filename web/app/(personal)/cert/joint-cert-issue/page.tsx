'use client'

import Link from 'next/link'
import { useState } from 'react'

const SIDEBAR_ITEMS = [
  { label: '공동인증서 발급/재발급', href: '/cert/joint-cert-issue', active: true },
  { label: '타행·타기관인증서 등록/해제', href: '#' },
  { label: '인증서 갱신', href: '#' },
  { label: '인증서 관리', href: '/cert/joint-cert-management' },
  { label: '인증서폐기/수수료환급등록', href: '#' },
  { label: '영수증/세금계산서', href: '#' },
  { label: '스마트폰 인증서 복사', href: '#' },
  { label: 'AXful인증서로 발급받기', href: '#' },
]

const TERMS = [
  { label: '전자금융거래기본약관', required: true },
  { label: '전자금융서비스이용약관', required: true },
  { label: '전자금융서비스설명서', required: true },
  { label: '개인(신용)정보 수집·이용 동의서(개인 공동/금융인증서 발급용)', required: true },
  { label: '고유식별정보 수집·이용 동의서(개인 공동/금융인증서 발급용)', required: true },
]

const STEPS = ['약관동의 및 사용자 본인확인', '2', '3', '4', '5', '6', '7']

export default function JointCertIssuePage() {
  const [allChecked, setAllChecked] = useState(true)
  const [checked, setChecked] = useState<boolean[]>(TERMS.map(() => true))
  const [userId, setUserId] = useState('')
  const [rrn1, setRrn1] = useState('')
  const [rrn2, setRrn2] = useState('')

  function toggleAll() {
    const next = !allChecked
    setAllChecked(next)
    setChecked(TERMS.map(() => next))
  }

  function toggleOne(i: number) {
    const next = checked.map((v, idx) => (idx === i ? !v : v))
    setChecked(next)
    setAllChecked(next.every(Boolean))
  }

  return (
    <div className="max-w-kb-container mx-auto px-6 py-8 flex gap-8">

      {/* 좌측 사이드바 */}
      <aside className="w-52 flex-shrink-0">
        <div className="bg-white border border-kb-border">
          <div className="bg-kb-text px-4 py-3">
            <p className="text-body font-bold text-white">인증센터(개인)</p>
          </div>
          <div className="px-4 py-3 border-b border-kb-border bg-kb-beige-light">
            <p className="text-body font-bold text-kb-text">공동인증서(구 공인인증서)</p>
          </div>
          {SIDEBAR_ITEMS.map((item) => (
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
          <div className="px-4 py-2.5 border-b border-kb-border text-caption text-kb-text-body hover:bg-kb-beige-light cursor-pointer">기타인증서비스</div>
          <div className="px-4 py-2.5 border-b border-kb-border text-caption text-kb-text-body hover:bg-kb-beige-light cursor-pointer">인증서 발급 안내</div>
          <div className="px-4 py-2.5 border-b border-kb-border text-caption text-kb-text-body hover:bg-kb-beige-light cursor-pointer">인증센터 FAQ</div>
        </div>

        {/* 프로모 박스 */}
        <div className="mt-4 border border-kb-border p-4 bg-kb-beige-light text-center">
          <p className="text-[11px] text-kb-text-muted leading-tight mb-2">1,500만명의 선택</p>
          <p className="text-caption font-bold text-kb-text mb-3">AXful인증서 제휴신청</p>
          <div className="w-10 h-10 bg-kb-yellow rounded-lg flex items-center justify-center mx-auto mb-2">
            <span className="text-[9px] font-extrabold text-kb-text">AX</span>
          </div>
          <Link href="#" className="text-[11px] text-kb-blue hover:underline">바로가기 &gt;</Link>
        </div>
      </aside>

      {/* 우측 메인 */}
      <main className="flex-1 min-w-0 space-y-6">

        {/* 브레드크럼 */}
        <div className="flex items-center gap-1 text-[12px] text-kb-text-muted">
          <Link href="/cert" className="hover:text-kb-text">인증센터(개인)</Link>
          <span>&gt;</span>
          <span>공동인증서(구 공인인증서)</span>
          <span>&gt;</span>
          <span>공동인증서 발급/재발급</span>
          <span>&gt;</span>
          <span className="text-kb-text font-medium">개인용 인증서 발급</span>
        </div>

        {/* 페이지 제목 */}
        <h2 className="text-[22px] font-bold text-kb-text border-b-2 border-kb-text pb-3">
          개인용 인증서 발급
        </h2>

        {/* 단계 표시 */}
        <div className="flex items-center gap-0">
          {STEPS.map((step, i) => (
            <div key={i} className="flex items-center">
              <div className={`flex items-center justify-center rounded-full text-[11px] font-bold border-2
                ${i === 0
                  ? 'w-auto px-3 h-7 bg-kb-yellow border-kb-yellow text-kb-text'
                  : 'w-7 h-7 border-gray-300 text-gray-400'
                }`}
              >
                {i === 0 ? step : step}
              </div>
              {i < STEPS.length - 1 && <div className="w-6 h-px bg-gray-300 mx-1" />}
            </div>
          ))}
        </div>

        {/* 약관동의 섹션 */}
        <section>
          <h3 className="text-[15px] font-bold text-kb-text mb-3">약관동의 및 사용자 본인 확인</h3>

          {/* 전체약관보기 */}
          <div
            onClick={toggleAll}
            className="flex items-center justify-between px-4 py-3 border border-kb-border bg-gray-50 cursor-pointer hover:bg-kb-beige-light mb-1"
          >
            <div className="flex items-center gap-2">
              <span className="text-green-600 font-bold">✓</span>
              <span className="text-[13px] font-bold text-kb-text">전체약관보기</span>
            </div>
            <span className="text-kb-text-muted">&gt;</span>
          </div>

          {/* 개별 약관 */}
          <div className="border border-kb-border divide-y divide-kb-border">
            {TERMS.map((term, i) => (
              <div
                key={term.label}
                className="flex items-center justify-between px-4 py-3 hover:bg-kb-beige-light cursor-pointer"
                onClick={() => toggleOne(i)}
              >
                <div className="flex items-center gap-2">
                  <span className={`font-bold text-[13px] ${checked[i] ? 'text-green-600' : 'text-gray-300'}`}>✓</span>
                  {term.required && (
                    <span className="text-[11px] text-kb-blue font-semibold">[필수]</span>
                  )}
                  <span className="text-[13px] text-kb-text-body">{term.label}</span>
                </div>
                <span className="text-kb-text-muted">&gt;</span>
              </div>
            ))}
          </div>
        </section>

        {/* 사용자 본인확인 섹션 */}
        <section className="border border-kb-border p-6 space-y-4">
          <h3 className="text-[15px] font-bold text-kb-text border-b border-kb-border pb-3">사용자 본인확인</h3>

          {/* 사용자 ID */}
          <div className="flex items-center gap-3">
            <label className="w-24 text-[13px] text-kb-text-body text-right flex-shrink-0">사용자 ID</label>
            <input
              type="text"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              className="border border-gray-400 px-2 py-1.5 text-[13px] w-40 outline-none focus:border-blue-400"
            />
            <Link href="/customer/id-inquiry" className="text-[12px] text-kb-blue hover:underline">
              ID를 모르시는 경우↗
            </Link>
          </div>

          {/* 주민등록번호 */}
          <div className="flex items-center gap-3">
            <label className="w-24 text-[13px] text-kb-text-body text-right flex-shrink-0">주민등록번호</label>
            <div className="flex items-center gap-1">
              <input
                type="text"
                maxLength={6}
                value={rrn1}
                onChange={(e) => setRrn1(e.target.value.replace(/\D/g, ''))}
                className="border border-gray-400 px-2 py-1.5 text-[13px] w-24 outline-none focus:border-blue-400"
              />
              <span className="text-gray-500">-</span>
              <input
                type="password"
                maxLength={7}
                value={rrn2}
                onChange={(e) => setRrn2(e.target.value.replace(/\D/g, ''))}
                className="border border-gray-400 px-2 py-1.5 text-[13px] w-24 outline-none focus:border-blue-400"
              />
            </div>
            <label className="flex items-center gap-1 text-[12px] text-kb-text-body cursor-pointer">
              <input type="checkbox" className="w-3.5 h-3.5" />
              마우스로 입력
            </label>
          </div>
          <p className="text-[11px] text-kb-text-muted pl-[108px]">
            ① 숫자만 입력하실 수 있으며, 붙여넣기는 지원되지 않습니다.
          </p>

          <p className="text-[12px] text-kb-text-body pt-2">
            위의 전자금융거래기본약관과 전자금융서비스이용약관의 내용에 동의하고 인터넷뱅킹 서비스를 이용하시겠습니까?
          </p>

          {/* 버튼 */}
          <div className="flex gap-2 pt-2">
            <button className="px-8 py-2.5 bg-kb-yellow text-[13px] font-bold text-kb-text hover:brightness-95">
              약관 동의/본인확인
            </button>
            <button className="px-8 py-2.5 border border-gray-400 text-[13px] text-gray-600 hover:bg-gray-50">
              취소
            </button>
          </div>
        </section>

        {/* 보안카드/OTP 안내 */}
        <div className="flex items-center gap-4 border border-kb-border p-5 bg-kb-beige-light">
          <div className="flex-shrink-0 w-14 h-14 bg-white border border-kb-border rounded flex items-center justify-center">
            <span className="text-2xl">🔐</span>
          </div>
          <div>
            <p className="text-[13px] font-bold text-kb-text mb-1">보안카드/OTP가 없으신가요?</p>
            <p className="text-[12px] text-kb-text-muted leading-relaxed">
              AXful인증서를 발급 받으시면 보안카드/OTP가 없어도 인증서를 발급 받을 수 있습니다
            </p>
            <Link href="#" className="text-[12px] text-kb-blue hover:underline">AXful인증서 알아보기 &gt;</Link>
          </div>
        </div>

      </main>
    </div>
  )
}
