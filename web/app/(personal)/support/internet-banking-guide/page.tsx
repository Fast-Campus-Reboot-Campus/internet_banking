'use client'
import { KB_MINT,KB_PRIMARY } from '@/lib/theme'

import Link from 'next/link'

const SECTIONS = [
  {
    title: '이용 대상',
    items: [
      'AXful Bank 계좌를 보유한 개인 고객',
      '공인인증서 또는 AXful 인증서를 발급받은 고객',
    ],
  },
  {
    title: '이용 시간',
    items: [
      '24시간 365일 이용 가능 (일부 서비스 제외)',
      '계좌이체: 평일 00:30 ~ 23:30 / 주말·공휴일 00:30 ~ 23:30',
      '시스템 점검 시간 (매일 23:30 ~ 00:30) 에는 일부 서비스가 제한될 수 있습니다.',
    ],
  },
  {
    title: '이용 방법',
    items: [
      '1단계: 보안프로그램 설치 후 인터넷뱅킹 접속',
      '2단계: AXful 인증서 또는 공동인증서로 로그인',
      '3단계: 조회·이체·예금·대출 등 금융서비스 이용',
    ],
  },
  {
    title: '이용 준비사항',
    items: [
      '권장 브라우저: Chrome, Edge, Firefox 최신 버전',
      '보안프로그램 설치 필수 (키보드 보안, 백신 등)',
      'AXful 인증서 또는 공동인증서 발급 필요',
    ],
  },
  {
    title: '이체 한도',
    items: [
      '1회 이체한도: 최대 1억원',
      '1일 이체한도: 최대 5억원',
      '한도 변경은 뱅킹관리 > 이체한도 변경 메뉴에서 가능합니다.',
    ],
  },
  {
    title: '유의사항',
    items: [
      '타인에게 인증서 비밀번호·OTP를 절대 알려주지 마세요.',
      'AXful Bank는 전화·문자로 비밀번호를 요청하지 않습니다.',
      '의심스러운 거래 발생 시 즉시 고객센터(1588-0000)로 연락하세요.',
      '금융사기 피해 예방을 위해 정기적으로 비밀번호를 변경해 주세요.',
    ],
  },
]

export default function InternetBankingGuidePage() {
  return (
    <div className="max-w-kb-container mx-auto px-6 py-8">
      {/* 브레드크럼 */}
      <div className="flex items-center gap-1 text-[12px] text-kb-text-muted mb-6">
        <Link href="/" className="hover:underline">홈</Link>
        <span>›</span>
        <span>고객지원</span>
        <span>›</span>
        <span className="font-semibold text-kb-text">인터넷뱅킹 이용안내</span>
      </div>

      <h1 className="text-[24px] font-bold text-kb-text mb-2">인터넷뱅킹 이용안내</h1>
      <p className="text-[14px] text-kb-text-muted mb-8">AXful Bank 인터넷뱅킹 서비스를 안전하고 편리하게 이용하는 방법을 안내해 드립니다.</p>

      <div className="space-y-6">
        {SECTIONS.map(section => (
          <div key={section.title} className="border border-kb-border rounded-xl overflow-hidden">
            <div className="px-6 py-3 font-semibold text-[15px] text-white" style={{ backgroundColor: KB_PRIMARY }}>
              {section.title}
            </div>
            <ul className="px-6 py-4 space-y-2">
              {section.items.map((item, i) => (
                <li key={i} className="flex items-start gap-2 text-[14px] text-kb-text-body">
                  <span className="mt-1.5 w-1.5 h-1.5 rounded-full flex-shrink-0" style={{ backgroundColor: KB_MINT }} />
                  {item}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      {/* 하단 바로가기 */}
      <div className="mt-8 flex gap-3">
        <Link href="/security-install"
          className="px-5 py-2.5 text-[14px] font-semibold rounded-lg border-2 transition-colors hover:bg-kb-primary-bg"
          style={{ borderColor: KB_PRIMARY, color: KB_PRIMARY }}>
          보안프로그램 설치
        </Link>
        <Link href="/cert"
          className="px-5 py-2.5 text-[14px] font-semibold rounded-lg text-white transition-opacity hover:opacity-85"
          style={{ backgroundColor: KB_PRIMARY }}>
          인증센터 바로가기
        </Link>
      </div>
    </div>
  )
}
