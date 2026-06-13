'use client'
import { useCallback, useEffect, useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import {
  listScreeningPending, clearScreeningHit, confirmScreeningHit,
  SanctionHit, fmtYmd, fmtDateTime, errMsg,
} from '@/lib/admin-customer-api'

const HIT_COLOR: Record<string, string> = {
  OFAC_SDN: 'border-red-400 text-red-700 bg-red-50',
  KR_PEP: 'border-orange-400 text-orange-700 bg-orange-50',
  UN: 'border-purple-400 text-purple-700 bg-purple-50',
  EU: 'border-blue-400 text-blue-700 bg-blue-50',
}

export default function ScreeningPage() {
  const [rows, setRows] = useState<SanctionHit[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    listScreeningPending(0, 50)
      .then(res => { setRows(res.content); setTotal(res.totalElements) })
      .catch(e => setError(errMsg(e, '제재 스크리닝 대기 목록을 불러오지 못했습니다.')))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  async function act(hitId: number, kind: 'clear' | 'confirm') {
    setBusy(hitId)
    setError(null)
    try {
      if (kind === 'clear') await clearScreeningHit(hitId)
      else await confirmScreeningHit(hitId)
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
          심사 &gt; <span className="text-kb-text font-semibold">제재대상 Hit 검토</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-h2 font-bold text-kb-text mb-4">제재대상 스크리닝 Hit 검토</h1>

          <p className="text-xs text-kb-text-muted mb-2">검토 대기 <span className="font-semibold text-kb-text">{total.toLocaleString()}건</span>{loading && ' · 조회 중…'}</p>

          {error && <div className="mb-3 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>}

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['Party ID', '이름', '생년월일', '국적', 'Hit 유형', '일치율', '탐지일시', '작업'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-semibold">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-kb-border">
                {rows.map(r => (
                  <tr key={r.sanctionScreeningHitId} className="hover:bg-kb-beige-light transition-colors">
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.partyId}</td>
                    <td className="px-3 py-2.5 font-medium text-kb-text">{r.partyName}</td>
                    <td className="px-3 py-2.5 text-xs text-kb-text-muted">{fmtYmd(r.birthDate)}</td>
                    <td className="px-3 py-2.5 text-kb-text-body">{r.nationalityCode ?? '-'}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs border px-1.5 py-0.5 rounded-sm font-medium ${HIT_COLOR[r.hitTypeCode] ?? 'border-gray-300 text-gray-600 bg-gray-50'}`}>{r.hitTypeCode}</span>
                    </td>
                    <td className="px-3 py-2.5 font-semibold text-kb-text">{r.matchRate}%</td>
                    <td className="px-3 py-2.5 text-xs text-kb-text-muted">{fmtDateTime(r.detectedAt)}</td>
                    <td className="px-3 py-2.5 whitespace-nowrap">
                      <button onClick={() => act(r.sanctionScreeningHitId, 'clear')} disabled={busy === r.sanctionScreeningHitId}
                        className="text-xs border border-green-400 text-green-700 px-2 py-0.5 rounded mr-1 hover:bg-green-50 disabled:opacity-50">승인(동명이인)</button>
                      <button onClick={() => act(r.sanctionScreeningHitId, 'confirm')} disabled={busy === r.sanctionScreeningHitId}
                        className="text-xs border border-red-400 text-red-600 px-2 py-0.5 rounded hover:bg-red-50 disabled:opacity-50">거절(제재확정)</button>
                    </td>
                  </tr>
                ))}
                {!loading && rows.length === 0 && !error && (
                  <tr><td colSpan={8} className="px-3 py-8 text-center text-gray-400 text-sm">검토 대기 Hit이 없습니다.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  )
}
