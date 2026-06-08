'use client'
import { KB_PRIMARY } from '@/lib/theme'

import Link from 'next/link'
import { useState } from 'react'

type MainTab = '인터넷뱅킹 이용안내' | '개인용인증서 발급안내' | '타인증서 등록안내'
type CertSubTab = '개인 범용 발급안내' | '개인 은행/신용카드/보험용 발급안내'

// ── 스텝 목록 ──
const CERT_STEPS_PERSONAL: { num: string; title: string; desc: string }[] = [
  { num: '01', title: '개인고객 로그인', desc: 'AXful Bank 지점에서 인터넷뱅킹 신청 시 지정한 사용자 아이디(ID)와 사용자 암호를 입력합니다.' },
  { num: '02', title: '약관동의 및 사용자 본인 확인', desc: '전자금융거래 기본약관, 전자금융서비스 이용약관, 인증서비스 약관을 확인하고 동의합니다.' },
  { num: '03', title: '발급대상 유료 인증서 선택', desc: '발급하고자 하는 개인용 인증서의 종류와 그 사용범위를 확인하고 선택합니다.\n개인 범용을 선택하는 경우 인증기관을 선택하고 약관에 동의해야 합니다.' },
  { num: '04', title: '전자금융사기 예방서비스', desc: '본인 확인용 추가 인증을 위하여 전자금융사기예방 서비스를 선택합니다.' },
  { num: '05', title: '출금계좌 확인', desc: '고객님 인터넷뱅킹에 등록된 출금계좌번호와 계좌비밀번호를 입력합니다.' },
  { num: '06', title: '수수료 출금 예약확인', desc: '개인 범용 인증서 발급수수료 유료화에 따른 수수료 금액 및 출금계좌번호를 확인합니다.\n(개인 범용: 1년 4,400원, 유효기간 내 재발급 시 면제)' },
  { num: '07', title: '고객 세부 정보 입력', desc: '고객님의 주소, 전화번호 등 세부 정보를 입력합니다.\n입력한 고객 정보는 인증서 발급용으로만 사용됩니다.' },
  { num: '08', title: '인증서 암호 및 저장위치 선정', desc: '인증서 저장 위치를 선택한 후, 앞으로 인증서 로그인 시 사용할 암호를 10~56자리(영문, 숫자, 특수문자 조합)로 입력합니다.' },
  { num: '09', title: '인증서 발급 완료', desc: '고객님의 인증서가 발급되었습니다.\n인증서 암호를 입력하고 로그인하면 AXful Bank 인터넷뱅킹을 편리하게 이용하실 수 있습니다.' },
]

const CERT_STEPS_BANK: { num: string; title: string; desc: string }[] = [
  { num: '01', title: '개인고객 로그인', desc: 'AXful Bank 지점에서 인터넷뱅킹 신청 시 지정한 사용자 아이디(ID)와 사용자 암호를 입력합니다.' },
  { num: '02', title: '약관동의 및 사용자 본인 확인', desc: '전자금융거래 기본약관, 전자금융서비스 이용약관, 인증서비스 약관을 확인하고 동의합니다.' },
  { num: '03', title: '발급대상 인증서 선택', desc: '발급하고자 하는 은행/신용카드/보험용 인증서를 선택합니다.' },
  { num: '04', title: '출금계좌 확인', desc: '고객님 인터넷뱅킹에 등록된 출금계좌번호와 계좌비밀번호를 입력합니다.' },
  { num: '05', title: '고객 세부 정보 입력', desc: '고객님의 주소, 전화번호 등 세부 정보를 입력합니다.' },
  { num: '06', title: '인증서 암호 및 저장위치 선정', desc: '인증서 저장 위치를 선택한 후, 인증서 로그인 시 사용할 암호를 입력합니다.' },
  { num: '07', title: '인증서 발급 완료', desc: '고객님의 은행/신용카드/보험용 인증서가 발급되었습니다.\n인증서 암호를 입력하고 로그인하면 AXful Bank 인터넷뱅킹을 이용하실 수 있습니다.' },
]

const OTHER_CERT_STEPS: { num: string; title: string; desc: string }[] = [
  { num: '01', title: '로그인', desc: 'AXful Bank 지점에서 인터넷뱅킹 신청 시 지정한 사용자 아이디(ID)와 사용자 암호를 입력합니다.' },
  { num: '02', title: '사용자 본인확인', desc: '본인확인 정보를 입력하고 [확인]을 클릭합니다.' },
  { num: '03', title: '인증서 암호 입력', desc: '[인증서 선택]을 클릭하고, 인증서 암호를 입력합니다.' },
  { num: '04', title: '전자금융사기 예방서비스', desc: '본인 확인용 추가 인증을 위하여 전자금융사기예방 서비스를 선택합니다.' },
  { num: '05', title: '출금계좌 확인', desc: '고객님 인터넷뱅킹에 등록된 출금계좌번호와 계좌비밀번호를 입력합니다.' },
  { num: '06', title: '타행·타기관 인증서 등록 내역확인', desc: '타행·타기관 인증서 등록 내역을 확인합니다.' },
  { num: '07', title: '인증서 등록 완료', desc: '타행·타기관에서 발급된 고객님의 인증서가 AXful Bank에 등록되었습니다.\n인증서 암호를 입력하고 로그인하면 AXful Bank 인터넷뱅킹을 편리하게 이용하실 수 있습니다.' },
]

function StepList({ steps }: { steps: { num: string; title: string; desc: string }[] }) {
  return (
    <div className="border border-kb-border rounded-xl overflow-hidden">
      {steps.map((step, i) => (
        <div key={step.num}
          className={`px-6 py-5 ${i < steps.length - 1 ? 'border-b border-kb-border' : ''}`}>
          <div className="flex items-start gap-4">
            <span className="text-[18px] font-bold flex-shrink-0 w-8" style={{ color: KB_PRIMARY }}>
              {step.num}
            </span>
            <div>
              <p className="text-[15px] font-semibold text-kb-text mb-1">{step.title}</p>
              <p className="text-[13px] text-kb-text-muted whitespace-pre-line leading-relaxed">{step.desc}</p>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}

const INTERNET_STEPS = [
  { num: '01', title: '보안프로그램 설치', desc: '인터넷뱅킹 이용을 위해 보안프로그램을 설치합니다.\n키보드 보안, 백신, 방화벽 프로그램이 자동으로 설치됩니다.' },
  { num: '02', title: '인증서 발급', desc: 'AXful 금융인증서 또는 공동인증서를 발급받습니다.\n이미 인증서가 있는 경우 타행인증서 등록을 진행합니다.' },
  { num: '03', title: '로그인', desc: '발급받은 인증서와 사용자 아이디(ID)/암호로 로그인합니다.' },
  { num: '04', title: '서비스 이용', desc: '조회·이체·예금·대출 등 다양한 금융서비스를 이용합니다.\n이체 서비스는 평일 00:30 ~ 23:30, 주말·공휴일 동일하게 운영됩니다.' },
]

export default function FirstVisitPage() {
  const [mainTab, setMainTab] = useState<MainTab>('인터넷뱅킹 이용안내')
  const [certSubTab, setCertSubTab] = useState<CertSubTab>('개인 범용 발급안내')

  return (
    <div className="max-w-kb-container mx-auto px-6 py-8">

      {/* 브레드크럼 */}
      <div className="flex items-center gap-1 text-[12px] text-kb-text-muted mb-6">
        <Link href="/" className="hover:underline">홈</Link>
        <span>›</span>
        <span>뱅킹관리</span>
        <span>›</span>
        <span>이용안내</span>
        <span>›</span>
        <span className="font-semibold text-kb-text">첫 방문 고객을 위한 안내</span>
      </div>

      <h1 className="text-[24px] font-bold text-kb-text mb-2">첫 방문 고객을 위한 안내</h1>
      <p className="text-[14px] text-kb-text-muted mb-8">
        AXful Bank 인터넷뱅킹을 처음 이용하시는 고객님을 위한 안내입니다.
      </p>

      {/* 메인 탭 */}
      <div className="flex border-b border-kb-border mb-6">
        {(['인터넷뱅킹 이용안내', '개인용인증서 발급안내', '타인증서 등록안내'] as MainTab[]).map((tab) => (
          <button key={tab} onClick={() => setMainTab(tab)}
            className={`px-6 py-3 text-[14px] whitespace-nowrap transition-colors
              ${mainTab === tab ? 'border-b-2 font-bold -mb-px' : 'text-kb-text-muted hover:text-kb-text'}`}
            style={mainTab === tab ? { borderColor: KB_PRIMARY, color: KB_PRIMARY } : {}}>
            {tab}
          </button>
        ))}
      </div>

      {/* ── 인터넷뱅킹 이용안내 ── */}
      {mainTab === '인터넷뱅킹 이용안내' && (
        <div className="space-y-6">
          <div className="bg-kb-primary-bg border border-kb-border rounded-xl px-6 py-4 text-[14px] text-kb-text-body space-y-1.5">
            <p>· 인터넷뱅킹은 은행이 정한 인증서를 발급받아야 이용이 가능합니다.</p>
            <p>· 이미 다른 은행에서 발급받은 인증서가 있다면 [타행인증서등록]을 통해 이용하실 수 있습니다.</p>
            <p>· 만 14세 미만 고객은 온라인 가입이 제한되며, 가까운 영업점을 방문해 주시기 바랍니다.</p>
          </div>
          <StepList steps={INTERNET_STEPS} />
          <div className="flex gap-3 pt-2">
            <Link href="/cert/fin-cert-issue"
              className="px-6 py-2.5 text-[14px] font-bold text-white rounded-lg transition-opacity hover:opacity-85"
              style={{ backgroundColor: KB_PRIMARY }}>
              인증서 발급하기
            </Link>
            <Link href="/login"
              className="px-6 py-2.5 text-[14px] font-semibold rounded-lg border-2 transition-colors hover:bg-kb-primary-bg"
              style={{ borderColor: KB_PRIMARY, color: KB_PRIMARY }}>
              로그인하기
            </Link>
          </div>
        </div>
      )}

      {/* ── 개인용인증서 발급안내 ── */}
      {mainTab === '개인용인증서 발급안내' && (
        <div className="space-y-5">
          {/* 서브탭 */}
          <div className="flex gap-1 border-b border-kb-border">
            {(['개인 범용 발급안내', '개인 은행/신용카드/보험용 발급안내'] as CertSubTab[]).map((tab) => (
              <button key={tab} onClick={() => setCertSubTab(tab)}
                className={`px-4 py-2.5 text-[13px] whitespace-nowrap transition-colors
                  ${certSubTab === tab ? 'border-b-2 font-bold -mb-px' : 'text-kb-text-muted hover:text-kb-text'}`}
                style={certSubTab === tab ? { borderColor: KB_PRIMARY, color: KB_PRIMARY } : {}}>
                ▶ {tab}
              </button>
            ))}
          </div>

          <StepList steps={certSubTab === '개인 범용 발급안내' ? CERT_STEPS_PERSONAL : CERT_STEPS_BANK} />

          <div className="flex gap-3 pt-2">
            <Link href="/cert/fin-cert-issue"
              className="px-6 py-2.5 text-[14px] font-bold text-white rounded-lg transition-opacity hover:opacity-85"
              style={{ backgroundColor: KB_PRIMARY }}>
              개인용인증서발급 바로가기
            </Link>
            <button onClick={() => setMainTab('타인증서 등록안내')}
              className="px-6 py-2.5 text-[14px] font-semibold rounded-lg border-2 transition-colors hover:bg-kb-primary-bg"
              style={{ borderColor: KB_PRIMARY, color: KB_PRIMARY }}>
              타인증서 등록안내
            </button>
          </div>
        </div>
      )}

      {/* ── 타인증서 등록안내 ── */}
      {mainTab === '타인증서 등록안내' && (
        <div className="space-y-5">
          {/* 인증서 종류 안내 */}
          <div className="border border-kb-border rounded-xl overflow-hidden">
            <div className="px-6 py-3 font-semibold text-[14px] text-white" style={{ backgroundColor: KB_PRIMARY }}>
              등록 가능한 인증서 종류
            </div>
            <div className="divide-y divide-kb-border">
              <div className="px-6 py-4">
                <p className="text-[14px] font-semibold text-kb-text mb-1">타행 인증서</p>
                <p className="text-[13px] text-kb-text-muted">다른 은행에서 이미 발급받은 인증서로서 인증서 발급기관이 금융결제원(YessignCA)인 인증서</p>
              </div>
              <div className="px-6 py-4">
                <p className="text-[14px] font-semibold text-kb-text mb-1">타기관 인증서</p>
                <p className="text-[13px] text-kb-text-muted">은행 외 증권사, 공공기관에서 발급받은 인증서로서 인증서 발급기관이 (주)코스콤(Signkorea), 한국정보인증(kica), 한국전자인증(crosscert), 한국무역정보통신(TradeSign)인 인증서</p>
              </div>
            </div>
          </div>

          <StepList steps={OTHER_CERT_STEPS} />

          <div className="flex gap-3 pt-2">
            <Link href="/cert/joint-cert-issue"
              className="px-6 py-2.5 text-[14px] font-bold text-white rounded-lg transition-opacity hover:opacity-85"
              style={{ backgroundColor: KB_PRIMARY }}>
              타행/타기관 인증서등록 바로가기
            </Link>
          </div>
        </div>
      )}
    </div>
  )
}
