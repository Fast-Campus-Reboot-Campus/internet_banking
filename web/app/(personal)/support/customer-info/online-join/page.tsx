'use client'

import Link from 'next/link'
import { useState } from 'react'
import { createPortal } from 'react-dom'
import { api } from '@/lib/api'

const SUPPORT_TABS = [
  { label: '고객상담',         href: '#' },
  { label: '고객정보관리',     href: '#', active: true },
  { label: '사고신고',         href: '#' },
  { label: '소비자보호',       href: '#' },
  { label: '금융서비스',       href: '#' },
  { label: '서식/약관/설명서', href: '#' },
  { label: '상품공시실',       href: '#' },
]

const LEFT_MENU = [
  { label: '고객정보조회/수정',              href: '#', sub: null },
  { label: 'ID조회/사용자암호 설정',         href: '#', sub: null },
  {
    label: '온라인고객관리', href: '#', open: true,
    sub: [
      { label: '온라인고객 신규가입', href: '/support/customer-info/online-join', active: true },
      { label: '조회계좌 등록/삭제',  href: '#', active: false },
      { label: '온라인고객 탈퇴',     href: '#', active: false },
    ],
  },
  { label: '본인정보 이용·제공 조회',        href: '#', sub: null },
  { label: '해외 납세의무자\n본인확인서 등록', href: '#', sub: null },
  { label: '고객확인제도(CDD/EDD)',          href: '#', sub: null },
  { label: '그룹 내 고객정보 제공 안내',     href: '#', sub: null },
]

const STEPS = ['약관동의', '본인확인', '정보입력', '가입완료']

// ── 약관 데이터 ──────────────────────────────────────────────
const TERMS_LIST = [
  { label: '전자금융거래기본약관' },
  { label: '전자금융서비스이용약관' },
  { label: '[필수] 개인(신용)정보 수집·이용 동의서\n(온라인고객 신규가입)' },
]

function TermContent({ index }: { index: number }) {
  if (index === 0) return (
    <div className="text-[13px] leading-relaxed text-kb-text-body space-y-4">
      <h2 className="text-[17px] font-bold text-center mb-6">전자금융거래기본약관</h2>
      <div>
        <p className="font-bold mb-1">제1조(목적)</p>
        <p>이 약관은 AX풀뱅크(이하 "은행"이라 합니다.)과 이용자 사이의 전자금융거래에 관한 기본적인 사항을 정함으로써, 거래의 신속하고 효율적인 처리를 도모하고 거래당사자 상호간의 이해관계를 합리적으로 조정하는 것을 목적으로 합니다.</p>
      </div>
      <div>
        <p className="font-bold mb-1">제2조(용어의 정의)</p>
        <p className="mb-2">① 이 약관에서 사용하는 용어의 의미는 다음 각 호와 같습니다.</p>
        <ol className="list-decimal ml-5 space-y-2">
          <li>"전자금융거래"라 함은 은행이 전자적 장치를 통하여 제공하는 금융상품 및 서비스를 이용자가 전자적 장치를 통하여 비대면·자동화된 방식으로 직접 이용하는 거래를 말합니다.</li>
          <li>"이용자"라 함은 전자금융거래를 위하여 은행과 체결한 계약(이하 "전자금융거래계약"이라 합니다.)에 따라 전자금융거래를 이용하는 고객을 말합니다.</li>
          <li>"지급인"이라 함은 전자금융거래에 의하여 자금이 출금되는 계좌(이하 "출금계좌"라 합니다.)의 명의인을 말합니다.</li>
          <li>"수취인"이라 함은 전자금융거래에 의하여 자금이 입금되는 계좌(이하 "입금계좌"라 합니다.)의 명의인을 말합니다.</li>
          <li>"전자적 장치"라 함은 현금자동지급기, 자동입출금기, 지급용단말기, 컴퓨터, 전화기 그 밖에 전자적 방법으로 전자금융거래정보를 전송하거나 처리하는데 이용되는 장치를 말합니다.</li>
          <li>"접근매체"라 함은 전자금융거래에 있어서 거래지시를 하거나 이용자 및 거래내용의 진정성을 확보하기 위하여 사용되는 수단 또는 정보를 말합니다.</li>
        </ol>
      </div>
      <div>
        <p className="font-bold mb-1">제3조(약관의 명시 및 변경)</p>
        <p className="mb-1">① 은행은 이 약관을 영업점 및 AX풀뱅크 인터넷뱅킹 홈페이지에 게시합니다.</p>
        <p>② 은행은 이 약관을 변경하는 경우 변경일로부터 1개월 전에 영업점 및 홈페이지에 게시합니다.</p>
      </div>
    </div>
  )

  if (index === 1) return (
    <div className="text-[13px] leading-relaxed text-kb-text-body space-y-4">
      <h2 className="text-[17px] font-bold text-center mb-6">전자금융서비스 이용약관</h2>
      <div>
        <p className="font-bold mb-1">제 1 조(목적)</p>
        <p>이 약관은 AX풀뱅크(이하 "은행"이라 한다)과 전자금융서비스(인터넷뱅킹, 모바일뱅킹, 폰뱅킹, 이하 서비스라 한다)를 이용하는 고객(이하 "이용자"라 한다)사이의 서비스 이용에 관한 제반 사항을 정함을 목적으로 한다.</p>
      </div>
      <div>
        <p className="font-bold mb-1">제 2 조(용어의 정의)</p>
        <p className="mb-2">① 이 약관에서 사용하는 용어의 의미는 다음 각 호와 같다.</p>
        <ol className="list-decimal ml-5 space-y-2">
          <li>"인터넷뱅킹"이라 함은 인터넷이 가능한 이용매체를 이용하여 계좌조회, 이체, 신규, 해지, 대출, 외화송금 등의 은행업무를 언제 어디서나 편리하게 이용할 수 있는 서비스를 말한다.</li>
          <li>"모바일뱅킹"이라 함은 휴대기기(스마트폰, 태블릿PC 등 모바일기기 포함)를 통하여 제공되는 계좌조회, 이체, 신규, 해지, 대출, 외화송금 등의 은행업무를 이용할 수 있는 서비스를 말한다.</li>
          <li>"폰뱅킹"이라 함은 전화를 이용하여 각종 조회, 이체, 상담, 사고신고, 자동이체신청, 지로 및 공과금납부 등의 은행업무를 처리하는 서비스를 말한다.</li>
          <li>"간편비밀번호" 또는 "PIN(Personal Identification Number)"이란 전자금융서비스 이용시 이용자의 본인확인수단으로서 이용자가 직접 지정한 6~8자리의 숫자가 조합된 개인인증번호를 말한다.</li>
          <li>"바이오인증"이란 지문, 음성, 정맥 등 이용자의 생체정보를 본인의 전자적 장치(스마트폰 등)에 미리 저장하여 은행이 확인하는 본인인증방법을 말한다.</li>
          <li>"알림서비스"란 고객이 지정한 휴대기기(스마트폰, 태블릿PC 등 모바일기기)를 통하여 고객의 입출금거래내역, 금융정보, 마케팅, 보안정보 등을 Push 알림으로 제공하는 서비스를 말한다.</li>
        </ol>
      </div>
      <div>
        <p className="font-bold mb-1">제 3 조(약관의 적용)</p>
        <p>① 서비스의 이용에 관하여 이 약관에 명시되지 아니한 사항은 전자금융거래법 및 관계법령, 전자금융거래기본약관(이하 "기본약관"이라 한다), 예금거래기본약관(가계용/기업용), 외환거래 약관 등 관련 약관의 규정에 따른다.</p>
      </div>
    </div>
  )

  // index === 2: 개인정보
  return (
    <div className="text-[13px] leading-relaxed text-kb-text-body">
      <h2 className="text-[15px] font-bold text-center mb-1">[필수] 개인(신용)정보 수집·이용 동의서</h2>
      <h2 className="text-[15px] font-bold text-center mb-4">(온라인고객 신규가입)</h2>
      <p className="font-bold mb-2">AX풀뱅크 귀중</p>
      <p className="mb-4 text-[12px]">* 귀 행과의 온라인고객 신규가입과 관련하여 귀 행이 본인의 개인(신용)정보를 수집·이용하고자 하는 경우에는 「신용정보의 이용 및 보호에 관한 법률」,「개인정보보호법」, 동 관계 법령에 따라 본인의 동의가 필요합니다.</p>

      <table className="w-full border-collapse text-[12px] mb-5">
        <tbody>
          <tr>
            <td className="border border-kb-border px-3 py-3 font-bold text-center w-28 align-top"
              style={{ backgroundColor: '#5BC9A8' }}>
              수집·이용 목적
            </td>
            <td className="border border-kb-border px-4 py-3">
              <ul className="space-y-1">
                <li>– 금융거래 인증, 금융정보 제공</li>
                <li>– 전자금융거래법에 의거 인터넷뱅킹, 모바일뱅킹 거래 시 필요 정보 수집</li>
                <li>– 전자금융거래의 내용 추적 및 검색</li>
                <li>– 인터넷 보안정책 수립 및 통계 자료 활용</li>
              </ul>
            </td>
          </tr>
          <tr>
            <td className="border border-kb-border px-3 py-3 font-bold text-center align-top"
              style={{ backgroundColor: '#5BC9A8' }}>
              보유 및 이용기간
            </td>
            <td className="border border-kb-border px-4 py-3">
              <p className="mb-2">– (금융)거래 종료일로부터 5 년까지 보유·이용됩니다.</p>
              <p className="font-bold text-[11px] mb-1">■ 위 보유 기간에서의 (금융)거래 종료일이란 "당 행과 거래중인 모든 계약(여·수신, 내·외국환, 카드 및 제3자 담보 제공 등) 해지 및 서비스(대여금고, 보호예수, 외국환거래지정, 인터넷뱅킹 포함 전자금융거래 등)가 종료된 날"을 말합니다.</p>
              <p className="text-[11px] text-kb-text-muted">※ (금융)거래 종료일 후에는 금융사고 조사, 분쟁 해결, 민원 처리, 법령상 의무이행을 위한 목적으로만 보유·이용됩니다.</p>
            </td>
          </tr>
          <tr>
            <td className="border border-kb-border px-3 py-3 font-bold text-center align-top"
              style={{ backgroundColor: '#5BC9A8' }}>
              거부권리및불이익
            </td>
            <td className="border border-kb-border px-4 py-3">
              귀하는 동의를 거부하실 수 있습니다. 다만, 위 개인(신용)정보 수집·이용에 관한 동의는 온라인고객 신규가입을 위한 필수적 사항이므로, 위 사항에 동의하셔야만 온라인고객 가입이 가능합니다.
            </td>
          </tr>
        </tbody>
      </table>

      <p className="font-bold mb-2">수집·이용 항목</p>
      <table className="w-full border-collapse text-[12px]">
        <tbody>
          <tr>
            <td className="border border-kb-border px-3 py-3 font-bold align-top" style={{ backgroundColor: '#5BC9A8', width: '110px' }}>
              개인(신용)정보<br />
              <span className="font-normal text-[11px]">└ 일반개인정보</span>
            </td>
            <td className="border border-kb-border px-4 py-3">
              – 성명, 생년월일, 아이디(ID), 사용자암호, 접속일시, IP주소, HDD Serial, 기기식별정보<br />
              위 개인(신용)정보 수집·이용에 동의하십니까?&nbsp;&nbsp;
              <label className="inline-flex items-center gap-1 cursor-pointer">
                <input type="radio" name="privacy_agree" readOnly /> 동의하지 않음
              </label>
              &nbsp;&nbsp;
              <label className="inline-flex items-center gap-1 cursor-pointer">
                <input type="radio" name="privacy_agree" defaultChecked readOnly /> 동의함
              </label>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}

export default function OnlineJoinPage() {
  const [step, setStep] = useState(0)

  // Step 0 — 약관
  const [termChecked, setTermChecked] = useState([false, false, false])
  const allTermChecked = termChecked.every(Boolean)
  const [termIndex, setTermIndex] = useState<number | null>(null)  // 모달 인덱스

  // Step 1 — 본인확인
  const [birth,      setBirth]      = useState('')
  const [accountNo,  setAccountNo]  = useState('')
  const [accountPw,  setAccountPw]  = useState('')
  const [mouseInput, setMouseInput] = useState(false)

  // Step 2 — 정보입력 (성명·생년월일은 본인확인에서 가져옴)
  const [name,       setName]      = useState('')
  const [genderCode, setGenderCode] = useState<'M' | 'F' | 'U'>('M')
  const [email,      setEmail]     = useState('')
  const [phone,      setPhone]     = useState('')
  const [userId,    setUserId]    = useState('')
  const [idOk,      setIdOk]      = useState<boolean | null>(null)
  const [pw,        setPw]        = useState('')
  const [pwConfirm, setPwConfirm] = useState('')
  const [mouseId,   setMouseId]   = useState(false)
  const [mouseId2,  setMouseId2]  = useState(false)
  const [registerError, setRegisterError] = useState('')

  // Step 3 — 가입완료
  const joinDate = new Date(2026, 4, 26)
  const joinDateStr = `${joinDate.getFullYear()}.${String(joinDate.getMonth()+1).padStart(2,'0')}.${String(joinDate.getDate()).padStart(2,'0')}`

  function handleAllCheck() {
    const next = !allTermChecked
    setTermChecked([next, next, next])
  }

  function handleSingleCheck(i: number) {
    const next = [...termChecked]
    next[i] = !next[i]
    setTermChecked(next)
  }

  function handleModalAgreeAll() {
    setTermChecked([true, true, true])
    setTermIndex(null)
  }

  function handleModalAgreeOne() {
    if (termIndex === null) return
    const next = [...termChecked]
    next[termIndex] = true
    setTermChecked(next)
    // 다음 미동의 약관으로 이동
    const nextIdx = [0,1,2].find(i => i > termIndex && !next[i])
    setTermIndex(nextIdx !== undefined ? nextIdx : null)
  }

  function handleStep0() {
    if (!allTermChecked) { alert('필수 약관에 모두 동의해주세요.'); return }
    setStep(1)
  }

  function handleStep1() {
    if (!birth)     { alert('생년월일을 입력해주세요.'); return }
    if (!accountNo) { alert('계좌번호를 입력해주세요.'); return }
    if (!accountPw) { alert('계좌비밀번호를 입력해주세요.'); return }
    setStep(2)
  }

  function handleIdCheck() {
    if (!userId || userId.length < 4) { alert('아이디는 4자리 이상 입력해주세요.'); return }
    setIdOk(true)
  }

  async function handleStep2() {
    if (!name)            { alert('성명을 입력해주세요.'); return }
    if (!idOk)            { alert('아이디 중복확인을 해주세요.'); return }
    if (pw.length < 8)    { alert('사용자암호는 8자리 이상 입력해주세요.'); return }
    if (pw !== pwConfirm) { alert('사용자암호가 일치하지 않습니다.'); return }

    // YYMMDD → YYYYMMDD 변환 (26 이하면 2000년대, 초과면 1900년대)
    const yy = parseInt(birth.slice(0, 2))
    const birthDate = (yy <= 26 ? '20' : '19') + birth

    try {
      await api.post('/api/v1/auth/register', {
        loginId: userId,
        password: pw,
        name,
        birthDate,
        genderCode,
        ...(email && { email }),
        ...(phone && { phone }),
      })
      setRegisterError('')
      setStep(3)
    } catch (err: any) {
      setRegisterError(err.response?.data?.message || '회원가입에 실패했습니다.')
    }
  }

  return (
    <div className="min-h-screen bg-white">
      {/* 고객센터 탭 */}
      <div className="bg-[#5D3D2B]">
        <div className="max-w-kb-container mx-auto px-6">
          <div className="flex">
            {SUPPORT_TABS.map(tab => (
              <Link key={tab.label} href={tab.href}
                className={`px-6 py-3 text-[14px] font-medium transition-colors ${
                  tab.active ? 'bg-[#5BC9A8] text-kb-text font-bold' : 'text-white hover:bg-white/10'
                }`}>
                {tab.label}
              </Link>
            ))}
          </div>
        </div>
      </div>

      <div className="max-w-kb-container mx-auto px-6 py-6">
        {/* 브레드크럼 */}
        <div className="flex justify-end mb-3 text-[12px] text-kb-text-muted gap-1">
          <span>고객센터</span><span>›</span>
          <span>고객정보관리</span><span>›</span>
          <span>온라인고객관리</span><span>›</span>
          <span className="text-kb-blue">온라인고객 신규가입</span>
        </div>

        <div className="flex gap-6">
          {/* 사이드바 */}
          <aside className="w-[200px] flex-shrink-0">
            <div className="border border-kb-border">
              <div className="bg-[#5D3D2B] px-4 py-3">
                <span className="text-white font-bold text-[14px]">고객정보관리</span>
              </div>
              {LEFT_MENU.map(item => (
                <div key={item.label}>
                  <Link href={item.href}
                    className="flex items-center justify-between px-4 py-3 text-[13px] border-t border-kb-border hover:bg-kb-beige-light text-kb-text-body whitespace-pre-line">
                    {item.label}
                    {item.sub && <span className="text-[10px] text-kb-text-muted">{item.open ? '▼' : '▶'}</span>}
                  </Link>
                  {item.open && item.sub?.map(sub => (
                    <Link key={sub.label} href={sub.href}
                      className={`block pl-6 pr-4 py-2.5 text-[12px] border-t border-kb-border transition-colors ${
                        sub.active
                          ? 'bg-[#5BC9A8] text-kb-text font-bold'
                          : 'hover:bg-kb-beige-light text-kb-text-muted'
                      }`}>
                      {sub.label}
                    </Link>
                  ))}
                </div>
              ))}
            </div>
            <div className="mt-3">
              <Link href="/cert"
                className="flex items-center justify-between border border-kb-border px-4 py-3 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                인증센터 <span className="text-[#5BC9A8] font-bold text-[16px]">›</span>
              </Link>
            </div>
          </aside>

          {/* 본문 */}
          <main className="flex-1 min-w-0">
            <div className="flex items-center justify-between mb-5">
              <h1 className="text-[22px] font-bold text-kb-text">온라인고객 신규가입</h1>
              <div className="flex items-center gap-1">
                {STEPS.map((s, i) => (
                  <div key={s} className="flex items-center gap-1">
                    <div className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-[12px] font-bold
                      ${step === i ? 'bg-[#5BC9A8] text-kb-text' : step > i ? 'bg-kb-border text-white' : 'border border-kb-border text-kb-text-muted'}`}>
                      <span>{i+1}.</span><span>{s}</span>
                    </div>
                    {i < STEPS.length-1 && <span className="text-kb-border text-[10px]">›</span>}
                  </div>
                ))}
              </div>
            </div>

            {/* ── STEP 0: 약관동의 ── */}
            {step === 0 && (
              <div>
                <div className="border border-kb-border mb-4">
                  {/* 전체약관보기 */}
                  <div className="flex items-center justify-between px-5 py-4 border-b border-kb-border bg-[#FAFAF7]">
                    <button className="flex items-center gap-3 flex-1 text-left" onClick={handleAllCheck}>
                      <div className={`w-5 h-5 rounded border-2 flex items-center justify-center flex-shrink-0
                        ${allTermChecked ? 'border-[#5BC9A8] bg-[#5BC9A8]' : 'border-kb-border'}`}>
                        {allTermChecked && <svg viewBox="0 0 12 10" fill="none" className="w-3 h-2.5"><polyline points="1,5 4.5,8.5 11,1" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>}
                      </div>
                      <span className="text-[14px] font-bold text-kb-text">전체약관보기</span>
                    </button>
                    <button onClick={() => setTermIndex(0)}
                      className="border border-kb-border px-3 py-1 text-[13px] font-semibold text-kb-text-body hover:bg-kb-beige flex-shrink-0">
                      약관보기 ›
                    </button>
                  </div>

                  {/* 개별 약관 */}
                  {TERMS_LIST.map((term, i) => (
                    <div key={i} className="flex items-center justify-between px-5 py-4 border-b border-kb-border last:border-b-0">
                      <div className="flex items-center gap-3 min-w-0">
                        <button onClick={() => handleSingleCheck(i)}
                          className={`w-5 h-5 rounded border-2 flex items-center justify-center flex-shrink-0 transition-colors
                            ${termChecked[i] ? 'border-[#5BC9A8] bg-[#5BC9A8]' : 'border-kb-border'}`}>
                          {termChecked[i] && <svg viewBox="0 0 12 10" fill="none" className="w-3 h-2.5"><polyline points="1,5 4.5,8.5 11,1" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>}
                        </button>
                        <span className="text-[13px] text-kb-text-muted whitespace-pre-line">
                          <span className="text-kb-blue font-semibold mr-1">[필수]</span>
                          {term.label}
                        </span>
                      </div>
                      <button onClick={() => setTermIndex(i)}
                        className="border border-kb-border px-3 py-1 text-[13px] font-semibold text-kb-text-body hover:bg-kb-beige flex-shrink-0 ml-3">
                        약관보기 ›
                      </button>
                    </div>
                  ))}
                </div>

                <div className="flex justify-center gap-3 mt-6">
                  <button onClick={handleStep0}
                    className="bg-[#5BC9A8] px-14 py-3 text-[14px] font-bold text-kb-text hover:opacity-90 transition-opacity">
                    확인
                  </button>
                  <Link href="/personal"
                    className="border border-kb-border px-14 py-3 text-[14px] text-kb-text-body hover:bg-kb-beige-light">
                    취소
                  </Link>
                </div>
              </div>
            )}

            {/* ── STEP 1: 본인확인 ── */}
            {step === 1 && (
              <div>
                <div className="border border-kb-border bg-[#FAFAF7] px-5 py-4 mb-6 text-[13px] text-kb-text-body space-y-1.5">
                  <p>· 온라인 고객으로 가입하시면 계좌조회 서비스를 이용하실 수 있습니다.</p>
                  <p>· AX풀뱅크 입출금식 상품 계좌를 보유하신 고객에 한하여 신규 가입이 가능합니다.</p>
                  <p>· 임의단체 또는 개인사업자, 기업 고객님께서는 AX풀뱅크 영업점을 방문하셔서 인터넷뱅킹에 가입하여 이용하여 주시기 바랍니다. (인터넷을 통한 온라인고객 신규 가입은 불가능 합니다.)</p>
                  <p>· 정보통신부 개인정보보호지침에 따라 만 14세미만 고객은 온라인 고객 가입이 제한됩니다. 가까운 영업점 내점하시어 부모님께서 대리인으로 인터넷뱅킹에 가입하시어 이용하시기 바랍니다.</p>
                  <p>· 본인확인을 위해 고객정보에 등록된 전화번호로 ARS 전화인증이 필요합니다. 전화번호가 없는 경우 가까운 영업점을 방문하여 주시기 바랍니다.</p>
                  <p>· 생년월일은 실제 생일이 아닌, 주민등록번호 발급 시 신고한 생년월일을 입력해주시기 바랍니다.</p>
                </div>

                <table className="w-full text-[13px] border-collapse mb-6">
                  <tbody>
                    <tr className="border-b border-kb-border">
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text w-36 whitespace-nowrap">
                        생년월일
                        <button className="ml-1 text-kb-text-muted border border-kb-border rounded-full w-4 h-4 text-[10px] inline-flex items-center justify-center">ⓘ</button>
                      </td>
                      <td className="border border-kb-border px-4 py-3">
                        <input type="text" value={birth} onChange={e => setBirth(e.target.value)}
                          placeholder="예: 1981년 2월 1일인 경우 : 810201" maxLength={6}
                          className="border border-kb-border px-3 py-1.5 w-64 outline-none text-[13px]" />
                      </td>
                    </tr>
                    <tr className="border-b border-kb-border">
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text whitespace-nowrap">
                        계좌번호
                        <button className="ml-1 text-kb-text-muted border border-kb-border rounded-full w-4 h-4 text-[10px] inline-flex items-center justify-center">ⓘ</button>
                      </td>
                      <td className="border border-kb-border px-4 py-3">
                        <input type="text" value={accountNo} onChange={e => setAccountNo(e.target.value)}
                          placeholder="'-' 없이 입력"
                          className="border border-kb-border px-3 py-1.5 w-64 outline-none text-[13px]" />
                      </td>
                    </tr>
                    <tr>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text whitespace-nowrap">
                        계좌비밀번호
                        <button className="ml-1 text-kb-text-muted border border-kb-border rounded-full w-4 h-4 text-[10px] inline-flex items-center justify-center">ⓘ</button>
                      </td>
                      <td className="border border-kb-border px-4 py-3">
                        <div className="flex items-center gap-3">
                          <input type={mouseInput ? 'text' : 'password'} value={accountPw}
                            onChange={e => setAccountPw(e.target.value)} maxLength={4}
                            placeholder="4자리 입력"
                            className="border border-kb-border px-3 py-1.5 w-28 outline-none text-[13px] text-center tracking-widest" />
                          <label className="flex items-center gap-1.5 text-[12px] text-kb-text-body cursor-pointer">
                            <input type="checkbox" checked={mouseInput} onChange={e => setMouseInput(e.target.checked)} className="w-3.5 h-3.5" />
                            마우스로 입력
                          </label>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>

                <div className="flex justify-center gap-3">
                  <button onClick={() => setStep(0)}
                    className="border border-kb-border px-14 py-3 text-[14px] text-kb-text-body hover:bg-kb-beige-light">
                    취소
                  </button>
                  <button onClick={handleStep1}
                    className="bg-[#5BC9A8] px-14 py-3 text-[14px] font-bold text-kb-text hover:opacity-90 transition-opacity">
                    확인
                  </button>
                </div>
              </div>
            )}

            {/* ── STEP 2: 정보입력 ── */}
            {step === 2 && (
              <div>
                <div className="border border-kb-border bg-[#FAFAF7] px-5 py-4 mb-6 text-[13px] text-kb-text-body space-y-1.5">
                  <p>· 본인확인을 위해 은행에 등록되어 있는 전화번호로 ARS 추가인증 완료 후 이용이 가능합니다.</p>
                  <p>· 은행에 등록되어 있는 전화번호가 없으신 경우, 가까운 영업점을 방문하셔서 인터넷뱅킹을 가입해주시기 바랍니다.</p>
                  <p>· 인터넷뱅킹ID: 인터넷뱅킹 서비스 이용을 위해 지정하는 정보로서 영문 또는 영문/숫자조합으로 입력(특수문자 제외, 6~12자리)</p>
                  <p>· 사용자암호: 인터넷뱅킹 가입시(영업점 또는 온라인 가입) 등록하는 암호로서 아이디(ID)로그인 시 필요(영문/숫자/특수문자 조합, 8~12자리)</p>
                </div>

                <table className="w-full text-[13px] border-collapse mb-6">
                  <tbody>
                    <tr className="border-b border-kb-border">
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text w-36 whitespace-nowrap">성명(고객명)</td>
                      <td className="border border-kb-border px-4 py-3">
                        <input type="text" value={name} onChange={e => setName(e.target.value)}
                          placeholder="실명 입력"
                          className="border border-kb-border px-3 py-1.5 w-52 outline-none text-[13px]" />
                      </td>
                    </tr>
                    <tr className="border-b border-kb-border">
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text whitespace-nowrap">생년월일</td>
                      <td className="border border-kb-border px-4 py-3 text-kb-text">{birth || '810201'}</td>
                    </tr>
                    <tr className="border-b border-kb-border">
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text whitespace-nowrap">성별</td>
                      <td className="border border-kb-border px-4 py-3">
                        <div className="flex items-center gap-4 text-[13px]">
                          {(['M', 'F', 'U'] as const).map(g => (
                            <label key={g} className="flex items-center gap-1.5 cursor-pointer">
                              <input type="radio" name="genderCode" value={g}
                                checked={genderCode === g}
                                onChange={() => setGenderCode(g)} />
                              {g === 'M' ? '남성' : g === 'F' ? '여성' : '미확인'}
                            </label>
                          ))}
                        </div>
                      </td>
                    </tr>
                    <tr className="border-b border-kb-border">
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text whitespace-nowrap">
                        이메일 <span className="font-normal text-kb-text-muted text-[11px]">(선택)</span>
                      </td>
                      <td className="border border-kb-border px-4 py-3">
                        <input type="email" value={email} onChange={e => setEmail(e.target.value)}
                          placeholder="example@email.com"
                          className="border border-kb-border px-3 py-1.5 w-52 outline-none text-[13px]" />
                      </td>
                    </tr>
                    <tr className="border-b border-kb-border">
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text whitespace-nowrap">
                        휴대전화 <span className="font-normal text-kb-text-muted text-[11px]">(선택)</span>
                      </td>
                      <td className="border border-kb-border px-4 py-3">
                        <input type="tel" value={phone} onChange={e => setPhone(e.target.value)}
                          placeholder="01012345678"
                          className="border border-kb-border px-3 py-1.5 w-52 outline-none text-[13px]" />
                      </td>
                    </tr>
                    <tr className="border-b border-kb-border">
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text whitespace-nowrap">ID</td>
                      <td className="border border-kb-border px-4 py-3">
                        <div className="flex items-center gap-2">
                          <input type="text" value={userId} onChange={e => { setUserId(e.target.value); setIdOk(null) }}
                            placeholder="6~12자리 영문/숫자 조합(특수문자 제외)"
                            className="border border-kb-border px-3 py-1.5 w-52 outline-none text-[13px]" />
                          <button onClick={handleIdCheck}
                            className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light whitespace-nowrap">
                            ID중복확인 ↗
                          </button>
                          <button className="text-kb-text-muted border border-kb-border rounded-full w-5 h-5 text-[10px] flex items-center justify-center flex-shrink-0">ⓘ</button>
                        </div>
                        {idOk === true  && <p className="text-[12px] text-[#5BC9A8] font-semibold mt-1">✓ 사용 가능한 아이디입니다.</p>}
                        {idOk === false && <p className="text-[12px] text-kb-red mt-1">이미 사용 중인 아이디입니다.</p>}
                      </td>
                    </tr>
                    <tr className="border-b border-kb-border">
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text whitespace-nowrap">사용자암호</td>
                      <td className="border border-kb-border px-4 py-3">
                        <div className="flex items-center gap-3">
                          <input type={mouseId ? 'text' : 'password'} value={pw}
                            onChange={e => setPw(e.target.value)}
                            placeholder="영문/숫자/특수문자 조합(8~12자리)"
                            className="border border-kb-border px-3 py-1.5 w-52 outline-none text-[13px]" />
                          <label className="flex items-center gap-1.5 text-[12px] cursor-pointer">
                            <input type="checkbox" checked={mouseId} onChange={e => setMouseId(e.target.checked)} className="w-3.5 h-3.5" />
                            마우스로 입력
                          </label>
                          <button className="text-kb-text-muted border border-kb-border rounded-full w-5 h-5 text-[10px] flex items-center justify-center">ⓘ</button>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text whitespace-nowrap">사용자암호 확인</td>
                      <td className="border border-kb-border px-4 py-3">
                        <div className="flex items-center gap-3">
                          <input type={mouseId2 ? 'text' : 'password'} value={pwConfirm}
                            onChange={e => setPwConfirm(e.target.value)}
                            placeholder="사용자암호 재입력"
                            className="border border-kb-border px-3 py-1.5 w-52 outline-none text-[13px]" />
                          <label className="flex items-center gap-1.5 text-[12px] cursor-pointer">
                            <input type="checkbox" checked={mouseId2} onChange={e => setMouseId2(e.target.checked)} className="w-3.5 h-3.5" />
                            마우스로 입력
                          </label>
                        </div>
                        {pwConfirm && pw !== pwConfirm && (
                          <p className="text-[12px] text-kb-red mt-1">사용자암호가 일치하지 않습니다.</p>
                        )}
                      </td>
                    </tr>
                  </tbody>
                </table>

                {registerError && (
                  <p className="text-center text-[13px] text-red-500 mb-3">{registerError}</p>
                )}
                <div className="flex justify-center gap-3">
                  <button onClick={() => setStep(1)}
                    className="border border-kb-border px-14 py-3 text-[14px] text-kb-text-body hover:bg-kb-beige-light">
                    취소
                  </button>
                  <button onClick={handleStep2}
                    className="bg-[#5BC9A8] px-14 py-3 text-[14px] font-bold text-kb-text hover:opacity-90 transition-opacity">
                    확인
                  </button>
                </div>
              </div>
            )}

            {/* ── STEP 3: 가입완료 ── */}
            {step === 3 && (
              <div>
                <div className="border border-[#5BC9A8] bg-[#F0FBF8] px-6 py-8 mb-6 flex items-center gap-6">
                  <div className="w-16 h-16 rounded-full bg-[#5BC9A8] flex items-center justify-center flex-shrink-0">
                    <svg viewBox="0 0 24 24" fill="none" className="w-8 h-8" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="20,6 9,17 4,12"/>
                    </svg>
                  </div>
                  <div>
                    <p className="text-[16px] font-bold text-[#2A8A6A] mb-1">{name}님, AX풀뱅크 온라인고객 가입이 완료되었습니다.</p>
                    <p className="text-[13px] text-kb-text-muted">인터넷뱅킹 서비스를 이용하시려면 AXful 금융인증서를 발급하세요.</p>
                  </div>
                </div>

                <table className="w-full text-[13px] border-collapse mb-6">
                  <tbody>
                    {[
                      { label: '성명(고객명)', value: name },
                      { label: 'ID',           value: userId },
                      { label: '가입일시',      value: joinDateStr },
                    ].map(row => (
                      <tr key={row.label} className="border-b border-kb-border last:border-b-0">
                        <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text w-36 whitespace-nowrap">{row.label}</td>
                        <td className="border border-kb-border px-4 py-3">{row.value}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                <div className="border border-kb-border bg-[#FAFAF7] px-5 py-4 mb-6 text-[12px] text-kb-text-muted space-y-1">
                  <p>· AXful 인터넷뱅킹 이용을 위해서는 AXful 금융인증서 또는 공동인증서 발급이 필요합니다.</p>
                  <p>· 안전한 금융거래를 위해 OTP 카드 등 보안매체 등록을 권장합니다.</p>
                  <p>· 이체, 대출 등 금융거래를 위해서는 영업점 방문 또는 비대면 실명확인이 필요할 수 있습니다.</p>
                </div>

                <div className="flex justify-center gap-3">
                  <Link href="/cert"
                    className="bg-[#5BC9A8] px-10 py-3 text-[14px] font-bold text-kb-text hover:opacity-90">
                    인증서 발급하기
                  </Link>
                  <Link href="/login"
                    className="border border-[#5BC9A8] px-10 py-3 text-[14px] font-semibold text-[#5BC9A8] hover:bg-[#F0FBF8]">
                    로그인하기
                  </Link>
                  <Link href="/personal"
                    className="border border-kb-border px-10 py-3 text-[14px] text-kb-text-body hover:bg-kb-beige-light">
                    메인으로
                  </Link>
                </div>
              </div>
            )}
          </main>
        </div>
      </div>

      {/* ── 약관 모달 (캐러셀) ── */}
      {termIndex !== null && createPortal(
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[9999]">
          <div className="bg-white w-[720px] max-h-[85vh] flex flex-col border border-kb-border shadow-2xl relative">
            {/* 헤더 */}
            <div className="flex items-center justify-between px-5 py-3 border-b border-kb-border bg-[#FAFAF7]">
              <div className="flex items-center gap-2">
                <svg viewBox="0 0 20 20" fill="none" className="w-4 h-4" stroke="currentColor" strokeWidth="1.5">
                  <line x1="3" y1="2" x2="3" y2="18"/><line x1="3" y1="2" x2="16" y2="2"/>
                  <line x1="3" y1="10" x2="12" y2="10"/>
                </svg>
                <span className="text-[13px] font-bold text-kb-text">약관동의 및 사용자 본인 확인</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-[11px] text-kb-text-muted">{termIndex + 1} / {TERMS_LIST.length}</span>
                <button onClick={() => setTermIndex(null)}
                  className="text-kb-text-muted hover:text-kb-text text-[18px] leading-none ml-2">✕</button>
              </div>
            </div>

            {/* 스크롤 본문 */}
            <div className="flex-1 overflow-y-auto px-10 py-6 relative">
              <div className="flex justify-end mb-3">
                <div className="border border-kb-border px-3 py-1 text-[11px] text-kb-text-muted">AX풀뱅크</div>
              </div>
              <TermContent index={termIndex} />
            </div>

            {/* 이전/다음 화살표 */}
            {termIndex > 0 && (
              <button onClick={() => setTermIndex(termIndex - 1)}
                className="absolute left-3 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-kb-border/80 hover:bg-kb-border flex items-center justify-center text-white text-[18px] font-bold shadow">
                ‹
              </button>
            )}
            {termIndex < TERMS_LIST.length - 1 && (
              <button onClick={() => setTermIndex(termIndex + 1)}
                className="absolute right-3 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-[#5BC9A8] hover:opacity-90 flex items-center justify-center text-white text-[18px] font-bold shadow">
                ›
              </button>
            )}

            {/* 하단 버튼 */}
            <div className="px-5 py-4 border-t border-kb-border flex items-center justify-between">
              <button onClick={() => { handleSingleCheck(termIndex); setTermIndex(null) }}
                className="border border-kb-border px-6 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                이 약관만 동의
              </button>
              <button onClick={handleModalAgreeAll}
                className="flex items-center gap-2 bg-[#5BC9A8] px-8 py-2.5 text-[14px] font-bold text-kb-text hover:opacity-90 transition-opacity">
                <svg viewBox="0 0 16 16" fill="none" className="w-4 h-4" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="3,8 6.5,11.5 13,4.5"/>
                </svg>
                전체동의
              </button>
            </div>
          </div>
        </div>
      , document.body)}
    </div>
  )
}
