'use client'

import Link from 'next/link'
import { useEffect } from 'react'
import { useSearchParams } from 'next/navigation'
import { Suspense } from 'react'

function LogoutContent() {
  const searchParams = useSearchParams()
  const isManual = searchParams.get('reason') === 'manual'

  useEffect(() => {
    localStorage.removeItem('access_token')
    localStorage.removeItem('user')
  }, [])

  return (
    <div className="min-h-[calc(100vh-260px)] py-12" style={{ backgroundColor: '#F0F8F4' }}>
      <div className="w-full max-w-[540px] mx-auto bg-white shadow-sm">
        {/* 본문 */}
        <div className="flex flex-col items-center px-12 py-14 gap-5">
          {/* 아이콘 */}
          <div className="text-6xl">{isManual ? '👋' : '🚪'}</div>

          <h2 className="text-2xl font-bold text-kb-text">
            {isManual ? '정상적으로 로그아웃되었습니다.' : '자동으로 로그아웃되었습니다.'}
          </h2>

          <p className="text-base text-kb-text-body text-center leading-relaxed">
            {isManual ? (
              <>
                AX풀뱅크 axful.com을 방문해주셔서 감사합니다.<br />
                즐거운 하루 보내시기 바랍니다.<br />
                <span className="text-kb-blue">확인을 누르시면 메인화면으로 이동합니다.</span>
              </>
            ) : (
              <>
                고객님의 안전한 금융거래를 위해 로그인 후 10분 동안 서비스 이용이 없어
                자동 로그아웃 되었습니다. 이용해 주셔서 감사합니다.<br />
                <span className="text-kb-blue">확인을 누르시면 메인화면으로 이동합니다.</span>
              </>
            )}
          </p>

          <Link href="/personal" className="btn-primary w-full py-3.5 text-center text-base font-bold mt-2">
            확인
          </Link>
        </div>

        {/* 하단 AX풀뱅크인증서 배너 */}
        <div className="border-t border-kb-border mx-6 mb-6" />
        <div className="mx-6 mb-6 flex items-center gap-4 border border-kb-border rounded-lg p-6">
          <div className="w-10 h-10 bg-kb-yellow rounded-full flex items-center justify-center flex-shrink-0">
            <span className="text-sm font-bold text-kb-text">AX</span>
          </div>
          <div>
            <p className="text-sm text-kb-text-body">
              간편하고 안전하게 사용가능한 AX풀뱅크인증서를<br />
              통해 다양한 금융서비스를 이용해보세요!
            </p>
            <Link href="#" className="text-sm text-kb-blue hover:underline">
              AX풀뱅크인증서 알아보기 &gt;
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default function LogoutPage() {
  return (
    <Suspense>
      <LogoutContent />
    </Suspense>
  )
}
