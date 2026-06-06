'use client'
import { useEffect, useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import {
  getJoinStats, JoinStats, STATUS_LABEL,
  listEddPending, listScreeningPending, listMinors, listDuplicatesPending, listAgentReviewPending,
  errMsg,
} from '@/lib/admin-customer-api'

type QueueCounts = { edd: number; screening: number; minor: number; duplicate: number; agent: number }

export default function JoinStatsPage() {
  const [stats, setStats] = useState<JoinStats | null>(null)
  const [queues, setQueues] = useState<QueueCounts | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getJoinStats().then(setStats).catch(e => setError(errMsg(e, '가입 통계를 불러오지 못했습니다.')))
    // 심사 현황 카드: 각 큐의 totalElements 만 사용 (size=1)
    Promise.all([
      listEddPending(0, 1), listScreeningPending(0, 1), listMinors(0, 1),
      listDuplicatesPending(0, 1), listAgentReviewPending(0, 1),
    ]).then(([edd, scr, mnr, dup, agt]) => setQueues({
      edd: edd.totalElements, screening: scr.totalElements, minor: mnr.totalElements,
      duplicate: dup.totalElements, agent: agt.totalElements,
    })).catch(() => { /* 큐 카운트는 선택적 — 실패 무시 */ })
  }, [])

  const maxChannel = Math.max(1, ...(stats?.byChannel.map(c => c.count) ?? [1]))

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          모니터링 &gt; <span className="text-gray-700 font-medium">가입 현황 대시보드</span>
        </div>
        <div className="px-6 py-5">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-lg font-bold text-gray-800">가입 현황 대시보드</h1>
            <span className="text-xs text-gray-400">customer 집계 기준</span>
          </div>

          {error && <div className="mb-4 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>}

          {/* KPI 카드 */}
          <div className="grid grid-cols-3 gap-4 mb-5">
            {[
              { label: '총 회원', value: stats?.total, color: 'text-gray-800' },
              { label: '오늘 가입', value: stats?.joinedToday, color: 'text-green-600' },
              { label: '이번달 가입', value: stats?.joinedThisMonth, color: 'text-blue-600' },
            ].map(card => (
              <div key={card.label} className="bg-white border border-kb-border rounded-lg p-4 text-center shadow-sm">
                <p className="text-xs text-gray-400 mb-1">{card.label}</p>
                <p className={`text-2xl font-bold ${card.color}`}>
                  {card.value?.toLocaleString() ?? '–'}<span className="text-sm font-normal ml-1">명</span>
                </p>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-3 gap-4">
            {/* 상태별 분포 */}
            <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
              <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">회원 상태별</div>
              <div className="p-3 space-y-2">
                {(stats?.byStatus ?? []).map(s => (
                  <div key={s.code ?? 'null'} className="flex justify-between text-xs">
                    <span className="text-gray-600">{s.code ? (STATUS_LABEL[s.code] ?? s.code) : '미설정'}</span>
                    <span className="font-medium text-gray-800">{s.count.toLocaleString()}명</span>
                  </div>
                ))}
                {!stats && <p className="text-xs text-gray-300">불러오는 중…</p>}
              </div>
            </div>

            {/* 채널별 분포 */}
            <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
              <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">가입 채널별</div>
              <div className="p-3 space-y-3">
                {(stats?.byChannel ?? []).map(ch => (
                  <div key={ch.code ?? 'null'}>
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-gray-700">{ch.code ?? '미설정'}</span>
                      <span className="text-gray-500">{ch.count.toLocaleString()}명</span>
                    </div>
                    <div className="w-full bg-gray-100 rounded-full h-2">
                      <div className="bg-kb-yellow h-2 rounded-full" style={{ width: `${(ch.count / maxChannel) * 100}%` }} />
                    </div>
                  </div>
                ))}
                {!stats && <p className="text-xs text-gray-300">불러오는 중…</p>}
              </div>
            </div>

            {/* 등급별 + 심사 현황 */}
            <div className="space-y-4">
              <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
                <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">등급별</div>
                <div className="p-3 space-y-2">
                  {(stats?.byGrade ?? []).map(g => (
                    <div key={g.code ?? 'null'} className="flex justify-between text-xs">
                      <span className="text-gray-600">{g.code ?? '미설정'}</span>
                      <span className="font-medium text-gray-800">{g.count.toLocaleString()}명</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="bg-white border border-kb-border rounded-lg p-4 shadow-sm">
                <p className="text-xs font-medium text-gray-600 mb-3">심사 대기 현황</p>
                <div className="space-y-2">
                  {[
                    { label: '제재 스크리닝', value: queues?.screening, color: 'text-red-600' },
                    { label: 'EDD 심사', value: queues?.edd, color: 'text-orange-600' },
                    { label: '대리인 검토', value: queues?.agent, color: 'text-blue-600' },
                    { label: '중복고객 검토', value: queues?.duplicate, color: 'text-purple-600' },
                    { label: '미성년 검토', value: queues?.minor, color: 'text-green-600' },
                  ].map(item => (
                    <div key={item.label} className="flex justify-between text-xs">
                      <span className="text-gray-500">{item.label}</span>
                      <span className={`font-medium ${item.color}`}>{item.value?.toLocaleString() ?? '–'}건</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
