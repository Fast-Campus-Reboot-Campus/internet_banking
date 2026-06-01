'use client'

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
        <h2 className="text-xl font-bold">오류가 발생했습니다.</h2>
        <p className="text-sm text-gray-500">{error.message || '알 수 없는 오류입니다.'}</p>
        <button
          onClick={() => reset()}
          className="px-6 py-2 bg-yellow-400 font-semibold hover:bg-yellow-500 transition-colors"
        >
          다시 시도
        </button>
      </body>
    </html>
  )
}
