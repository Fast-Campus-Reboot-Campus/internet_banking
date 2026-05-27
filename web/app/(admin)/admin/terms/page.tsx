'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_TERMS, TermRecord, TermStatus } from '@/lib/admin-mock-data'

const STATUS_COLOR: Record<TermStatus, string> = {
  '적용중':   'bg-green-100 text-green-700',
  '예고 발송': 'bg-blue-100 text-blue-700',
}

export default function TermsPage() {
  const [selected, setSelected] = useState<TermRecord | null>(null)

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          정책 &gt; <span className="text-gray-700 font-medium">약관 관리</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">약관 관리</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">구분</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option><option>필수</option><option>선택</option></select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">상태</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option><option>적용중</option><option>예고 발송</option></select>
            </div>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
            <button className="text-xs bg-blue-50 text-blue-700 border border-blue-300 px-3 py-1.5 rounded">+ 약관 등록</button>
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['약관 코드','약관명','구분','적용 범위','버전','시행일','상태','동의 인원','작업'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {MOCK_TERMS.map(t => (
                  <tr key={t.id} className="hover:bg-kb-beige-light cursor-pointer" onClick={() => setSelected(t)}>
                    <td className="px-3 py-2.5 font-mono text-xs text-blue-600">{t.id}</td>
                    <td className="px-3 py-2.5 font-medium max-w-xs">{t.name}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${t.type === '필수' ? 'bg-purple-100 text-purple-700' : 'bg-gray-100 text-gray-600'}`}>{t.type}</span>
                    </td>
                    <td className="px-3 py-2.5 text-gray-600">{t.scope}</td>
                    <td className="px-3 py-2.5 font-mono text-xs text-gray-600">{t.version}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{t.effectiveDate}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[t.status]}`}>{t.status}</span>
                    </td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">
                      {t.consentCount > 0 ? t.consentCount.toLocaleString() + '명' : '-'}
                    </td>
                    <td className="px-3 py-2.5">
                      <div className="flex gap-1">
                        <button className="text-xs border border-gray-300 px-2 py-0.5 rounded text-gray-600">내용</button>
                        {t.status === '적용중' && <button className="text-xs border border-blue-300 text-blue-600 px-2 py-0.5 rounded">개정</button>}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {selected && (
            <div className="mt-4 bg-white border border-kb-border rounded-lg p-5 shadow-sm">
              <h3 className="text-sm font-bold mb-3">약관 상세 — {selected.id}</h3>
              <div className="grid grid-cols-4 gap-4 text-sm mb-4">
                <div><p className="text-xs text-gray-400">버전</p><p className="font-medium">{selected.version}</p></div>
                <div><p className="text-xs text-gray-400">시행일</p><p className="font-medium">{selected.effectiveDate}</p></div>
                <div><p className="text-xs text-gray-400">상태</p><span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[selected.status]}`}>{selected.status}</span></div>
                <div><p className="text-xs text-gray-400">동의 인원</p><p className="font-medium">{selected.consentCount.toLocaleString()}명</p></div>
              </div>
              <div className="border border-gray-200 rounded bg-gray-50 p-4 text-xs text-gray-600 leading-relaxed mb-4 min-h-24">
                [약관 본문 — {selected.name} {selected.version}]<br /><br />
                제1조 (목적) 이 약관은 국민은행(이하 &quot;은행&quot;)이 제공하는 전자금융거래서비스 이용에 관한 기본적인 사항을 정함을 목적으로 합니다.<br />
                제2조 (정의) ...<br />
                (이하 약관 내용)
              </div>
              {selected.status === '적용중' && (
                <div className="bg-blue-50 border border-blue-200 rounded p-3 text-xs text-blue-700 mb-4">
                  ℹ 개정 시 전체 동의 회원 {selected.consentCount.toLocaleString()}명에게 사전 예고 발송됩니다. (시행 최소 30일 전)
                </div>
              )}
              <div className="flex justify-end gap-2">
                <button onClick={() => setSelected(null)} className="px-4 py-2 border border-gray-300 text-sm rounded">닫기</button>
                {selected.status === '적용중' && <button className="px-4 py-2 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark transition-colors">개정안 작성</button>}
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
