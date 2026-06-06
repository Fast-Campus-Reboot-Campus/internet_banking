'use client'
import { useEffect, useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { listEddPending, EddPending, fmtYmd, errMsg } from '@/lib/admin-customer-api'

const RISK_COLOR: Record<string, string> = {
  HIGH: 'bg-red-100 text-red-700',
  MEDIUM: 'bg-yellow-100 text-yellow-700',
  LOW: 'bg-green-100 text-green-700',
}

export default function EDDPage() {
  const [rows, setRows] = useState<EddPending[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    listEddPending(0, 50)
      .then(res => { setRows(res.content); setTotal(res.totalElements) })
      .catch(e => setError(errMsg(e, 'EDD 대기 목록을 불러오지 못했습니다.')))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          심사 &gt; <span className="text-gray-700 font-medium">EDD 심사·승인</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">EDD 심사·승인 (강화된 고객확인)</h1>

          <p className="text-xs text-gray-500 mb-2">EDD 심사 대기 {total.toLocaleString()}건{loading && ' · 조회 중…'}</p>

          {error && <div className="mb-3 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>}

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['Party ID', '이름', 'AML 위험등급', 'CDD 등급', 'KYC 상태', 'EDD 차기검토일'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map(r => (
                  <tr key={r.partyId} className="hover:bg-kb-beige-light">
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.partyId}</td>
                    <td className="px-3 py-2.5 font-medium">{r.partyName}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${RISK_COLOR[r.amlRiskLevelCode] ?? 'bg-gray-100 text-gray-500'}`}>{r.amlRiskLevelCode}</span>
                    </td>
                    <td className="px-3 py-2.5 text-gray-600 text-xs">{r.cddLevelCode}</td>
                    <td className="px-3 py-2.5 text-gray-600 text-xs">{r.kycStatusCode}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{fmtYmd(r.eddNextReviewDate)}</td>
                  </tr>
                ))}
                {!loading && rows.length === 0 && !error && (
                  <tr><td colSpan={6} className="px-3 py-8 text-center text-gray-400 text-sm">EDD 심사 대기 건이 없습니다.</td></tr>
                )}
              </tbody>
            </table>
          </div>
          <p className="mt-3 text-xs text-gray-400">※ 자금원천·실소유자·제출서류·승인/반려 워크플로는 별도 연동 예정(현재 edd_required 대기 목록 조회).</p>
        </div>
      </main>
    </div>
  )
}
