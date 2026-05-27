'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_CONSENT_LOGS, ConsentLog } from '@/lib/admin-mock-data'

const CONSENT_COLOR: Record<string, string> = {
  '동의':   'bg-green-100 text-green-700',
  '미동의': 'bg-gray-100 text-gray-500',
  '철회':   'bg-red-100 text-red-700',
}

export default function ConsentLogPage() {
  const [search, setSearch] = useState('')
  const [consentFilter, setConsentFilter] = useState('전체')
  const [selected, setSelected] = useState<ConsentLog | null>(null)

  const filtered = MOCK_CONSENT_LOGS.filter(l =>
    (consentFilter === '전체' || l.consentType === consentFilter) &&
    (l.customerId.includes(search) || l.termName.includes(search))
  )

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          정책 &gt; <span className="text-gray-700 font-medium">동의이력 조회</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">동의이력 조회</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm flex-wrap">
            <input
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="고객번호 / 약관명"
              className="border border-gray-300 text-xs px-2 py-1.5 rounded w-48"
            />
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">약관 구분</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                <option>전체</option><option>필수</option><option>선택</option>
              </select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">동의 유형</span>
              <select value={consentFilter} onChange={e => setConsentFilter(e.target.value)} className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                {['전체','동의','미동의','철회'].map(o => <option key={o}>{o}</option>)}
              </select>
            </div>
            <span className="text-xs text-gray-400">기간 2026-04-01 ~ 2026-05-11</span>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
            <button className="text-xs border border-gray-300 px-3 py-1.5 rounded text-gray-600">엑셀</button>
          </div>

          <div className="mb-2 text-xs text-gray-500">조회 {filtered.length}건</div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['로그 ID','일시','고객번호','약관 ID','약관 버전','약관명','동의 유형','접속 IP','단말','해시값'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map(l => (
                  <tr key={l.id} className="hover:bg-kb-beige-light cursor-pointer" onClick={() => setSelected(l)}>
                    <td className="px-3 py-2.5 text-xs font-mono text-blue-600">{l.id}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{l.date}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-600">{l.customerId}</td>
                    <td className="px-3 py-2.5 text-xs font-mono text-gray-500">{l.termId}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{l.termVersion}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-700">{l.termName}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${CONSENT_COLOR[l.consentType]}`}>{l.consentType}</span>
                    </td>
                    <td className="px-3 py-2.5 text-xs font-mono text-gray-500">{l.ip}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{l.device}</td>
                    <td className="px-3 py-2.5 text-xs font-mono text-gray-400">{l.hash}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {selected && (
            <div className="mt-4 bg-white border border-kb-border rounded-lg p-5 shadow-sm">
              <h3 className="text-sm font-bold mb-3">동의 상세 — {selected.id}</h3>
              <div className="grid grid-cols-2 gap-4 text-sm mb-4">
                <div className="space-y-2">
                  <div><span className="text-xs text-gray-400 mr-2">고객번호</span><span className="font-medium">{selected.customerId}</span></div>
                  <div><span className="text-xs text-gray-400 mr-2">약관</span><span className="font-medium">{selected.termName} ({selected.termVersion})</span></div>
                  <div><span className="text-xs text-gray-400 mr-2">동의 유형</span>
                    <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${CONSENT_COLOR[selected.consentType]}`}>{selected.consentType}</span>
                  </div>
                </div>
                <div className="space-y-2">
                  <div><span className="text-xs text-gray-400 mr-2">일시</span><span className="font-medium">{selected.date}</span></div>
                  <div><span className="text-xs text-gray-400 mr-2">단말</span><span className="font-medium">{selected.device}</span></div>
                  <div><span className="text-xs text-gray-400 mr-2">IP</span><span className="font-mono font-medium">{selected.ip}</span></div>
                </div>
              </div>
              <div className="bg-kb-beige-light border border-kb-border rounded p-3 mb-4">
                <p className="text-xs text-gray-400 mb-1">무결성 해시 (SHA-256)</p>
                <p className="font-mono text-xs text-gray-700 break-all">{selected.hash}</p>
              </div>
              <div className="flex justify-end">
                <button onClick={() => setSelected(null)} className="px-4 py-2 border border-gray-300 text-sm rounded">닫기</button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
