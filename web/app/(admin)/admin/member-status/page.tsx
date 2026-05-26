'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_MEMBERS, MOCK_STATUS_CHANGES, MemberStatus } from '@/lib/admin-mock-data'

const STATUS_COLOR: Record<MemberStatus, string> = {
  '활성': 'bg-green-100 text-green-700',
  '휴면': 'bg-gray-100 text-gray-500',
  '정지': 'bg-red-100 text-red-700',
  '탈퇴': 'bg-gray-100 text-gray-400',
}

export default function MemberStatusPage() {
  const [selected, setSelected] = useState<string | null>(null)
  const [reason, setReason] = useState('')
  const [newStatus, setNewStatus] = useState<MemberStatus>('정지')
  const selectedMember = selected ? MOCK_MEMBERS.find(m => m.id === selected) : null

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          회원관리 &gt; <span className="text-gray-700 font-medium">회원 상태 관리</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">회원 상태 관리 (정지 / 해제)</h1>

          {/* 검색 */}
          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <input placeholder="고객번호 / 이름" className="border border-gray-300 text-xs px-2 py-1.5 rounded w-48" />
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">현재 상태</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                {['전체','활성','휴면','정지','탈퇴'].map(o => <option key={o}>{o}</option>)}
              </select>
            </div>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
          </div>

          <div className="grid grid-cols-2 gap-4">
            {/* 회원 목록 */}
            <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
              <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">회원 선택</div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 text-xs text-gray-500">
                    {['고객번호','이름','현재 상태','위험등급'].map(h => (
                      <th key={h} className="px-3 py-2 text-left font-medium">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {MOCK_MEMBERS.map(m => (
                    <tr
                      key={m.id}
                      className={`cursor-pointer hover:bg-yellow-50 ${selected === m.id ? 'bg-yellow-50 border-l-2 border-yellow-400' : ''}`}
                      onClick={() => setSelected(m.id)}
                    >
                      <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{m.id}</td>
                      <td className="px-3 py-2.5 font-medium">{m.name}</td>
                      <td className="px-3 py-2.5">
                        <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[m.status]}`}>{m.status}</span>
                      </td>
                      <td className="px-3 py-2.5 text-xs text-gray-500">{m.riskLevel}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* 상태 변경 패널 */}
            <div className="bg-white border border-kb-border rounded-lg shadow-sm">
              <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">상태 변경</div>
              {selectedMember ? (
                <div className="p-4">
                  <div className="mb-4 bg-kb-beige-light rounded p-3 text-sm">
                    <div className="grid grid-cols-2 gap-2">
                      <div><span className="text-xs text-gray-400">고객번호</span><p className="font-mono font-medium">{selectedMember.id}</p></div>
                      <div><span className="text-xs text-gray-400">이름</span><p className="font-medium">{selectedMember.name}</p></div>
                      <div><span className="text-xs text-gray-400">현재 상태</span>
                        <span className={`inline-block mt-0.5 text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[selectedMember.status]}`}>{selectedMember.status}</span>
                      </div>
                      <div><span className="text-xs text-gray-400">위험등급</span><p className="font-medium">{selectedMember.riskLevel}</p></div>
                    </div>
                  </div>
                  <div className="mb-3">
                    <label className="text-xs text-gray-500 block mb-1">변경할 상태</label>
                    <select
                      value={newStatus}
                      onChange={e => setNewStatus(e.target.value as MemberStatus)}
                      className="border border-gray-300 text-sm px-2 py-1.5 rounded bg-white w-full"
                    >
                      {(['활성','휴면','정지','탈퇴'] as MemberStatus[]).map(s => (
                        <option key={s} value={s}>{s}</option>
                      ))}
                    </select>
                  </div>
                  <div className="mb-3">
                    <label className="text-xs text-gray-500 block mb-1">변경 사유 <span className="text-red-500">*</span></label>
                    <textarea
                      value={reason}
                      onChange={e => setReason(e.target.value)}
                      placeholder="예: 이상거래 감지 (FDS Alert #2024), 고객 요청 등"
                      rows={3}
                      className="border border-gray-300 text-sm px-2 py-1.5 rounded w-full resize-none"
                    />
                  </div>
                  <div className="flex gap-2 justify-end">
                    <button onClick={() => setSelected(null)} className="px-3 py-1.5 border border-gray-300 text-sm rounded">취소</button>
                    <button className="px-3 py-1.5 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark transition-colors">상태 변경 확정</button>
                  </div>
                </div>
              ) : (
                <div className="p-8 text-center text-sm text-gray-400">
                  왼쪽 목록에서 회원을 선택하세요.
                </div>
              )}
            </div>
          </div>

          {/* 변경 이력 */}
          <div className="mt-4 bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">최근 상태 변경 이력</div>
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['변경일시','고객번호','이름','이전 상태','변경 상태','사유','처리자','비고'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {MOCK_STATUS_CHANGES.map((r, i) => (
                  <tr key={i} className="hover:bg-kb-beige-light">
                    <td className="px-3 py-2.5 text-xs text-gray-400">{r.changedAt}</td>
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{r.customerId}</td>
                    <td className="px-3 py-2.5 font-medium">{r.name}</td>
                    <td className="px-3 py-2.5"><span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[r.fromStatus]}`}>{r.fromStatus}</span></td>
                    <td className="px-3 py-2.5"><span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[r.toStatus]}`}>{r.toStatus}</span></td>
                    <td className="px-3 py-2.5 text-xs text-gray-600">{r.reason}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{r.processor}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{r.note}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  )
}
