import Link from 'next/link'

export default function NotFound() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-kb-beige-light px-6">
      <div className="text-center max-w-lg">
        {/* KB 로고 영역 */}
        <div className="mb-8">
          <div className="inline-flex items-center gap-2 mb-6">
            <div className="w-10 h-10 bg-kb-yellow rounded-sm flex items-center justify-center">
              <span className="text-[18px] font-black text-kb-text">AX</span>
            </div>
            <span className="text-[20px] font-bold text-kb-text">AX풀뱅크</span>
          </div>
        </div>

        {/* 404 */}
        <p className="text-[80px] font-black text-kb-yellow leading-none mb-2">404</p>
        <h1 className="text-2xl font-bold text-kb-text mb-3">페이지를 찾을 수 없습니다.</h1>
        <p className="text-base text-kb-text-muted leading-relaxed mb-8">
          요청하신 페이지가 존재하지 않거나 이동되었을 수 있습니다.<br />
          URL을 다시 확인하시거나 아래 버튼을 이용해 주세요.
        </p>

        <div className="flex justify-center gap-3">
          <Link
            href="/personal"
            className="px-8 py-3 bg-kb-yellow text-[14px] font-bold text-kb-text hover:brightness-95 transition-all"
          >
            홈으로 이동
          </Link>
          <Link
            href="/inquiry/accounts"
            className="px-8 py-3 border border-kb-border text-[14px] text-kb-text hover:bg-white transition-colors"
          >
            계좌조회
          </Link>
        </div>

        {/* 구분선 + 고객센터 */}
        <div className="mt-10 pt-6 border-t border-kb-border">
          <p className="text-[13px] text-kb-text-muted">
            도움이 필요하시면 고객센터로 연락해 주세요.
          </p>
          <p className="text-[18px] font-bold mt-1" style={{ color: '#3FA889' }}>
            1588-0000
          </p>
          <p className="text-[12px] text-kb-text-muted">평일 09:00 ~ 18:00 (은행 휴무일 제외)</p>
        </div>
      </div>
    </div>
  )
}
