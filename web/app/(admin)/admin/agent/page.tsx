'use client'
import { useCallback, useEffect, useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import {
  listAgentReviewPending, approveAgentReview, rejectAgentReview,
  AgentReview, fmtYmd, errMsg,
} from '@/lib/admin-customer-api'

export default function AgentPage() {
  const [rows, setRows] = useState<AgentReview[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    listAgentReviewPending(0, 50)
      .then(res => { setRows(res.content); setTotal(res.totalElements) })
      .catch(e => setError(errMsg(e, '대리인 검토 대기 목록을 불러오지 못했습니다.')))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  async function act(relationId: number, kind: 'approve' | 'reject') {
    setBusy(relationId)
    setError(null)
    try {
      if (kind === 'approve') await approveAgentReview(relationId)
      else await rejectAgentReview(relationId)
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
          심사 &gt; <span className="text-gray-700 font-medium">대리인 검토</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">대리인 위임장 검토</h1>

          <p className="text-xs text-gray-500 mb-2">대리인 검토 대기 {total.toLocaleString()}건{loading && ' · 조회 중…'}</p>

          {error && <div className="mb-3 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>}

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['관계 ID', '본인 Party', '대리인 이름', '관계유형', '위임 범위', '서류', '접수일', '작업'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map(r => (
                  <tr key={r.relationId} className="hover:bg-kb-beige-light">
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.relationId}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{r.ownerPartyId}</td>
                    <td className="px-3 py-2.5 font-medium">{r.agentName}</td>
                    <td className="px-3 py-2.5 text-gray-600 text-xs">{r.relationTypeCode}</td>
                    <td className="px-3 py-2.5 text-gray-600 text-xs">{r.representationScope ?? '-'}</td>
                    <td className="px-3 py-2.5 text-xs">
                      {r.proofUrl
                        ? <a href={r.proofUrl} target="_blank" rel="noreferrer" className="text-blue-600 underline">위임장</a>
                        : <span className="text-gray-300">-</span>}
                    </td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{fmtYmd(r.relationStartDate)}</td>
                    <td className="px-3 py-2.5 whitespace-nowrap">
                      <button onClick={() => act(r.relationId, 'approve')} disabled={busy === r.relationId}
                        className="text-xs bg-kb-yellow text-white px-2 py-0.5 rounded mr-1 font-medium hover:bg-kb-yellow-dark disabled:opacity-50">승인</button>
                      <button onClick={() => act(r.relationId, 'reject')} disabled={busy === r.relationId}
                        className="text-xs border border-red-400 text-red-600 px-2 py-0.5 rounded hover:bg-red-50 disabled:opacity-50">거절</button>
                    </td>
                  </tr>
                ))}
                {!loading && rows.length === 0 && !error && (
                  <tr><td colSpan={8} className="px-3 py-8 text-center text-gray-400 text-sm">검토 대기 위임건이 없습니다.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  )
}
