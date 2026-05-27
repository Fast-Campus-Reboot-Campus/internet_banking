'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_SCREENINGS, ScreeningRecord, ScreeningStatus, HitType } from '@/lib/admin-mock-data'

const STATUS_COLOR: Record<ScreeningStatus, string> = {
  '검토대기': 'border-orange-400 text-orange-700 bg-orange-50',
  '검토중':   'border-blue-400 text-blue-700 bg-blue-50',
  '승인':     'border-green-500 text-green-700 bg-green-50',
  '거절':     'border-gray-400 text-gray-500 bg-gray-50',
}
const HIT_COLOR: Record<HitType, string> = {
  'OFAC SDN 매칭': 'border-red-400 text-red-700 bg-red-50',
  '국내 PEP':      'border-orange-400 text-orange-700 bg-orange-50',
  'UN 제재명단':   'border-purple-400 text-purple-700 bg-purple-50',
  'EU 제재':       'border-blue-400 text-blue-700 bg-blue-50',
}

export default function ScreeningPage() {
  const [selected, setSelected] = useState<ScreeningRecord | null>(null)
  const [statusFilter, setStatusFilter] = useState('전체')

  const filtered = MOCK_SCREENINGS.filter(r =>
    statusFilter === '전체' || r.status === statusFilter
  )

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          심사 &gt; <span className="text-kb-text font-semibold">제재대상 Hit 검토</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-h2 font-bold text-kb-text mb-4">제재대상 스크리닝 Hit 검토</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 flex-wrap shadow-sm">
            <input placeholder="고객번호 / 이름" className="border border-kb-border text-xs px-2 py-1.5 rounded-kb w-40 bg-white focus:outline-none focus:border-kb-yellow" />
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-kb-text-muted">Hit 유형</span>
              <select className="border border-kb-border text-xs px-2 py-1 rounded-kb bg-white focus:outline-none">
                <option>전체</option>
                {['OFAC SDN 매칭','국내 PEP','UN 제재명단','EU 제재'].map(o => <option key={o}>{o}</option>)}
              </select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-kb-text-muted">상태</span>
              <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)} className="border border-kb-border text-xs px-2 py-1 rounded-kb bg-white focus:outline-none">
                {['전체','검토대기','검토중','승인','거절'].map(o => <option key={o}>{o}</option>)}
              </select>
            </div>
            <span className="text-xs text-kb-text-muted">탐지일 2026-05-01 ~ 2026-05-11</span>
            <div className="ml-auto flex gap-2">
              <button className="px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded-kb hover:bg-kb-yellow-dark transition-colors">조회</button>
              <button className="text-xs border border-kb-border px-3 py-1.5 rounded-kb text-kb-text-body bg-white hover:bg-kb-beige-light transition-colors">초기화</button>
            </div>
          </div>

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-kb-text-muted">
              Hit 검토 <span className="font-semibold text-kb-text">{filtered.length}건</span>
              {' '}(검토대기 {filtered.filter(r => r.status === '검토대기').length} / 검토중 {filtered.filter(r => r.status === '검토중').length})
            </p>
            <button className="text-xs border border-kb-border px-3 py-1 rounded-kb text-kb-text-body bg-white hover:bg-kb-beige-light transition-colors">엑셀 다운로드</button>
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['고객번호','이름','생년월일','국적','Hit 유형','일치율','탐지일시','상태','검토자','작업'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-semibold">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-kb-border">
                {filtered.map(r => (
                  <tr key={r.id} className="hover:bg-kb-beige-light cursor-pointer transition-colors" onClick={() => setSelected(r)}>
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.customerId}</td>
                    <td className="px-3 py-2.5 font-medium text-kb-text">{r.name}</td>
                    <td className="px-3 py-2.5 text-xs text-kb-text-muted">{r.birthDate}</td>
                    <td className="px-3 py-2.5 text-kb-text-body">{r.nationality}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs border px-1.5 py-0.5 rounded-sm font-medium ${HIT_COLOR[r.hitType]}`}>{r.hitType}</span>
                    </td>
                    <td className="px-3 py-2.5 font-semibold text-kb-text">{r.matchRate}%</td>
                    <td className="px-3 py-2.5 text-xs text-kb-text-muted">{r.detectedAt}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs border px-1.5 py-0.5 rounded-sm font-medium ${STATUS_COLOR[r.status]}`}>{r.status}</span>
                    </td>
                    <td className="px-3 py-2.5 text-xs text-kb-text-muted">{r.reviewer ?? '-'}</td>
                    <td className="px-3 py-2.5">
                      {r.status === '검토대기'
                        ? <button className="text-xs bg-kb-yellow text-white px-2 py-0.5 rounded-kb font-medium hover:bg-kb-yellow-dark transition-colors">검토</button>
                        : <button className="text-xs border border-kb-border text-kb-text-muted px-2 py-0.5 rounded-kb hover:bg-kb-beige-light transition-colors">보기</button>
                      }
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {selected && (
            <div className="mt-4 bg-white border border-kb-border rounded-lg p-5 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-bold text-kb-text">스크리닝 상세 — {selected.customerId}</h3>
                <span className={`text-xs border px-2 py-0.5 rounded-sm font-medium ${STATUS_COLOR[selected.status]}`}>{selected.status}</span>
              </div>
              <div className="grid grid-cols-4 gap-3 text-sm mb-4">
                {[['고객번호', selected.customerId], ['이름', selected.name], ['생년월일', selected.birthDate], ['국적', selected.nationality]].map(([label, val]) => (
                  <div key={label} className="bg-kb-beige-light rounded-kb p-3">
                    <p className="text-xs text-kb-text-muted mb-0.5">{label}</p>
                    <p className="font-semibold text-kb-text">{val}</p>
                  </div>
                ))}
              </div>
              <div className="bg-red-50 border border-red-200 rounded-kb p-3 mb-4 text-xs text-red-700">
                <span className={`border px-1.5 py-0.5 rounded-sm font-medium mr-2 ${HIT_COLOR[selected.hitType]}`}>{selected.hitType}</span>
                유사도 <strong>{selected.matchRate}%</strong> 일치 · 탐지 {selected.detectedAt}
              </div>
              <p className="text-xs text-kb-text-muted mb-1.5">검토 의견</p>
              <textarea rows={2} placeholder="검토 의견을 입력하세요..." className="w-full border border-kb-border rounded-kb px-3 py-2 text-sm resize-none focus:outline-none focus:border-kb-yellow" />
              <div className="flex justify-end gap-2 mt-3">
                <button onClick={() => setSelected(null)} className="px-4 py-2 border border-kb-border text-sm rounded-kb text-kb-text-body hover:bg-kb-beige-light transition-colors">닫기</button>
                <button className="px-4 py-2 border border-red-400 text-sm rounded-kb text-red-600 hover:bg-red-50 transition-colors">거절 (제재 확정)</button>
                <button className="px-4 py-2 bg-kb-yellow text-white text-sm font-bold rounded-kb hover:bg-kb-yellow-dark transition-colors">승인 (동명이인)</button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
