'use client'

import { useEffect, useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import ConsultationTabs from '@/components/admin/ConsultationTabs'
import { getChatHistory, ChatHistoryItem, listAgents, AgentAccount } from '@/lib/consultation-api'

function BarRow({ label, value, max, color }: { label: string; value: number; max: number; color: string }) {
  const pct = max ? Math.round((value / max) * 100) : 0
  return (
    <div className="flex items-center gap-3">
      <span className="text-xs text-kb-text-muted w-14 shrink-0">{label}</span>
      <div className="flex-1 bg-gray-100 rounded-full h-2 overflow-hidden">
        <div className={`h-2 rounded-full ${color}`} style={{ width: `${pct}%` }} />
      </div>
      <span className="text-xs text-gray-600 w-5 text-right shrink-0">{value}</span>
    </div>
  )
}

function agentStats(items: ChatHistoryItem[]) {
  const ended = items.filter(i => i.status === 'ENDED')
  const scored = items.filter(i => i.satisfaction_score != null)
  const avgScore = scored.length
    ? (scored.reduce((a, b) => a + (b.satisfaction_score ?? 0), 0) / scored.length).toFixed(1)
    : null
  const scoreDist = [5, 4, 3, 2, 1].map(s => ({
    label: '★'.repeat(s),
    count: scored.filter(i => i.satisfaction_score === s).length,
  }))
  const durations = ended
    .filter(i => i.agent_connected_at && i.chat_ended_at)
    .map(i => Math.round((new Date(i.chat_ended_at!).getTime() - new Date(i.agent_connected_at!).getTime()) / 60000))
  const avgDuration = durations.length
    ? (durations.reduce((a, b) => a + b, 0) / durations.length).toFixed(1) : null
  const waitTimes = items
    .filter(i => i.agent_requested_at && i.agent_connected_at)
    .map(i => Math.round((new Date(i.agent_connected_at!).getTime() - new Date(i.agent_requested_at!).getTime()) / 1000))
  const avgWait = waitTimes.length
    ? (waitTimes.reduce((a, b) => a + b, 0) / waitTimes.length).toFixed(0) : null
  return { ended, scored, avgScore, scoreDist, avgDuration, durations, avgWait, waitTimes }
}

export default function ConsultationStatsPage() {
  const [items, setItems] = useState<ChatHistoryItem[]>([])
  const [agents, setAgents] = useState<AgentAccount[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([getChatHistory({ limit: 500 }), listAgents()])
      .then(([h, a]) => { setItems(h); setAgents(a) })
      .catch(() => setError('통계를 불러오지 못했습니다. 상담 서비스 상태를 확인하세요.'))
      .finally(() => setLoading(false))
  }, [])

  const overall = agentStats(items)

  const today = new Date()
  const dailyCounts = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(today)
    d.setDate(today.getDate() - (6 - i))
    const dateStr = d.toISOString().slice(0, 10)
    return { date: dateStr.slice(5), count: items.filter(it => (it.agent_requested_at ?? '').startsWith(dateStr)).length }
  })
  const maxDaily = Math.max(...dailyCounts.map(d => d.count), 1)

  const kpis = [
    { label: '총 상담 수',  value: items.length,                                        color: 'text-gray-800' },
    { label: '종료 완료',   value: overall.ended.length,                                color: 'text-kb-primary' },
    { label: '상담중',      value: items.filter(i => i.status === 'CONNECTED').length,  color: 'text-green-700' },
    { label: '대기중',      value: items.filter(i => i.status === 'WAITING').length,    color: 'text-amber-600' },
    { label: '평균 만족도', value: overall.avgScore ? `${overall.avgScore}점` : '-',    color: 'text-kb-primary' },
  ]

  const activeAgents = agents.filter(a => a.status === 'ACTIVE')

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">

        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          상담 &gt; <span className="text-gray-700 font-medium">만족도 / 통계</span>
        </div>

        <ConsultationTabs />

        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">만족도 / 통계 대시보드</h1>

          {error && (
            <div className="mb-3 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>
          )}
          {loading ? (
            <p className="py-20 text-center text-sm text-gray-400">불러오는 중…</p>
          ) : (
            <>
              {/* 전체 KPI */}
              <div className="grid grid-cols-5 gap-3 mb-5">
                {kpis.map(c => (
                  <div key={c.label} className="bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
                    <p className="text-xs text-kb-text-muted mb-1">{c.label}</p>
                    <p className={`text-2xl font-bold ${c.color}`}>{c.value}</p>
                  </div>
                ))}
              </div>

              {/* 전체 일별 건수 */}
              <div className="bg-white border border-kb-border rounded-lg p-5 shadow-sm mb-5">
                <p className="text-sm font-bold text-gray-700 mb-4">최근 7일 상담 건수 (전체)</p>
                <div className="space-y-3">
                  {dailyCounts.map(d => (
                    <BarRow key={d.date} label={d.date} value={d.count} max={maxDaily} color="bg-kb-primary" />
                  ))}
                </div>
              </div>

              {/* 상담사별 카드 */}
              <h2 className="text-sm font-bold text-gray-700 mb-3">상담사별 만족도 / 통계</h2>
              <div className="grid grid-cols-2 gap-4">
                {activeAgents.map(agent => {
                  const agentItems = items.filter(i => i.employee_id === agent.employee_id)
                  const s = agentStats(agentItems)
                  const maxScore = Math.max(...s.scoreDist.map(x => x.count), 1)
                  return (
                    <div key={agent.employee_id} className="bg-white border border-kb-border rounded-lg p-5 shadow-sm">
                      <div className="flex items-center justify-between mb-4">
                        <div>
                          <span className="text-sm font-bold text-gray-800">{agent.name}</span>
                          <span className="ml-2 text-[10px] text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded">{agent.role}</span>
                        </div>
                        <span className="text-xs text-gray-400">총 {agentItems.length}건</span>
                      </div>

                      {/* KPI */}
                      <div className="grid grid-cols-4 gap-2 mb-4">
                        <div className="text-center">
                          <p className="text-[10px] text-gray-400">종료</p>
                          <p className="text-lg font-bold text-kb-primary">{s.ended.length}</p>
                        </div>
                        <div className="text-center">
                          <p className="text-[10px] text-gray-400">평균만족도</p>
                          <p className="text-lg font-bold text-amber-500">{s.avgScore ? `${s.avgScore}점` : '-'}</p>
                        </div>
                        <div className="text-center">
                          <p className="text-[10px] text-gray-400">평균소요</p>
                          <p className="text-lg font-bold text-gray-700">{s.avgDuration ? `${s.avgDuration}분` : '-'}</p>
                        </div>
                        <div className="text-center">
                          <p className="text-[10px] text-gray-400">평균대기</p>
                          <p className="text-lg font-bold text-gray-700">{s.avgWait ? `${s.avgWait}초` : '-'}</p>
                        </div>
                      </div>

                      {/* 만족도 분포 */}
                      {s.scored.length === 0 ? (
                        <p className="text-center text-xs text-gray-400 py-3">만족도 응답 없음</p>
                      ) : (
                        <div className="space-y-2">
                          {s.scoreDist.map(x => (
                            <BarRow key={x.label} label={x.label} value={x.count} max={maxScore} color="bg-kb-yellow" />
                          ))}
                        </div>
                      )}
                    </div>
                  )
                })}
              </div>
            </>
          )}
        </div>
      </main>
    </div>
  )
}
