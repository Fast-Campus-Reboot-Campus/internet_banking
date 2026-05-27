'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_AGENTS, AgentRecord } from '@/lib/admin-mock-data'

export default function AgentPage() {
  const [selected, setSelected] = useState<AgentRecord | null>(null)

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          심사 &gt; <span className="text-gray-700 font-medium">대리인 검토</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">대리인 위임장 검토</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">위임 유형</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option><option>임의대리</option><option>법정대리</option></select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">상태</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option></select>
            </div>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
          </div>

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-gray-500">대리인 검토 {MOCK_AGENTS.length}건</p>
            <button className="text-xs border border-gray-300 px-3 py-1 rounded text-gray-600">엑셀</button>
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['접수번호','본인 고객번호','대리인 이름','관계','위임 유형','위임 범위','서류 제출','접수일','상태','작업'].map(h=>(
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {MOCK_AGENTS.map(r => (
                  <tr key={r.id} className="hover:bg-kb-beige-light cursor-pointer" onClick={() => setSelected(r)}>
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.id}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{r.ownerId}</td>
                    <td className="px-3 py-2.5 font-medium">{r.agentName}</td>
                    <td className="px-3 py-2.5 text-gray-600">{r.relationship}</td>
                    <td className="px-3 py-2.5 text-gray-600">{r.delegationType}</td>
                    <td className="px-3 py-2.5 text-gray-600">{r.scope}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{r.documents}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{r.submittedAt}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium
                        ${r.status === '검토대기' ? 'bg-orange-100 text-orange-700' : 'bg-red-100 text-red-700'}`}>
                        {r.status}
                      </span>
                    </td>
                    <td className="px-3 py-2.5">
                      {r.status === '검토대기' && <button className="text-xs bg-kb-yellow text-white px-2 py-0.5 rounded font-medium">검토</button>}
                      {r.status !== '검토대기' && <button className="text-xs border border-gray-300 text-gray-500 px-2 py-0.5 rounded">보기</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {selected && (
            <div className="mt-4 bg-white border border-kb-border rounded-lg p-5 shadow-sm">
              <h3 className="text-sm font-bold mb-3">위임 서류 검토 ({selected.id})</h3>
              <div className="grid grid-cols-3 gap-4 mb-4">
                {['위임장 이미지', '인감증명서', '가족관계증명서'].map(doc => (
                  <div key={doc} className="border border-gray-200 rounded h-32 flex items-center justify-center bg-gray-50 text-xs text-gray-400">[{doc}]</div>
                ))}
              </div>
              <div className="grid grid-cols-3 gap-4 text-sm mb-4">
                <div>
                  <p className="text-xs text-gray-400">위임장 (인감 포함)</p>
                  <p className="font-medium">위임 범위: {selected.scope}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-400">인감증명서 (발급일)</p>
                  <p className="font-medium text-green-600">일치 (등록 인감과 동일)</p>
                </div>
                <div>
                  <p className="text-xs text-gray-400">인감 대조 결과</p>
                  <p className="font-medium text-green-600">일치 (등록 인감과 동일)</p>
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <button onClick={() => setSelected(null)} className="px-4 py-2 border border-gray-300 text-sm rounded">닫기</button>
                <button className="px-4 py-2 border border-red-400 text-sm rounded text-red-600">거절 (위조 의심)</button>
                <button className="px-4 py-2 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark transition-colors">승인 (권한 액팅)</button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
