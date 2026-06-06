'use client'
import { useCallback, useEffect, useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import {
  searchCustomers, CustomerSummary, STATUS_LABEL,
  makeDormant, suspendCustomer, reactivateCustomer, closeCustomer, errMsg,
} from '@/lib/admin-customer-api'

const STATUS_COLOR: Record<string, string> = {
  '활성': 'bg-green-100 text-green-700',
  '휴면': 'bg-gray-100 text-gray-500',
  '정지': 'bg-red-100 text-red-700',
  '탈퇴': 'bg-gray-100 text-gray-400',
}
const NEW_STATUSES = ['활성', '휴면', '정지', '탈퇴'] as const
type NewStatus = (typeof NEW_STATUSES)[number]

export default function MemberStatusPage() {
  const [rows, setRows] = useState<CustomerSummary[]>([])
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState<CustomerSummary | null>(null)
  const [newStatus, setNewStatus] = useState<NewStatus>('정지')
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    searchCustomers({ keyword: search.trim() || undefined, size: 50 })
      .then(res => setRows(res.content))
      .catch(e => setError(errMsg(e, '회원 목록을 불러오지 못했습니다.')))
      .finally(() => setLoading(false))
  }, [search])

  useEffect(() => { load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  async function confirmChange() {
    if (!selected) return
    if (!reason.trim()) { setError('변경 사유를 입력하세요.'); return }
    setBusy(true); setError(null); setMsg(null)
    const cid = selected.customerId
    try {
      if (newStatus === '활성') await reactivateCustomer(cid, reason)
      else if (newStatus === '휴면') await makeDormant(cid, reason)
      else if (newStatus === '정지') await suspendCustomer(cid, reason)
      else await closeCustomer(cid, { closeReasonCode: 'CUST_REQ', reasonDetail: reason })
      setMsg(`${selected.partyName} 회원을 '${newStatus}'(으)로 변경했습니다.`)
      setReason(''); setSelected(null); load()
    } catch (e) {
      setError(errMsg(e, '상태 변경에 실패했습니다.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          회원관리 &gt; <span className="text-gray-700 font-medium">회원 상태 관리</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">회원 상태 관리 (정지 / 해제 / 해지)</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <input value={search} onChange={e => setSearch(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') load() }}
              placeholder="이름 / 휴대폰" className="border border-gray-300 text-xs px-2 py-1.5 rounded w-48" />
            <button onClick={load} disabled={loading} className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark disabled:opacity-50">{loading ? '조회 중…' : '조회'}</button>
          </div>

          {error && <div className="mb-3 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>}
          {msg && <div className="mb-3 bg-green-50 border border-green-200 rounded px-4 py-2.5 text-xs text-green-700">{msg}</div>}

          <div className="grid grid-cols-2 gap-4">
            {/* 회원 목록 */}
            <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
              <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">회원 선택</div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 text-xs text-gray-500">
                    {['고객번호', '이름', '현재 상태', '휴대폰'].map(h => <th key={h} className="px-3 py-2 text-left font-medium">{h}</th>)}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {rows.map(m => {
                    const label = STATUS_LABEL[m.customerStatusCode] ?? m.customerStatusCode
                    return (
                      <tr key={m.customerId} onClick={() => setSelected(m)}
                        className={`cursor-pointer hover:bg-yellow-50 ${selected?.customerId === m.customerId ? 'bg-yellow-50 border-l-2 border-yellow-400' : ''}`}>
                        <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{m.customerId}</td>
                        <td className="px-3 py-2.5 font-medium">{m.partyName}</td>
                        <td className="px-3 py-2.5"><span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[label] ?? 'bg-gray-100 text-gray-500'}`}>{label}</span></td>
                        <td className="px-3 py-2.5 text-xs text-gray-500">{m.phone ?? '-'}</td>
                      </tr>
                    )
                  })}
                  {!loading && rows.length === 0 && <tr><td colSpan={4} className="px-3 py-6 text-center text-gray-400 text-sm">결과 없음</td></tr>}
                </tbody>
              </table>
            </div>

            {/* 상태 변경 패널 */}
            <div className="bg-white border border-kb-border rounded-lg shadow-sm">
              <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">상태 변경</div>
              {selected ? (
                <div className="p-4">
                  <div className="mb-4 bg-kb-beige-light rounded p-3 text-sm grid grid-cols-2 gap-2">
                    <div><span className="text-xs text-gray-400">고객번호</span><p className="font-mono font-medium">{selected.customerId}</p></div>
                    <div><span className="text-xs text-gray-400">이름</span><p className="font-medium">{selected.partyName}</p></div>
                    <div><span className="text-xs text-gray-400">현재 상태</span>
                      <span className={`inline-block mt-0.5 text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[STATUS_LABEL[selected.customerStatusCode]] ?? 'bg-gray-100 text-gray-500'}`}>{STATUS_LABEL[selected.customerStatusCode] ?? selected.customerStatusCode}</span>
                    </div>
                  </div>
                  <div className="mb-3">
                    <label className="text-xs text-gray-500 block mb-1">변경할 상태</label>
                    <select value={newStatus} onChange={e => setNewStatus(e.target.value as NewStatus)} className="border border-gray-300 text-sm px-2 py-1.5 rounded bg-white w-full">
                      {NEW_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </div>
                  <div className="mb-3">
                    <label className="text-xs text-gray-500 block mb-1">변경 사유 <span className="text-red-500">*</span></label>
                    <textarea value={reason} onChange={e => setReason(e.target.value)} rows={3}
                      placeholder="예: 이상거래 감지 (FDS Alert), 고객 요청 등" className="border border-gray-300 text-sm px-2 py-1.5 rounded w-full resize-none" />
                  </div>
                  <div className="flex gap-2 justify-end">
                    <button onClick={() => setSelected(null)} className="px-3 py-1.5 border border-gray-300 text-sm rounded">취소</button>
                    <button onClick={confirmChange} disabled={busy} className="px-3 py-1.5 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark disabled:opacity-50">{busy ? '처리 중…' : '상태 변경 확정'}</button>
                  </div>
                </div>
              ) : (
                <div className="p-8 text-center text-sm text-gray-400">왼쪽 목록에서 회원을 선택하세요.</div>
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
