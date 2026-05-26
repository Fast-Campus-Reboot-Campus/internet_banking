'use client'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MARKETING_STATS } from '@/lib/admin-mock-data'

export default function MarketingStatsPage() {
  const { totalSent, avgOpenRate, avgConversionRate, optOutRate, channels, topEvents, consent } = MARKETING_STATS

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          마케팅 &gt; <span className="text-gray-700 font-medium">마케팅 통계</span>
        </div>
        <div className="px-6 py-5">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-lg font-bold text-gray-800">마케팅 통계</h1>
            <div className="flex items-center gap-2 text-xs text-gray-400">
              <span>기간:</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                <option>최근 30일</option><option>최근 7일</option><option>이번 달</option><option>직접 선택</option>
              </select>
              <button className="px-3 py-1 border border-gray-300 rounded text-gray-600">엑셀 다운로드</button>
            </div>
          </div>

          {/* KPI 카드 */}
          <div className="grid grid-cols-4 gap-4 mb-5">
            {[
              { label: '총 발송 건수', value: (totalSent / 10000).toFixed(0) + '만', unit: '건', color: 'text-gray-800' },
              { label: '평균 오픈율', value: avgOpenRate.toFixed(1), unit: '%', color: 'text-blue-600' },
              { label: '평균 전환율', value: avgConversionRate.toFixed(1), unit: '%', color: 'text-green-600' },
              { label: '수신 거부율', value: optOutRate.toFixed(1), unit: '%', color: 'text-red-600' },
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
            {/* 채널별 성과 */}
            <div className="col-span-2 bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
              <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">채널별 발송 성과</div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 text-xs text-gray-500">
                    {['채널','발송 건수','도달률','오픈/클릭률','전환율'].map(h => (
                      <th key={h} className="px-4 py-2.5 text-left font-medium">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {channels.map(ch => (
                    <tr key={ch.channel} className="hover:bg-kb-beige-light">
                      <td className="px-4 py-3 font-medium">{ch.channel}</td>
                      <td className="px-4 py-3 text-gray-600">{ch.sent.toLocaleString()}건</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <div className="w-16 bg-gray-100 rounded-full h-1.5">
                            <div className="bg-blue-400 h-1.5 rounded-full" style={{ width: `${ch.deliveryRate}%` }} />
                          </div>
                          <span className="text-xs text-gray-600">{ch.deliveryRate}%</span>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <div className="w-16 bg-gray-100 rounded-full h-1.5">
                            <div className="bg-green-400 h-1.5 rounded-full" style={{ width: `${ch.openClickRate}%` }} />
                          </div>
                          <span className="text-xs text-gray-600">{ch.openClickRate}%</span>
                        </div>
                      </td>
                      <td className="px-4 py-3 font-medium text-green-600">{ch.conversionRate}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* 동의 현황 */}
            <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
              <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">마케팅 동의 현황</div>
              <div className="p-4 space-y-4">
                {consent.map(c => (
                  <div key={c.label}>
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-gray-700">{c.label}</span>
                      <span className="text-gray-500">{c.count.toLocaleString()}명 ({c.rate}%)</span>
                    </div>
                    <div className="w-full bg-gray-100 rounded-full h-2">
                      <div className="bg-kb-yellow h-2 rounded-full" style={{ width: `${c.rate}%` }} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* 이벤트별 성과 */}
          <div className="mt-4 bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">이벤트별 응모 성과 TOP 5</div>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 text-xs text-gray-500">
                  {['순위','이벤트명','응모자 수','전환율'].map(h => (
                    <th key={h} className="px-4 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {topEvents.map((e, i) => (
                  <tr key={e.name} className="hover:bg-kb-beige-light">
                    <td className="px-4 py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-bold ${i === 0 ? 'bg-yellow-200 text-yellow-800' : 'bg-gray-100 text-gray-600'}`}>
                        {i + 1}위
                      </span>
                    </td>
                    <td className="px-4 py-3 font-medium">{e.name}</td>
                    <td className="px-4 py-3 text-gray-600">{e.applicants.toLocaleString()}명</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="w-24 bg-gray-100 rounded-full h-2">
                          <div
                            className="bg-green-400 h-2 rounded-full"
                            style={{ width: `${Math.min(e.conversionRate * 5, 100)}%` }}
                          />
                        </div>
                        <span className="text-sm font-medium text-green-600">{e.conversionRate}%</span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  )
}
