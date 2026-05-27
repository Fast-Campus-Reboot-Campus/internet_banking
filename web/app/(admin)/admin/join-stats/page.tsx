'use client'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { JOIN_STATS } from '@/lib/admin-mock-data'

export default function JoinStatsPage() {
  const { todayApplications, todayCompleted, completionRate, pendingReview, rejectionRate, funnel, channels, alerts } = JOIN_STATS

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
            <span className="text-xs text-gray-400">기준: 2026-05-25 실시간</span>
          </div>

          {/* 알림 */}
          {alerts.length > 0 && (
            <div className="mb-4 bg-orange-50 border border-orange-200 rounded p-3 space-y-1">
              {alerts.map((a, i) => (
                <p key={i} className="text-xs text-orange-700">{a}</p>
              ))}
            </div>
          )}

          {/* KPI 카드 */}
          <div className="grid grid-cols-5 gap-4 mb-5">
            {[
              { label: '오늘 신청', value: todayApplications.toLocaleString(), unit: '건', color: 'text-gray-800' },
              { label: '가입 완료', value: todayCompleted.toLocaleString(), unit: '건', color: 'text-green-600' },
              { label: '완료율', value: completionRate.toFixed(1), unit: '%', color: 'text-blue-600' },
              { label: '검토 대기', value: pendingReview.toLocaleString(), unit: '건', color: 'text-orange-600' },
              { label: '거절률', value: rejectionRate.toFixed(1), unit: '%', color: 'text-red-600' },
            ].map(card => (
              <div key={card.label} className="bg-white border border-kb-border rounded-lg p-4 text-center shadow-sm">
                <p className="text-xs text-gray-400 mb-1">{card.label}</p>
                <p className={`text-2xl font-bold ${card.color}`}>
                  {card.value}<span className="text-sm font-normal ml-1">{card.unit}</span>
                </p>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-3 gap-4">
            {/* 가입 퍼널 */}
            <div className="col-span-2 bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
              <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">가입 단계별 퍼널</div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 text-xs text-gray-500">
                    {['단계','진입','완료','이탈률','주 이탈 원인'].map(h => (
                      <th key={h} className="px-3 py-2 text-left font-medium">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {funnel.map((row, i) => (
                    <tr key={i} className="hover:bg-kb-beige-light">
                      <td className="px-3 py-2.5 font-medium text-xs">{row.step}</td>
                      <td className="px-3 py-2.5 text-xs text-gray-600">{row.entered.toLocaleString()}</td>
                      <td className="px-3 py-2.5 text-xs text-gray-600">{row.completed.toLocaleString()}</td>
                      <td className="px-3 py-2.5">
                        <span className={`text-xs font-medium ${row.dropRate > 6 ? 'text-red-600' : row.dropRate > 3 ? 'text-orange-600' : 'text-gray-600'}`}>
                          {row.dropRate}%
                        </span>
                      </td>
                      <td className="px-3 py-2.5 text-xs text-gray-400">{row.mainReason}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* 채널별 + 추가 정보 */}
            <div className="space-y-4">
              <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
                <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">채널별 가입</div>
                <div className="p-3 space-y-3">
                  {channels.map(ch => (
                    <div key={ch.channel}>
                      <div className="flex justify-between text-xs mb-1">
                        <span className="text-gray-700">{ch.channel}</span>
                        <span className="text-gray-500">{ch.count.toLocaleString()}건 ({ch.rate}%)</span>
                      </div>
                      <div className="w-full bg-gray-100 rounded-full h-2">
                        <div
                          className="bg-kb-yellow h-2 rounded-full"
                          style={{ width: `${ch.rate}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="bg-white border border-kb-border rounded-lg p-4 shadow-sm">
                <p className="text-xs font-medium text-gray-600 mb-3">심사 현황</p>
                <div className="space-y-2">
                  {[
                    { label: '제재 스크리닝 대기', value: '5건', color: 'text-red-600' },
                    { label: 'EDD 심사 대기', value: '8건', color: 'text-orange-600' },
                    { label: '증표 위변조 검토', value: '3건', color: 'text-orange-600' },
                    { label: '얼굴인증 라우팅 대기', value: '1건', color: 'text-yellow-600' },
                    { label: '미성년자 검토 대기', value: '2건', color: 'text-blue-600' },
                  ].map(item => (
                    <div key={item.label} className="flex justify-between text-xs">
                      <span className="text-gray-500">{item.label}</span>
                      <span className={`font-medium ${item.color}`}>{item.value}</span>
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
