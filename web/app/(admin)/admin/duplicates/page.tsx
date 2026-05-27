'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_DUPLICATES, DuplicateRecord, DupStatus } from '@/lib/admin-mock-data'

const STATUS_COLOR: Record<DupStatus, string> = {
  '검토대기': 'bg-orange-100 text-orange-700',
  '검토중':   'bg-blue-100 text-blue-700',
  '복본':     'bg-green-100 text-green-700',
}

const COMPARE_ROWS = [
  { label: '이름', newVal: '김민수', existVal: '김민수', match: true },
  { label: '생년월일', newVal: '1985-03-15', existVal: '1985-03-15', match: true },
  { label: 'CI', newVal: 'a8f3...c921', existVal: 'b2e7...d445', match: false },
  { label: '휴대폰', newVal: '010-****-3456', existVal: '010-****-7890', match: false },
  { label: '주소', newVal: '서울 강남구', existVal: '부산 해운대구', match: false },
]

export default function DuplicatesPage() {
  const [selected, setSelected] = useState<DuplicateRecord | null>(null)

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          심사 &gt; <span className="text-gray-700 font-medium">중복고객 검토</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">중복 고객 의심건 검토</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">처리상태</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option></select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">일치 유형</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option></select>
            </div>
            <span className="text-xs text-gray-400">발생일 2026-04-01 ~ 2026-05-11</span>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
          </div>

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-gray-500">동명이인 의심 {MOCK_DUPLICATES.length}건</p>
            <button className="text-xs border border-gray-300 px-3 py-1 rounded text-gray-600">엑셀</button>
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['발생번호','신규 고객번호','기존 고객번호','이름','생년월일','일치 항목','발생일','처리상태','작업'].map(h=>(
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {MOCK_DUPLICATES.map(r => (
                  <tr key={r.id} className="hover:bg-kb-beige-light cursor-pointer" onClick={() => setSelected(r)}>
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.id}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{r.newCustomerId} (신규)</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{r.existingCustomerId} (기존)</td>
                    <td className="px-3 py-2.5 font-medium">{r.name} / {r.name}</td>
                    <td className="px-3 py-2.5 text-gray-500">{r.birthDate}</td>
                    <td className="px-3 py-2.5"><span className="text-xs px-1.5 py-0.5 rounded-full bg-orange-100 text-orange-700 font-medium">{r.matchType}</span></td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{r.detectedAt}</td>
                    <td className="px-3 py-2.5"><span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[r.status]}`}>{r.status}</span></td>
                    <td className="px-3 py-2.5"><button className="text-xs bg-kb-yellow text-white px-2 py-0.5 rounded font-medium">대조</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* 양쪽 정보 대조 */}
          {selected && (
            <div className="mt-4 bg-white border border-kb-border rounded-lg p-5 shadow-sm">
              <h3 className="text-sm font-bold mb-3">양쪽 정보 대조 ({selected.id})</h3>
              <table className="w-full text-sm border border-gray-200 rounded overflow-hidden mb-4">
                <thead>
                  <tr className="bg-kb-beige-light text-xs text-kb-text-muted">
                    <th className="px-4 py-2 text-left font-medium">항목</th>
                    <th className="px-4 py-2 text-left font-medium">신규 고객 {selected.newCustomerId}</th>
                    <th className="px-4 py-2 text-left font-medium">기존 고객 {selected.existingCustomerId}</th>
                    <th className="px-4 py-2 text-left font-medium">일치</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {COMPARE_ROWS.map(row => (
                    <tr key={row.label}>
                      <td className="px-4 py-2 text-gray-500">{row.label}</td>
                      <td className="px-4 py-2 font-medium">{row.newVal}</td>
                      <td className="px-4 py-2 font-medium">{row.existVal}</td>
                      <td className="px-4 py-2">{row.match ? <span className="text-green-600">✓</span> : <span className="text-red-500">✗</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="flex justify-end gap-2">
                <button onClick={() => setSelected(null)} className="px-4 py-2 border border-gray-300 text-sm rounded">닫기</button>
                <button className="px-4 py-2 border border-gray-300 text-sm rounded text-gray-700">동명이인으로 확정 (별도 유지)</button>
                <button className="px-4 py-2 bg-red-500 text-white text-sm font-bold rounded">동일인 → 기존계정 안내, 신규 차단</button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
