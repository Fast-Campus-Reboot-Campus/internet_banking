'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_MINORS, MinorRecord, MinorStatus } from '@/lib/admin-mock-data'

const STATUS_COLOR: Record<MinorStatus, string> = {
  '검토대기': 'bg-orange-100 text-orange-700',
  '거절':     'bg-red-100 text-red-700',
}

export default function MinorPage() {
  const [selected, setSelected] = useState<MinorRecord | null>(null)

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          심사 &gt; <span className="text-gray-700 font-medium">미성년 검토</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">미성년자 법정대리인 관계 검토</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">상태</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option></select>
            </div>
            <span className="text-xs text-gray-400">접수일 2026-04-01 ~ 2026-05-11</span>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
          </div>

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-gray-500">미성년 가입 검토 {MOCK_MINORS.length}건</p>
            <button className="text-xs border border-gray-300 px-3 py-1 rounded text-gray-600">엑셀</button>
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['접수번호','미성년 이름','나이','법정대리인','관계','관계 검증','대리인 본인확인','접수일','상태','작업'].map(h=>(
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {MOCK_MINORS.map(r => (
                  <tr key={r.id} className="hover:bg-kb-beige-light cursor-pointer" onClick={() => setSelected(r)}>
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.id}</td>
                    <td className="px-3 py-2.5 font-medium">{r.minorName}</td>
                    <td className="px-3 py-2.5 text-gray-600">만 {r.age}세</td>
                    <td className="px-3 py-2.5 text-gray-600">{r.guardianName}</td>
                    <td className="px-3 py-2.5 text-gray-600">{r.relationship}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium
                        ${r.relationshipCheck.startsWith('일치') ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                        {r.relationshipCheck}
                      </span>
                    </td>
                    <td className="px-3 py-2.5 text-gray-600">{r.guardianVerified ? '✓ PASS 인증' : '✗ 미완료'}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{r.submittedAt}</td>
                    <td className="px-3 py-2.5"><span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[r.status]}`}>{r.status}</span></td>
                    <td className="px-3 py-2.5">
                      {r.status === '검토대기' && <button className="text-xs bg-kb-yellow text-white px-2 py-0.5 rounded font-medium">검토</button>}
                      {r.status === '거절' && <button className="text-xs border border-gray-300 text-gray-500 px-2 py-0.5 rounded">보기</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {selected && (
            <div className="mt-4 bg-white border border-kb-border rounded-lg p-5 shadow-sm">
              <h3 className="text-sm font-bold mb-3">관계 검증 상세 ({selected.id})</h3>
              <table className="w-full text-sm border border-gray-200 rounded overflow-hidden mb-4">
                <thead>
                  <tr className="bg-kb-beige-light text-xs text-kb-text-muted">
                    <th className="px-4 py-2 text-left font-medium">항목</th>
                    <th className="px-4 py-2 text-left font-medium">미성년 본인</th>
                    <th className="px-4 py-2 text-left font-medium">법정대리인</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {[
                    { label:'이름', minor:selected.minorName, guardian:selected.guardianName.split(' ')[0] },
                    { label:'생년월일', minor:`2014-08-15 (만 ${selected.age}세)`, guardian:'1985-03-15 (만 41세)' },
                    { label:'가족관계증명서', minor:'✓ 부녀 관계 확인 (2026-05-10 발급)', guardian:'-' },
                    { label:'법정대리인 본인확인', minor:'-', guardian:'✓ PASS 인증 완료' },
                    { label:'동의서 서명', minor:'-', guardian:'✓ 전자서명 완료' },
                  ].map(row => (
                    <tr key={row.label}>
                      <td className="px-4 py-2 text-gray-500">{row.label}</td>
                      <td className="px-4 py-2 font-medium">{row.minor}</td>
                      <td className="px-4 py-2 font-medium">{row.guardian}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="flex justify-end gap-2">
                <button onClick={() => setSelected(null)} className="px-4 py-2 border border-gray-300 text-sm rounded">닫기</button>
                <button className="px-4 py-2 border border-red-400 text-sm rounded text-red-600">거절 (관계 불일치)</button>
                <button className="px-4 py-2 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark transition-colors">승인 (가입 진행)</button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
