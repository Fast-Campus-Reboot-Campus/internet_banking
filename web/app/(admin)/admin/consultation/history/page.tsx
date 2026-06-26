'use client'

import { useEffect, useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import ConsultationTabs from '@/components/admin/ConsultationTabs'
import { getChatHistory, ChatHistoryItem } from '@/lib/consultation-api'

const fmt = (v: string | null) => (v ? v.slice(0, 16).replace('T', ' ') : '-')

const STATUS: Record<string, { label: string; cls: string }> = {
  WAITING:   { label: '대기중', cls: 'bg-amber-50 text-amber-700' },
  CONNECTED: { label: '상담중', cls: 'bg-green-100 text-green-700' },
  ENDED:     { label: '종료',   cls: 'bg-gray-100 text-gray-500' },
}

export default function ConsultationHistoryPage() {
  const [items, setItems] = useState<ChatHistoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')

  const load = async () => {
    setLoading(true); setError('')
    try { setItems(await getChatHistory({ limit: 200 })) }
    catch { setError('이력을 불러오지 못했습니다. 상담 서비스 상태를 확인하세요.') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const filtered = items.filter(it => {
    const matchStatus = statusFilter === 'ALL' || it.status === statusFilter
    const matchSearch = !search || it.customer_no.toLowerCase().includes(search.toLowerCase()) || String(it.chat_consultation_id).includes(search)
    return matchStatus && matchSearch
  })

  const scores = items.filter(i => i.satisfaction_score != null).map(i => i.satisfaction_score as number)
  const avgScore = scores.length ? (scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(1) : '-'

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">

        {/* 브레드크럼 */}
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          상담 &gt; <span className="text-gray-700 font-medium">상담 이력 조회</span>
        </div>

        {/* 탭 */}
        <ConsultationTabs />

        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">상담 이력 조회</h1>

          {/* 요약 카드 */}
          <div className="grid grid-cols-4 gap-3 mb-5">
            {[
              { label: '전체 상담',   value: items.length,                                       color: 'text-gray-800' },
              { label: '대기중',      value: items.filter(i => i.status === 'WAITING').length,   color: 'text-amber-600' },
              { label: '상담중',      value: items.filter(i => i.status === 'CONNECTED').length, color: 'text-green-700' },
              { label: '평균 만족도', value: scores.length ? `${avgScore} / 5` : '-',            color: 'text-kb-primary' },
            ].map(c => (
              <div key={c.label} className="bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
                <p className="text-xs text-kb-text-muted mb-1">{c.label}</p>
                <p className={`text-2xl font-bold ${c.color}`}>{c.value}</p>
              </div>
            ))}
          </div>

          {/* 필터 */}
          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <input
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="고객번호 또는 상담 ID"
              className="border border-gray-300 text-xs px-2 py-1.5 rounded w-44"
            />
            <select
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
              className="border border-gray-300 text-xs px-2 py-1.5 rounded bg-white"
            >
              <option value="ALL">전체 상태</option>
              <option value="WAITING">대기중</option>
              <option value="CONNECTED">상담중</option>
              <option value="ENDED">종료</option>
            </select>
            <button
              onClick={load}
              disabled={loading}
              className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors disabled:opacity-50"
            >
              {loading ? '조회 중…' : '조회'}
            </button>
          </div>

          {error && (
            <div className="mb-3 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>
          )}

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-gray-500">총 {filtered.length}건</p>
          </div>

          {/* 테이블 */}
          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['ID', '고객번호', '상태', '담당 상담원', '요청 시각', '연결 시각', '종료 시각', '메시지', '만족도'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {loading && (
                  <tr><td colSpan={9} className="px-3 py-8 text-center text-gray-400 text-sm">불러오는 중…</td></tr>
                )}
                {!loading && filtered.length === 0 && (
                  <tr><td colSpan={9} className="px-3 py-8 text-center text-gray-400 text-sm">조회 결과가 없습니다.</td></tr>
                )}
                {filtered.map(it => {
                  const s = STATUS[it.status] ?? { label: it.status, cls: 'bg-gray-100 text-gray-500' }
                  return (
                    <tr key={it.chat_consultation_id} className="hover:bg-kb-beige-light">
                      <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">#{it.chat_consultation_id}</td>
                      <td className="px-3 py-2.5 font-medium">{it.customer_no}</td>
                      <td className="px-3 py-2.5">
                        <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${s.cls}`}>{s.label}</span>
                      </td>
                      <td className="px-3 py-2.5 text-gray-500 text-xs">{it.employee_id ? `#${it.employee_id}` : '-'}</td>
                      <td className="px-3 py-2.5 text-xs text-gray-400 whitespace-nowrap">{fmt(it.agent_requested_at)}</td>
                      <td className="px-3 py-2.5 text-xs text-gray-400 whitespace-nowrap">{fmt(it.agent_connected_at)}</td>
                      <td className="px-3 py-2.5 text-xs text-gray-400 whitespace-nowrap">{fmt(it.chat_ended_at)}</td>
                      <td className="px-3 py-2.5 text-center text-xs text-gray-600">{it.message_count}</td>
                      <td className="px-3 py-2.5 text-center text-xs">
                        {it.satisfaction_score != null
                          ? <span className="text-amber-500">{'★'.repeat(it.satisfaction_score)}{'☆'.repeat(5 - it.satisfaction_score)}</span>
                          : <span className="text-gray-300">-</span>}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  )
}
