'use client'
import { useEffect, useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { listMinors, Minor, fmtYmd, errMsg } from '@/lib/admin-customer-api'

/** YYYYMMDD 생년월일 → 만 나이 */
function ageOf(birthYmd: string | null): string {
  if (!birthYmd || birthYmd.length !== 8) return '-'
  const y = +birthYmd.slice(0, 4), m = +birthYmd.slice(4, 6), d = +birthYmd.slice(6, 8)
  const today = new Date()
  let age = today.getFullYear() - y
  if (today.getMonth() + 1 < m || (today.getMonth() + 1 === m && today.getDate() < d)) age--
  return `만 ${age}세`
}

export default function MinorPage() {
  const [rows, setRows] = useState<Minor[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    listMinors(0, 50)
      .then(res => { setRows(res.content); setTotal(res.totalElements) })
      .catch(e => setError(errMsg(e, '미성년 목록을 불러오지 못했습니다.')))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          심사 &gt; <span className="text-gray-700 font-medium">미성년 검토</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">미성년자(만 19세 미만) 검토</h1>

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-gray-500">미성년 가입 {total.toLocaleString()}건{loading && ' · 조회 중…'}</p>
          </div>

          {error && <div className="mb-3 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>}

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['Party ID', '이름', '생년월일', '나이', '성별', '국적', '법정대리인 관계'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map(r => (
                  <tr key={r.partyId} className="hover:bg-kb-beige-light">
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.partyId}</td>
                    <td className="px-3 py-2.5 font-medium">{r.partyName}</td>
                    <td className="px-3 py-2.5 text-gray-500">{fmtYmd(r.birthDate)}</td>
                    <td className="px-3 py-2.5 text-gray-600">{ageOf(r.birthDate)}</td>
                    <td className="px-3 py-2.5 text-gray-600">{r.genderCode === 'M' ? '남' : r.genderCode === 'F' ? '여' : '-'}</td>
                    <td className="px-3 py-2.5 text-gray-600">{r.nationalityCode ?? '-'}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">관계 조회 별도</td>
                  </tr>
                ))}
                {!loading && rows.length === 0 && !error && (
                  <tr><td colSpan={7} className="px-3 py-8 text-center text-gray-400 text-sm">미성년 대상이 없습니다.</td></tr>
                )}
              </tbody>
            </table>
          </div>
          <p className="mt-3 text-xs text-gray-400">※ 검토상태(승인/거절) 워크플로와 법정대리인 관계 검증은 추후 연동됩니다(현재 목록 조회만).</p>
        </div>
      </main>
    </div>
  )
}
