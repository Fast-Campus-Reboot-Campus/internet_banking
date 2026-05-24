'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_EDD, EDDRecord, EDDType, EDDStatus } from '@/lib/admin-mock-data'

const EDD_COLOR: Record<EDDType, string> = {
  '고위험 국가': 'bg-red-100 text-red-700',
  'PEP':        'bg-orange-100 text-orange-700',
  '고액거래':   'bg-yellow-100 text-yellow-700',
}
const STATUS_COLOR: Record<EDDStatus, string> = {
  '심사':     'bg-yellow-100 text-yellow-700',
  '서류요청': 'bg-gray-100 text-gray-600',
}

export default function EDDPage() {
  const [tab, setTab] = useState<'심사 대기'|'진행중'|'완료'>('심사 대기')
  const [selected, setSelected] = useState<EDDRecord | null>(null)

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          심사 &gt; <span className="text-gray-700 font-medium">EDD 심사·승인</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">EDD 심사·승인</h1>

          {/* 탭 */}
          <div className="flex border-b border-gray-200 mb-4">
            {(['심사 대기', '진행중', '완료'] as const).map(t => (
              <button key={t} onClick={() => setTab(t)}
                className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors
                  ${tab === t ? 'border-kb-yellow text-kb-taupe-dark' : 'border-transparent text-gray-500 hover:text-gray-700'}`}>
                {t} {t === '심사 대기' ? `(${MOCK_EDD.filter(r=>r.status==='심사').length})` : t === '진행중' ? '(5)' : '(152)'}
              </button>
            ))}
          </div>

          {/* 필터 */}
          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">위험등급</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option><option>고위험 국가</option><option>PEP</option></select>
            </div>
            <span className="text-xs text-gray-400">입수일 2026-04-01 ~ 2026-05-11</span>
            <input placeholder="고객번호/이름" className="border border-gray-300 text-xs px-2 py-1 rounded w-32" />
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
            <button className="text-xs border border-gray-300 px-3 py-1 rounded text-gray-600">엑셀</button>
          </div>

          <p className="text-xs text-gray-500 mb-2">EDD 심사 대기 {MOCK_EDD.length}건</p>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['접수번호','고객번호','이름','EDD 사유','자금원천','직업·소득','거래목적','실소유자','접수일','제출서류','작업'].map(h=>(
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {MOCK_EDD.map(r => (
                  <tr key={r.id} className="hover:bg-kb-beige-light cursor-pointer" onClick={() => setSelected(r)}>
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.id}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{r.customerId}</td>
                    <td className="px-3 py-2.5 font-medium">{r.name}</td>
                    <td className="px-3 py-2.5"><span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${EDD_COLOR[r.eddType]}`}>{r.eddType}</span></td>
                    <td className="px-3 py-2.5 text-center">{r.fundSource ? '✓' : '△ 미제출'}</td>
                    <td className="px-3 py-2.5 text-center">{r.jobIncome ? '✓' : '△ 미제출'}</td>
                    <td className="px-3 py-2.5 text-center">{r.transactionPurpose ? '✓' : '△ 미달'}</td>
                    <td className="px-3 py-2.5 text-center">{r.realOwner ? '✓ 등록' : '-'}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{r.submittedAt}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-600">{r.docCount}</td>
                    <td className="px-3 py-2.5">
                      <button className="text-xs bg-kb-yellow text-white px-2 py-0.5 rounded font-medium">심사</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* 선택 상세 */}
          {selected && (
            <div className="mt-4 bg-white border border-kb-border rounded-lg p-5 shadow-sm">
              <h3 className="text-sm font-bold mb-3">선택된 건 상세 ({selected.id})</h3>
              <div className="grid grid-cols-2 gap-x-8 gap-y-2 text-sm mb-4">
                <div><span className="text-gray-400">고객명 / 고객번호</span><span className="ml-2 font-medium">{selected.name} / {selected.customerId}</span></div>
                <div><span className="text-gray-400">EDD 사유</span><span className={`ml-2 text-xs px-1.5 py-0.5 rounded-full font-medium ${EDD_COLOR[selected.eddType]}`}>{selected.eddType} + 고객 신규 가입 (5천만원 예치)</span></div>
                <div><span className="text-gray-400">CDD 위험등급</span><span className="ml-2 font-medium text-red-600">고위험 (자동 산정)</span></div>
                <div><span className="text-gray-400">담당 심사자</span><span className="ml-2 text-gray-700">{selected.detail?.assignedTo ?? '컴플라이언스팀'}</span></div>
              </div>
              <div className="flex justify-end gap-2">
                <button onClick={() => setSelected(null)} className="px-4 py-2 border border-gray-300 text-sm rounded">닫기</button>
                <button className="px-4 py-2 border border-gray-300 text-sm rounded text-gray-700">반려 (서류 재요청)</button>
                <button className="px-4 py-2 border border-red-400 text-sm rounded text-red-600">거절</button>
                <button className="px-4 py-2 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark transition-colors">책임자 승인 요청</button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
