'use client'

import { useState, useEffect, useCallback } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { creditInfoReportApi } from '@/lib/loan-api'

const STATUS_MAP: Record<string, { text: string; cls: string }> = {
  SUCCESS: { text: '성공', cls: 'bg-green-100 text-green-700 border-green-300' },
  FAILED:  { text: '실패', cls: 'bg-red-100 text-red-700 border-red-300' },
  PENDING: { text: '대기', cls: 'bg-yellow-100 text-yellow-700 border-yellow-300' },
}

function StatusBadge({ status }: { status: string }) {
  const s = STATUS_MAP[status] ?? { text: status, cls: 'bg-gray-100 text-gray-500 border-gray-300' }
  return (
    <span className={`text-[11px] px-2 py-0.5 rounded border ${s.cls}`}>{s.text}</span>
  )
}

function formatDt(dt: string | null) {
  if (!dt) return '-'
  return dt.slice(0, 16).replace('T', ' ')
}

export default function AdminCreditReportPage() {
  const [reports, setReports] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState<number | null>(null)
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')

  function notify(m: string) { setMsg(m); setTimeout(() => setMsg(''), 3000) }
  function fail(m: string) { setErr(m); setTimeout(() => setErr(''), 3000) }

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const { data: res } = await creditInfoReportApi.list({ size: 50 })
      setReports(res.data ?? res ?? [])
    } catch { fail('목록을 불러오지 못했습니다.') }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  async function handleRetry(reportId: number) {
    setBusy(reportId)
    try {
      await creditInfoReportApi.retry(reportId)
      notify(`reportId ${reportId} 재시도 요청을 보냈습니다.`)
      await load()
    } catch (e: any) { fail(e?.response?.data?.message ?? '재시도 실패') }
    finally { setBusy(null) }
  }

  async function handleAck(reportId: number) {
    setBusy(reportId)
    try {
      await creditInfoReportApi.ack(reportId)
      notify(`reportId ${reportId} ACK 처리 완료.`)
      await load()
    } catch (e: any) { fail(e?.response?.data?.message ?? 'ACK 처리 실패') }
    finally { setBusy(null) }
  }

  return (
    <div className="flex min-h-screen bg-gray-50">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-gray-200 px-6 py-3 text-xs text-gray-500">
          대출 &gt; <span className="text-gray-800 font-medium">신용정보 보고서 관리</span>
        </div>
        <div className="px-6 py-5 max-w-5xl">
          <div className="flex items-center justify-between mb-5">
            <h1 className="text-lg font-bold text-gray-800">신용정보 보고서 관리</h1>
            {!loading && (
              <span className="text-[12px] text-gray-500">총 {reports.length}건</span>
            )}
          </div>

          {msg && <div className="mb-4 px-4 py-2 bg-green-50 border border-green-300 text-green-700 text-sm rounded">{msg}</div>}
          {err && <div className="mb-4 px-4 py-2 bg-red-50 border border-red-300 text-red-700 text-sm rounded">{err}</div>}

          <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
            {loading ? (
              <p className="py-10 text-center text-sm text-gray-400">불러오는 중...</p>
            ) : reports.length === 0 ? (
              <p className="py-10 text-center text-sm text-gray-400">보고서가 없습니다.</p>
            ) : (
              <table className="w-full text-[13px]">
                <thead>
                  <tr className="bg-gray-50 border-b border-gray-200">
                    {['reportId', 'applId', '상태', '외부기관', '요청일시', '응답일시', '처리'].map(h => (
                      <th key={h} className="px-4 py-3 text-left text-xs text-gray-600 font-semibold">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {reports.map((r: any) => (
                    <tr key={r.reportId} className="hover:bg-gray-50 transition-colors">
                      <td className="px-4 py-3 text-gray-400 text-xs">{r.reportId}</td>
                      <td className="px-4 py-3 font-mono font-bold text-gray-800">{r.applId}</td>
                      <td className="px-4 py-3"><StatusBadge status={r.statusCd} /></td>
                      <td className="px-4 py-3 text-gray-600">{r.agencyCd ?? '-'}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{formatDt(r.requestedAt)}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{formatDt(r.respondedAt)}</td>
                      <td className="px-4 py-3">
                        <div className="flex gap-2">
                          {r.statusCd === 'FAILED' && (
                            <button
                              onClick={() => handleRetry(r.reportId)}
                              disabled={busy === r.reportId}
                              className="px-3 py-1 text-[11px] bg-[#1B3A6B] text-white rounded hover:opacity-90 disabled:opacity-50"
                            >
                              재시도
                            </button>
                          )}
                          {r.statusCd === 'PENDING' && (
                            <button
                              onClick={() => handleAck(r.reportId)}
                              disabled={busy === r.reportId}
                              className="px-3 py-1 text-[11px] border border-blue-300 text-blue-600 rounded hover:bg-blue-50 disabled:opacity-50"
                            >
                              ACK 처리
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}
