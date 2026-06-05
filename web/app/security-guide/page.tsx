'use client'
import { KB_MINT,KB_PRIMARY,KB_PRIMARY_BG,KB_PRIMARY_BORDER } from '@/lib/theme'

import Link from 'next/link'

const PROGRAMS = [
  {
    id: 'cert',
    name: '공동인증프로그램',
    subtitle: 'WizIn-Delfino G3',
    sections: [
      {
        title: '프로그램 개요',
        items: [
          '공동인증서 로그인 및 전자서명에 사용되는 인증 보안 프로그램입니다.',
          '인터넷뱅킹 이용 시 본인 확인 및 거래 내역 전자서명에 필수적으로 적용됩니다.',
          '설치 후 별도의 실행 없이 브라우저 접속 시 자동으로 작동합니다.',
        ],
      },
      {
        title: '지원 환경',
        items: [
          '운영체제: Windows 10 이상 (64bit 권장)',
          '브라우저: Chrome 80 이상, Edge 80 이상, Firefox 75 이상',
          '기타: Internet Explorer는 지원이 종료되어 이용이 불가합니다.',
        ],
      },
      {
        title: '설치 방법',
        items: [
          '1단계: 보안프로그램 설치 페이지에서 [설치하기] 버튼을 클릭합니다.',
          '2단계: 다운로드된 설치 파일(WizInDelfino_Setup.exe)을 실행합니다.',
          '3단계: 설치 완료 후 브라우저를 재시작합니다.',
          '4단계: 인터넷뱅킹 페이지 접속 후 정상 작동을 확인합니다.',
        ],
      },
      {
        title: '설치 오류 해결',
        items: [
          '설치 후 팝업이 계속 뜨는 경우: 브라우저 캐시 삭제 후 재시도하세요.',
          'Windows 보안 경고가 뜨는 경우: [추가 정보] → [실행] 클릭 후 설치를 진행하세요.',
          '설치가 완료되지 않는 경우: 기존 버전을 삭제 후 재설치하세요.',
          '그 외 문제 발생 시 고객센터(1588-0000)로 문의하세요.',
        ],
      },
    ],
  },
  {
    id: 'shield',
    name: '통합 보안프로그램',
    subtitle: 'AhnLab Safe Transaction',
    sections: [
      {
        title: '프로그램 개요',
        items: [
          '키보드 보안, 백신/방화벽, 피싱·파밍 방지, 단말환경 수집 기능을 통합 제공합니다.',
          '인터넷뱅킹 이용 중 발생할 수 있는 보안 위협을 실시간으로 차단합니다.',
          'OTP 이용 시에는 자율적으로 선택 설치가 가능합니다.',
        ],
      },
      {
        title: '주요 기능',
        items: [
          '키보드 보안: 키 입력 정보 암호화로 개인정보 유출을 방지합니다.',
          '백신/방화벽: 악성코드 및 해킹 시도를 실시간으로 탐지하고 차단합니다.',
          '피싱·파밍 방지: 가짜 은행 사이트 접속을 자동으로 감지하고 경고합니다.',
          '단말환경 수집: 접속 환경을 분석하여 이상 거래를 사전에 방지합니다.',
        ],
      },
      {
        title: '지원 환경',
        items: [
          '운영체제: Windows 10 이상 (64bit 권장)',
          '브라우저: Chrome 80 이상, Edge 80 이상, Firefox 75 이상',
          '디스크 여유 공간: 500MB 이상 권장',
        ],
      },
      {
        title: '설치 방법',
        items: [
          '1단계: 보안프로그램 설치 페이지에서 [설치하기] 버튼을 클릭합니다.',
          '2단계: 다운로드된 설치 파일(ASTSetup.exe)을 실행합니다.',
          '3단계: 이용약관 동의 후 설치를 진행합니다.',
          '4단계: 설치 완료 후 브라우저를 재시작합니다.',
        ],
      },
    ],
  },
]

export default function SecurityGuidePage() {
  return (
    <div className="min-h-screen flex flex-col" style={{ backgroundColor: KB_PRIMARY_BG }}>

      {/* 헤더 */}
      <header className="bg-white shadow-sm">
        <div className="max-w-[1200px] mx-auto px-6 h-[60px] flex items-center justify-between">
          <Link href="/" className="flex items-center gap-3">
            <div className="w-[6px] h-[26px] rounded-full" style={{ backgroundColor: KB_MINT }} />
            <span className="text-[22px] font-bold tracking-[0.02em]" style={{ color: KB_PRIMARY }}>AXful Bank</span>
          </Link>
          <Link href="/security-install"
            className="text-[14px] font-semibold px-4 py-1.5 rounded-lg border-2 transition-colors hover:bg-kb-primary-bg"
            style={{ borderColor: KB_PRIMARY, color: KB_PRIMARY }}>
            보안프로그램 설치
          </Link>
        </div>
      </header>

      {/* 본문 */}
      <main className="flex-1 max-w-[900px] mx-auto w-full px-6 py-10 space-y-10">

        <div>
          <p className="text-[13px] font-semibold mb-1" style={{ color: KB_MINT }}>Security Guide</p>
          <h1 className="text-[24px] font-bold text-kb-text mb-2">보안프로그램 이용안내</h1>
          <p className="text-[14px] text-kb-text-muted">AXful Bank 보안프로그램의 설치 방법과 이용 안내를 확인하세요.</p>
        </div>

        {PROGRAMS.map(program => (
          <div key={program.id} className="space-y-4">
            {/* 프로그램 헤더 */}
            <div className="flex items-center gap-3 pb-3 border-b-2" style={{ borderColor: KB_PRIMARY }}>
              <div className="w-10 h-10 rounded-xl flex items-center justify-center"
                style={{ backgroundColor: KB_PRIMARY }}>
                {program.id === 'cert' ? (
                  <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="white" strokeWidth="1.8">
                    <path d="M9 12l2 2 4-4"/>
                    <path d="M12 2a7 7 0 017 7c0 4-2.5 6-4 7.5V20a1 1 0 01-1 1h-4a1 1 0 01-1-1v-3.5C7.5 15 5 13 5 9a7 7 0 017-7z"/>
                  </svg>
                ) : (
                  <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="white" strokeWidth="1.8">
                    <path d="M12 2l7 3v6c0 4.5-3 8-7 9-4-1-7-4.5-7-9V5l7-3z"/>
                    <path d="M9 12l2 2 4-4"/>
                  </svg>
                )}
              </div>
              <div>
                <h2 className="text-[18px] font-bold text-kb-text">{program.name}</h2>
                <p className="text-[13px] text-kb-text-muted">{program.subtitle}</p>
              </div>
            </div>

            {/* 섹션들 */}
            <div className="space-y-4">
              {program.sections.map(section => (
                <div key={section.title} className="rounded-xl overflow-hidden border border-kb-border">
                  <div className="px-5 py-2.5 font-semibold text-[14px]" style={{ backgroundColor: KB_PRIMARY_BG, color: KB_PRIMARY }}>
                    {section.title}
                  </div>
                  <ul className="px-5 py-4 space-y-2 bg-white">
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
          </div>
        ))}

        {/* 하단 버튼 */}
        <div className="flex gap-3 pt-2">
          <Link href="/security-install"
            className="px-6 py-2.5 text-[14px] font-bold text-white rounded-lg hover:opacity-85 transition-opacity"
            style={{ backgroundColor: KB_PRIMARY }}>
            보안프로그램 설치하기
          </Link>
          <Link href="/"
            className="px-6 py-2.5 text-[14px] font-semibold rounded-lg border-2 transition-colors hover:bg-kb-primary-bg"
            style={{ borderColor: KB_PRIMARY, color: KB_PRIMARY }}>
            홈으로 이동
          </Link>
        </div>
      </main>

      {/* 푸터 */}
      <footer className="bg-white border-t" style={{ borderColor: KB_PRIMARY_BORDER }}>
        <div className="max-w-[900px] mx-auto px-6 py-5 space-y-2">
          <div className="flex flex-wrap gap-x-4 gap-y-1 text-[12px] text-kb-text-muted">
            {['보호금융상품등록부', '전자민원접수', '전자금융거래기본약관', '개인정보 처리방침', '신용정보활용체제', '위치기반서비스 이용약관', '경영공시'].map(item => (
              <Link key={item} href="#" className="hover:text-kb-text transition-colors">{item}</Link>
            ))}
          </div>
          <p className="text-[12px] text-kb-text-muted">Copyright AXful Bank. All Rights Reserved.</p>
        </div>
      </footer>
    </div>
  )
}
