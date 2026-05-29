'use client'

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-white gap-4">
      <h2 className="text-xl font-bold text-kb-text">오류가 발생했습니다.</h2>
      <p className="text-sm text-kb-text-muted">{error.message || '알 수 없는 오류입니다.'}</p>
      <button
        onClick={() => reset()}
        className="px-6 py-2 bg-kb-yellow text-kb-text font-semibold hover:bg-kb-yellow-dark transition-colors"
      >
        다시 시도
      </button>
    </div>
  )
}
