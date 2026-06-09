'use client'

import { KB_PRIMARY } from '@/lib/theme'

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  return (
    <html lang="ko">
      <body className="min-h-screen flex flex-col items-center justify-center bg-white gap-4">
        <h2 className="text-xl font-bold text-kb-text">오류가 발생했습니다.</h2>
        <p className="text-sm text-kb-text-muted">{error.message || '알 수 없는 오류입니다.'}</p>
        <button
          onClick={() => reset()}
          className="px-6 py-2 text-white font-semibold rounded-lg hover:opacity-85 transition-opacity"
          style={{ backgroundColor: KB_PRIMARY }}
        >
          다시 시도
        </button>
      </body>
    </html>
  )
}
