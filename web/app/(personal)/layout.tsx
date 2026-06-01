import Header from '@/components/layout/Header'
import AuthGuard from '@/components/layout/AuthGuard'
import Link from 'next/link'

const FOOTER_LINKS_TOP = [
  '보호금융상품등록부', '전자민원접수', '전자금융거래기본약관',
  '개인정보 처리방침', '신용정보활용체제', '위치기반서비스 이용약관', '경영공시',
]
const FOOTER_LINKS_BOTTOM = [
  '이용상담', '보안프로그램', '사고신고', '그룹 내 고객정보 제공안내',
  '스튜어드십 코드', 'AXful인증서 제류문의', 'AXful 뱅킹 Ads',
]
const FOOTER_DROPDOWNS = [
  'AXful금융그룹네트워크', '대표전화 1588-0000', '챗봇/채팅/이메일상담(24시간)', '비교조회서비스',
]

export default function PersonalLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-white">
      <Header />
      <div className="min-h-[calc(100vh-300px)]">
        <AuthGuard>{children}</AuthGuard>
      </div>
      <footer className="border-t border-kb-border bg-white">
        <div className="max-w-kb-container mx-auto px-6 py-5">
          <div className="flex flex-wrap gap-x-1 gap-y-1 mb-2">
            {FOOTER_LINKS_TOP.map((link, i) => (
              <span key={link} className="flex items-center gap-3">
                {i > 0 && <span className="text-kb-border">|</span>}
                <Link href="#" className={`text-sm hover:underline text-kb-text ${['개인정보 처리방침', '전자민원접수', '전자금융거래기본약관', '신용정보활용체제', '위치기반서비스 이용약관', '경영공시'].includes(link) ? 'font-semibold' : ''}`}>
                  {link}
                </Link>
              </span>
            ))}
          </div>
          <div className="flex flex-wrap gap-x-1 gap-y-1 mb-4">
            {FOOTER_LINKS_BOTTOM.map((link, i) => (
              <span key={link} className="flex items-center gap-3">
                {i > 0 && <span className="text-kb-border">|</span>}
                <Link href="#" className="text-sm text-kb-text hover:underline">{link}</Link>
              </span>
            ))}
          </div>
          <p className="text-sm text-kb-text">
            사업자 등록번호 : 000-00-00000 &nbsp;|&nbsp; 서울특별시 중구 태평로1길 1(AXful동) &nbsp;|&nbsp; 대표 : 홍대표
          </p>
        </div>
        <div className="border-t border-kb-border bg-kb-beige-light">
          <div className="max-w-kb-container mx-auto px-6 py-3 flex items-center justify-between">
            <div className="flex items-center gap-3">
              {FOOTER_DROPDOWNS.map((label) => (
                <button key={label}
                  className="flex items-center gap-1.5 border border-kb-border px-3 py-1.5
                             text-sm text-kb-text-body bg-white hover:bg-kb-beige transition-colors">
                  {label} <span className="text-xs text-kb-text-muted">▾</span>
                </button>
              ))}
            </div>
            <div className="flex items-center gap-2">
              {[{ label: 'f', color: '#1877F2' }, { label: '📷', color: '#E4405F' }, { label: '▶', color: '#FF0000' }, { label: 'B', color: '#00C300' }].map((sns) => (
                <Link key={sns.label} href="#"
                  className="w-8 h-8 rounded-full border border-kb-border flex items-center justify-center text-sm font-bold hover:opacity-80"
                  style={{ color: sns.color }}>{sns.label}</Link>
              ))}
            </div>
          </div>
          <div className="max-w-kb-container mx-auto px-6 pb-4">
            <p className="text-sm text-kb-text-muted">Copyright AXful Bank. All Rights Reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  )
}
