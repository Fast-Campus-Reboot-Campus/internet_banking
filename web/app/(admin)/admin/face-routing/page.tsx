'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_FACE_ROUTING, FaceRoutingRecord, FaceRoutingStatus } from '@/lib/admin-mock-data'

const STATUS_COLOR: Record<FaceRoutingStatus, string> = {
  '대기':             'bg-orange-100 text-orange-700',
  '영업점 배정':      'bg-blue-100 text-blue-700',
  '완료 (대면확인)':  'bg-green-100 text-green-700',
}

export default function FaceRoutingPage() {
  const [selected, setSelected] = useState<FaceRoutingRecord | null>(null)
  const [branch, setBranch] = useState('강남역지점 (선택됨)')

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          심사 &gt; <span className="text-gray-700 font-medium">얼굴인증 라우팅</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">얼굴인증 실패 → 영업점 라우팅</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">실패 사유</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option><option>유사도 미달</option><option>딥페이크 의심</option></select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">라우팅 상태</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option></select>
            </div>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
          </div>

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-gray-500">대면확인 라우팅 {MOCK_FACE_ROUTING.length}건</p>
            <button className="text-xs border border-gray-300 px-3 py-1 rounded text-gray-600">엑셀</button>
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['접수번호','고객번호','이름','실패 사유','유사도 점수','라이브니스','실패 시각','희망 영업점','라우팅 상태','작업'].map(h=>(
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {MOCK_FACE_ROUTING.map(r => (
                  <tr key={r.id} className="hover:bg-kb-beige-light cursor-pointer" onClick={() => setSelected(r)}>
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.id}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{r.customerId}</td>
                    <td className="px-3 py-2.5 font-medium">{r.name}</td>
                    <td className="px-3 py-2.5"><span className="text-xs px-1.5 py-0.5 rounded-full bg-orange-100 text-orange-700 font-medium">{r.failureType}</span></td>
                    <td className="px-3 py-2.5 font-medium">{r.similarityScore} <span className="text-xs text-gray-400">(기준 0.85)</span></td>
                    <td className="px-3 py-2.5 text-gray-600">{r.liveness}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{r.failedAt}</td>
                    <td className="px-3 py-2.5 text-gray-600">{r.nearestBranch}</td>
                    <td className="px-3 py-2.5"><span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[r.status]}`}>{r.status}</span></td>
                    <td className="px-3 py-2.5">
                      {r.status === '대기' && <button className="text-xs bg-kb-yellow text-white px-2 py-0.5 rounded font-medium">배정</button>}
                      {r.status !== '대기' && <button className="text-xs border border-gray-300 text-gray-500 px-2 py-0.5 rounded">상세</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {selected && (
            <div className="mt-4 bg-white border border-kb-border rounded-lg p-5 shadow-sm">
              <h3 className="text-sm font-bold mb-3">영업점 배정 ({selected.id})</h3>
              <div className="grid grid-cols-2 gap-4 text-sm mb-4">
                <div><p className="text-xs text-gray-400 mb-0.5">고객</p><p className="font-medium">{selected.name} ({selected.customerId}) / 010-****-1234</p></div>
                <div>
                  <p className="text-xs text-gray-400 mb-0.5">희망 영업점</p>
                  <select value={branch} onChange={e=>setBranch(e.target.value)} className="border border-gray-300 text-sm px-2 py-1 rounded w-full bg-white">
                    <option>강남역지점 (선택됨)</option><option>여의도본점</option><option>판교테크노밸리</option>
                  </select>
                </div>
                <div><p className="text-xs text-gray-400 mb-0.5">방문 예정일</p><input type="date" defaultValue="2026-05-15" className="border border-gray-300 text-sm px-2 py-1 rounded w-full" /></div>
                <div>
                  <p className="text-xs text-gray-400 mb-0.5">사전 SMS 안내</p>
                  <select className="border border-gray-300 text-sm px-2 py-1 rounded w-full bg-white"><option>발송</option><option>미발송</option></select>
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <button onClick={() => setSelected(null)} className="px-4 py-2 border border-gray-300 text-sm rounded">취소</button>
                <button className="px-4 py-2 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark transition-colors">영업점 배정 + SMS 발송</button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
