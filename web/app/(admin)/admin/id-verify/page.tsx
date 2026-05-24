'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_ID_VERIFY, IDVerifyRecord, DocStatus } from '@/lib/admin-mock-data'

const STATUS_COLOR: Record<DocStatus, string> = {
  '검토대기': 'bg-orange-100 text-orange-700',
  '검토중':   'bg-blue-100 text-blue-700',
  '거절':     'bg-red-100 text-red-700',
}

export default function IDVerifyPage() {
  const [selected, setSelected] = useState<IDVerifyRecord | null>(null)

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          심사 &gt; <span className="text-gray-700 font-medium">증표 위변조 검토</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">실명확인 증표 위·변조 검토</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">증표 유형</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option><option>주민등록증</option><option>운전면허증</option><option>여권</option></select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">의심 사유</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option></select>
            </div>
            <span className="text-xs text-gray-400">접수일 2026-04-01 ~ 2026-05-11</span>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
          </div>

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-gray-500">증표 위변조 의심 {MOCK_ID_VERIFY.length}건</p>
            <button className="text-xs border border-gray-300 px-3 py-1 rounded text-gray-600">엑셀</button>
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['접수번호','고객번호','이름','증표 유형','증표번호 (마스킹)','의심 사유','API 검증 결과','접수일','상태','작업'].map(h=>(
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {MOCK_ID_VERIFY.map(r => (
                  <tr key={r.id} className="hover:bg-kb-beige-light cursor-pointer" onClick={() => setSelected(r)}>
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.id}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{r.customerId}</td>
                    <td className="px-3 py-2.5 font-medium">{r.name}</td>
                    <td className="px-3 py-2.5 text-gray-600">{r.docType}</td>
                    <td className="px-3 py-2.5 font-mono text-xs text-gray-500">{r.maskedDocNumber}</td>
                    <td className="px-3 py-2.5"><span className="text-xs px-1.5 py-0.5 rounded-full bg-red-100 text-red-700 font-medium">{r.suspicionType}</span></td>
                    <td className="px-3 py-2.5 text-xs text-gray-600">{r.apiResult}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{r.submittedAt}</td>
                    <td className="px-3 py-2.5"><span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[r.status]}`}>{r.status}</span></td>
                    <td className="px-3 py-2.5"><button className="text-xs bg-kb-yellow text-white px-2 py-0.5 rounded font-medium">검토</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {selected && (
            <div className="mt-4 bg-white border border-kb-border rounded-lg p-5 shadow-sm">
              <h3 className="text-sm font-bold mb-3">증표 이미지 검토 ({selected.id})</h3>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div className="border border-gray-200 rounded h-40 flex items-center justify-center bg-gray-50 text-xs text-gray-400">[증표 앞면 이미지]</div>
                <div className="border border-gray-200 rounded h-40 flex items-center justify-center bg-gray-50 text-xs text-gray-400">[증표 뒷면 이미지]</div>
              </div>
              <div className="grid grid-cols-3 gap-4 text-sm mb-4">
                <div><p className="text-xs text-gray-400">OCR 추출</p><p className="font-mono font-medium">850315-1234567</p></div>
                <div><p className="text-xs text-gray-400">행안부 진위확인 API</p><p className="text-red-600 font-medium">{selected.suspicionType}</p></div>
                {selected.aiScore && <div><p className="text-xs text-gray-400">AI 위변조 탐지 점수</p><p className="font-medium text-orange-600">{selected.aiScore} (위변조 의심 기준 0.7 초과)</p></div>}
              </div>
              <div className="flex justify-end gap-2">
                <button onClick={() => setSelected(null)} className="px-4 py-2 border border-gray-300 text-sm rounded">닫기</button>
                <button className="px-4 py-2 border border-gray-300 text-sm rounded text-gray-700">정상 처리</button>
                <button className="px-4 py-2 bg-red-500 text-white text-sm font-bold rounded">거절 (위변조 확정)</button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
