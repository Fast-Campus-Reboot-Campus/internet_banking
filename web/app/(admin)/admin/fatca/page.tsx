'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_FATCA, FATCARecord, FATCAType } from '@/lib/admin-mock-data'

const TYPE_COLOR: Record<FATCAType, string> = {
  'FATCA': 'bg-blue-100 text-blue-700',
  'CRS':   'bg-purple-100 text-purple-700',
  '비합조': 'bg-red-100 text-red-700',
}

export default function FATCAPage() {
  const [typeFilter, setTypeFilter] = useState('전체')
  const [selected, setSelected] = useState<FATCARecord | null>(null)

  const filtered = MOCK_FATCA.filter(r =>
    typeFilter === '전체' || r.type === typeFilter
  )

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          정책 &gt; <span className="text-gray-700 font-medium">FATCA/CRS</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-1">FATCA / CRS 해외 금융계좌 신고</h1>
          <p className="text-xs text-gray-400 mb-4">미국 세금보고 의무(FATCA) 및 공통보고기준(CRS) 대상 고객 현황</p>

          {/* 요약 카드 */}
          <div className="grid grid-cols-4 gap-4 mb-5">
            {[
              { label: 'FATCA 대상', count: MOCK_FATCA.filter(r => r.type === 'FATCA').length, color: 'bg-blue-50 border-blue-200 text-blue-700' },
              { label: 'CRS 대상', count: MOCK_FATCA.filter(r => r.type === 'CRS').length, color: 'bg-purple-50 border-purple-200 text-purple-700' },
              { label: '비합조', count: MOCK_FATCA.filter(r => r.type === '비합조').length, color: 'bg-red-50 border-red-200 text-red-700' },
              { label: '보고서 제출 완료', count: MOCK_FATCA.filter(r => r.isSubmitted).length, color: 'bg-green-50 border-green-200 text-green-700' },
            ].map(card => (
              <div key={card.label} className={`border rounded p-4 ${card.color}`}>
                <p className="text-xs mb-1">{card.label}</p>
                <p className="text-2xl font-bold">{card.count}<span className="text-sm font-normal ml-1">건</span></p>
              </div>
            ))}
          </div>

          {/* 필터 */}
          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <input placeholder="고객번호 / 이름" className="border border-gray-300 text-xs px-2 py-1.5 rounded w-48" />
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">유형</span>
              <select value={typeFilter} onChange={e => setTypeFilter(e.target.value)} className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                {['전체','FATCA','CRS','비합조'].map(o => <option key={o}>{o}</option>)}
              </select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">제출 여부</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option><option>제출</option><option>미제출</option></select>
            </div>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
            <button className="text-xs border border-gray-300 px-3 py-1.5 rounded text-gray-600">IRS/국세청 보고 파일 생성</button>
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['고객번호','이름','국적','납세 국가','TIN','유형','W-9/자가증명','제출여부','제출일','보고 기한','작업'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map(r => (
                  <tr key={r.customerId} className="hover:bg-kb-beige-light cursor-pointer" onClick={() => setSelected(r)}>
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.customerId}</td>
                    <td className="px-3 py-2.5 font-medium">{r.name}</td>
                    <td className="px-3 py-2.5 text-gray-600">{r.nationality}</td>
                    <td className="px-3 py-2.5 text-gray-600">{r.taxCountry}</td>
                    <td className="px-3 py-2.5 font-mono text-xs text-gray-500">{r.tin}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${TYPE_COLOR[r.type]}`}>{r.type}</span>
                    </td>
                    <td className="px-3 py-2.5 text-xs text-gray-600">{r.w9Status}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${r.isSubmitted ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                        {r.isSubmitted ? '제출' : '미제출'}
                      </span>
                    </td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{r.submittedAt}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{r.reportDeadline}</td>
                    <td className="px-3 py-2.5">
                      {!r.isSubmitted && <button className="text-xs bg-kb-yellow text-white px-2 py-0.5 rounded font-medium">독촉 발송</button>}
                      {r.isSubmitted && <button className="text-xs border border-gray-300 text-gray-500 px-2 py-0.5 rounded">보고서</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {selected && (
            <div className="mt-4 bg-white border border-kb-border rounded-lg p-5 shadow-sm">
              <h3 className="text-sm font-bold mb-3">FATCA/CRS 상세 — {selected.customerId}</h3>
              <div className="grid grid-cols-3 gap-4 text-sm mb-4">
                <div><p className="text-xs text-gray-400">이름</p><p className="font-medium">{selected.name}</p></div>
                <div><p className="text-xs text-gray-400">국적</p><p className="font-medium">{selected.nationality}</p></div>
                <div><p className="text-xs text-gray-400">납세 국가</p><p className="font-medium">{selected.taxCountry}</p></div>
                <div><p className="text-xs text-gray-400">TIN</p><p className="font-mono font-medium">{selected.tin}</p></div>
                <div><p className="text-xs text-gray-400">유형</p>
                  <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${TYPE_COLOR[selected.type]}`}>{selected.type}</span>
                </div>
                <div><p className="text-xs text-gray-400">보고 기한</p><p className="font-medium">{selected.reportDeadline}</p></div>
              </div>
              {!selected.isSubmitted && (
                <div className="bg-red-50 border border-red-200 rounded p-3 text-xs text-red-700 mb-4">
                  ⚠ 자가증명서 미제출 상태입니다. 독촉 발송 또는 계좌 이용 제한 조치가 필요합니다.
                </div>
              )}
              <div className="flex justify-end gap-2">
                <button onClick={() => setSelected(null)} className="px-4 py-2 border border-gray-300 text-sm rounded">닫기</button>
                {!selected.isSubmitted && <button className="px-4 py-2 border border-orange-400 text-orange-600 text-sm rounded">독촉 이메일 발송</button>}
                <button className="px-4 py-2 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark transition-colors">IRS 보고서 다운로드</button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
