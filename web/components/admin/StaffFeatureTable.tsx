'use client'

/**
 * 상담 직원용 고객조회 결과 표.
 * consultation-service의 STAFF_* feature 응답(행 배열)을 컬럼 정의대로 렌더링한다.
 * 5종(고객정보·계약·계좌·이체흐름·상담이력)이 컬럼만 다르므로 이 컴포넌트 하나로 공유한다.
 */

export type StaffColumn = {
  key: string
  label: string
  align?: 'left' | 'right'
  format?: (value: unknown) => string
}

export default function StaffFeatureTable({
  columns,
  rows,
  loading,
  notice,
  emptyMsg,
}: {
  columns: StaffColumn[]
  rows: Record<string, unknown>[]
  loading: boolean
  notice?: string | null
  emptyMsg: string
}) {
  if (loading) return <p className="py-10 text-center text-sm text-gray-400">불러오는 중...</p>
  if (notice)  return <p className="py-10 text-center text-sm text-gray-400">{notice}</p>
  if (!rows.length) return <p className="py-10 text-center text-sm text-gray-400">{emptyMsg}</p>

  return (
    <div className="bg-white border border-gray-200 rounded-lg overflow-hidden overflow-x-auto">
      <table className="w-full text-[13px]">
        <thead>
          <tr className="bg-gray-50 border-b border-gray-200">
            {columns.map(c => (
              <th key={c.key}
                className={`px-4 py-3 font-semibold text-gray-600 ${c.align === 'right' ? 'text-right' : 'text-left'}`}>
                {c.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {rows.map((row, i) => (
            <tr key={i} className="hover:bg-gray-50 transition-colors">
              {columns.map(c => {
                const raw = row[c.key]
                const text = c.format ? c.format(raw) : raw == null || raw === '' ? '-' : String(raw)
                return (
                  <td key={c.key}
                    className={`px-4 py-3 text-gray-700 ${c.align === 'right' ? 'text-right font-medium' : 'text-left'}`}>
                    {text}
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
