'use client'
import { useCallback, useEffect, useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import {
  listDuplicatesPending, markDuplicate, markDistinct,
  DuplicateReview, fmtDate, errMsg,
} from '@/lib/admin-customer-api'

const MATCH_LABEL: Record<string, string> = { CI: 'CI 충돌', NAME_BIRTH: '이름+생년월일' }

export default function DuplicatesPage() {
  const [rows, setRows] = useState<DuplicateReview[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    listDuplicatesPending(0, 50)
      .then(res => { setRows(res.content); setTotal(res.totalElements) })
      .catch(e => setError(errMsg(e, '중복 검토 대기 목록을 불러오지 못했습니다.')))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  async function act(caseId: number, kind: 'duplicate' | 'distinct') {
    setBusy(caseId)
    setError(null)
    try {
      if (kind === 'duplicate') await markDuplicate(caseId)
      else await markDistinct(caseId)
      load()
    } catch (e) {
      setError(errMsg(e, '처리에 실패했습니다.'))
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          심사 &gt; <span className="text-gray-700 font-medium">중복고객 검토</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">중복 고객 의심건 검토</h1>

          <p className="text-xs text-gray-500 mb-2">중복 의심 {total.toLocaleString()}건{loading && ' · 조회 중…'}</p>

          {error && <div className="mb-3 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>}

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['케이스 ID', '신규 Party', '기존 Party', '일치 유형', '발생일', '작업'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map(r => (
                  <tr key={r.duplicateReviewCaseId} className="hover:bg-kb-beige-light">
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.duplicateReviewCaseId}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-600">{r.newPartyName} <span className="text-gray-400">({r.newPartyId})</span></td>
                    <td className="px-3 py-2.5 text-xs text-gray-600">{r.existingPartyName} <span className="text-gray-400">({r.existingPartyId})</span></td>
                    <td className="px-3 py-2.5"><span className="text-xs px-1.5 py-0.5 rounded-full bg-orange-100 text-orange-700 font-medium">{MATCH_LABEL[r.matchTypeCode] ?? r.matchTypeCode}</span></td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{fmtDate(r.detectedAt)}</td>
                    <td className="px-3 py-2.5 whitespace-nowrap">
                      <button onClick={() => act(r.duplicateReviewCaseId, 'duplicate')} disabled={busy === r.duplicateReviewCaseId}
                        className="text-xs border border-red-400 text-red-600 px-2 py-0.5 rounded mr-1 hover:bg-red-50 disabled:opacity-50">복본 확정</button>
                      <button onClick={() => act(r.duplicateReviewCaseId, 'distinct')} disabled={busy === r.duplicateReviewCaseId}
                        className="text-xs border border-gray-300 text-gray-700 px-2 py-0.5 rounded hover:bg-gray-50 disabled:opacity-50">별개(동명이인)</button>
                    </td>
                  </tr>
                ))}
                {!loading && rows.length === 0 && !error && (
                  <tr><td colSpan={6} className="px-3 py-8 text-center text-gray-400 text-sm">검토 대기 중복 의심건이 없습니다.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  )
}
