import { KB_PRIMARY } from '@/lib/theme'
﻿import Link from 'next/link'

export default function NotFound() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-kb-beige-light px-6">
      <div className="w-full max-w-md">

        {/* 로고 */}
        <div className="flex justify-center mb-6">
          <div className="inline-flex items-center gap-2">
            <div className="w-9 h-9 rounded-sm flex items-center justify-center" style={{ backgroundColor: KB_PRIMARY }}>
              <span className="text-[16px] font-black text-white">AX</span>
            </div>
            <span className="text-[18px] font-bold text-kb-text">AXful Bank</span>
          </div>
        </div>

        {/* 카드 */}
        <div className="bg-white border border-kb-border rounded-lg px-10 py-10 text-center relative overflow-hidden">

          {/* 배경 장식 숫자 */}
          <span className="absolute inset-0 flex items-center justify-center text-[160px] font-black select-none pointer-events-none"
            style={{ color: KB_PRIMARY, opacity: 0.04, lineHeight: 1 }}>
            404
          </span>

          {/* 아이콘 */}
          <div className="flex justify-center mb-5">
            <div className="w-16 h-16 rounded-full flex items-center justify-center"
              style={{ backgroundColor: '#E8F7F3' }}>
              <svg viewBox="0 0 24 24" fill="none" className="w-8 h-8" stroke="#0D5C47" strokeWidth="1.8">
                <circle cx="11" cy="11" r="8"/>
                <path d="M21 21l-4.35-4.35"/>
                <path d="M11 8v3M11 14h.01" strokeLinecap="round"/>
              </svg>
            </div>
          </div>

          {/* 404 숫자 */}
          <p className="text-[56px] font-black leading-none mb-2" style={{ color: KB_PRIMARY }}>404</p>
          <h1 className="text-[18px] font-bold text-kb-text mb-2">페이지를 찾을 수 없습니다.</h1>
          <p className="text-[13px] text-kb-text-muted leading-relaxed mb-7">
            요청하신 페이지가 존재하지 않거나 이동되었을 수 있습니다.<br />
            URL을 다시 확인하시거나 아래 버튼을 이용해 주세요.
          </p>

          <div className="flex justify-center gap-2.5">
            <Link
              href="/"
              className="px-7 py-2.5 text-[13px] font-bold text-white rounded-lg transition-opacity hover:opacity-85"
              style={{ backgroundColor: KB_PRIMARY }}
            >
              홈으로 이동
            </Link>
            <Link
              href="/inquiry/accounts"
              className="px-7 py-2.5 border border-kb-border text-[13px] text-kb-text rounded-lg hover:bg-kb-beige-light transition-colors"
            >
              계좌조회
            </Link>
          </div>
        </div>

        {/* 고객센터 */}
        <div className="mt-5 text-center">
          <p className="text-[12px] text-kb-text-muted mb-1">도움이 필요하시면 고객센터로 연락해 주세요.</p>
          <p className="text-[17px] font-bold" style={{ color: KB_PRIMARY }}>1588-0000</p>
          <p className="text-[11px] text-kb-text-muted mt-0.5">평일 09:00 ~ 18:00 (은행 휴무일 제외)</p>
        </div>

      </div>
    </div>
  )
}
