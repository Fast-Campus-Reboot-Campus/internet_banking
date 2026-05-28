'use client'
import { useState } from 'react'
import Link from 'next/link'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_EVENTS, EventStatus } from '@/lib/admin-mock-data'

const STATUS_COLOR: Record<EventStatus, string> = {
  '진행중':   'bg-green-100 text-green-700',
  '마감임박': 'bg-orange-100 text-orange-700',
  '종료':     'bg-gray-100 text-gray-400',
}

export default function EventsPage() {
  const [statusFilter, setStatusFilter] = useState('전체')

  const filtered = MOCK_EVENTS.filter(e =>
    statusFilter === '전체' || e.status === statusFilter
  )

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          마케팅 &gt; <span className="text-gray-700 font-medium">이벤트 목록</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">이벤트 목록</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <input placeholder="이벤트명 / ID" className="border border-gray-300 text-xs px-2 py-1.5 rounded w-48" />
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">유형</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                <option>전체</option><option>응모형</option><option>미션</option><option>가입 이벤트</option>
              </select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">상태</span>
              <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)} className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                {['전체','진행중','마감임박','종료'].map(o => <option key={o}>{o}</option>)}
              </select>
            </div>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
            <Link href="/admin/events/new" className="text-xs bg-blue-50 text-blue-700 border border-blue-300 px-3 py-1.5 rounded">+ 이벤트 등록</Link>
          </div>

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-gray-500">총 {filtered.length}건</p>
            <button className="text-xs border border-gray-300 px-3 py-1 rounded text-gray-600">엑셀 다운로드</button>
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['이벤트 ID','이벤트명','유형','진행기간','대상','응모자','경품','상태','작업'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map(e => (
                  <tr key={e.id} className="hover:bg-kb-beige-light">
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{e.id}</td>
                    <td className="px-3 py-2.5 font-medium">{e.name}</td>
                    <td className="px-3 py-2.5">
                      <span className="text-xs px-1.5 py-0.5 bg-gray-100 text-gray-600 rounded-full">{e.type}</span>
                    </td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{e.period}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-600">{e.target}</td>
                    <td className="px-3 py-2.5 text-xs font-medium">{e.applicantCount.toLocaleString()}명</td>
                    <td className="px-3 py-2.5 text-xs text-gray-600">{e.prize}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[e.status]}`}>{e.status}</span>
                    </td>
                    <td className="px-3 py-2.5">
                      <div className="flex gap-1">
                        <Link href={`/admin/applicants?event=${e.id}`} className="text-xs border border-gray-300 px-2 py-0.5 rounded text-gray-600 hover:bg-kb-beige-light">응모자</Link>
                        <Link href={`/admin/winners?event=${e.id}`} className="text-xs border border-gray-300 px-2 py-0.5 rounded text-gray-600 hover:bg-kb-beige-light">당첨자</Link>
                        <Link href="/admin/events/new" className="text-xs border border-blue-300 text-blue-600 px-2 py-0.5 rounded hover:bg-blue-50">수정</Link>
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
