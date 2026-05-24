'use client'
import { useState } from 'react'
import Link from 'next/link'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_APPLICANTS, MOCK_EVENTS } from '@/lib/admin-mock-data'

export default function ApplicantsPage() {
  const [eventId, setEventId] = useState('EV-2026-024')
  const currentEvent = MOCK_EVENTS.find(e => e.id === eventId)
  const filtered = MOCK_APPLICANTS

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          마케팅 &gt; <span className="text-gray-700 font-medium">응모자 관리</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">응모자 관리</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">이벤트</span>
              <select value={eventId} onChange={e => setEventId(e.target.value)} className="border border-gray-300 text-xs px-2 py-1 rounded bg-white w-64">
                {MOCK_EVENTS.map(e => (
                  <option key={e.id} value={e.id}>[{e.id}] {e.name}</option>
                ))}
              </select>
            </div>
            <input placeholder="고객번호 / 이름" className="border border-gray-300 text-xs px-2 py-1.5 rounded w-40" />
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">조건 충족</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                <option>전체</option><option>충족</option><option>미충족</option>
              </select>
            </div>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
            <button className="text-xs border border-gray-300 px-3 py-1.5 rounded text-gray-600">엑셀</button>
          </div>

          {currentEvent && (
            <div className="mb-3 bg-yellow-50 border border-yellow-200 rounded px-4 py-2.5 flex items-center gap-4 text-xs">
              <span className="font-medium text-yellow-800">{currentEvent.name}</span>
              <span className="text-yellow-700">{currentEvent.period}</span>
              <span className="text-yellow-700">대상: {currentEvent.target}</span>
              <span className="font-medium text-yellow-800 ml-auto">총 응모자 {currentEvent.applicantCount.toLocaleString()}명</span>
            </div>
          )}

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-gray-500">
              조회 {filtered.length}건 (조건 충족 {filtered.filter(a => a.conditionMet).length}건 / 미충족 {filtered.filter(a => !a.conditionMet).length}건)
            </p>
            <Link href="/admin/winners" className="text-xs bg-kb-yellow text-white font-bold px-3 py-1 rounded">추첨 진행 →</Link>
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  <th className="w-8 px-3 py-2.5"><input type="checkbox" /></th>
                  {['응모 ID','고객번호','이름','연락처','응모일시','가입 상품','가입 금액','자동이체 금액','조건 충족'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map(a => (
                  <tr key={a.id} className="hover:bg-kb-beige-light">
                    <td className="px-3 py-2.5"><input type="checkbox" /></td>
                    <td className="px-3 py-2.5 text-xs font-mono text-blue-600">{a.id}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{a.customerId}</td>
                    <td className="px-3 py-2.5 font-medium">{a.name}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{a.phone}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{a.appliedAt}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-600">{a.product}</td>
                    <td className="px-3 py-2.5 text-xs text-right font-medium">{a.joinAmount.toLocaleString()}원</td>
                    <td className="px-3 py-2.5 text-xs text-right text-gray-600">{a.autoTransferAmount.toLocaleString()}원</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${a.conditionMet ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                        {a.conditionMet ? '충족' : '미충족'}
                      </span>
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
