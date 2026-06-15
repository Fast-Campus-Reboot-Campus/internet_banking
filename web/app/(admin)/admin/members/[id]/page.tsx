'use client'
import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import AdminSidebar from '@/components/admin/AdminSidebar'
import {
  getCustomerDetail, CustomerDetail, STATUS_LABEL,
  makeDormant, suspendCustomer, reactivateCustomer, closeCustomer,
  fmtYmd, fmtDate, fmtDateTime, errMsg,
} from '@/lib/admin-customer-api'

const STATUS_COLOR: Record<string, string> = {
  '활성': 'bg-green-100 text-green-700',
  '휴면': 'bg-gray-100 text-gray-500',
  '정지': 'bg-red-100 text-red-700',
  '탈퇴': 'bg-gray-100 text-gray-400',
}

export default function MemberDetailPage({ params }: { params: { id: string } }) {
  const customerId = Number(params.id)
  const [m, setM] = useState<CustomerDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [msg, setMsg] = useState<string | null>(null)

  const load = useCallback(() => {
    setError(null)
    getCustomerDetail(customerId).then(setM).catch(e => setError(errMsg(e, '회원 상세를 불러오지 못했습니다.')))
  }, [customerId])

  useEffect(() => { load() }, [load])

  async function act(fn: () => Promise<void>, ok: string) {
    setBusy(true); setMsg(null); setError(null)
    try { await fn(); setMsg(ok); setReason(''); load() }
    catch (e) { setError(errMsg(e, '상태 변경에 실패했습니다.')) }
    finally { setBusy(false) }
  }

  const status = m?.customerStatusCode
  const label = status ? (STATUS_LABEL[status] ?? status) : '-'

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          회원관리 &gt; <Link href="/admin/members" className="hover:underline">회원 목록</Link> &gt; <span className="text-gray-700 font-medium">회원 상세</span>
        </div>
        <div className="px-6 py-5 max-w-4xl">
          <div className="flex items-center justify-between mb-5">
            <h1 className="text-lg font-bold text-gray-800">회원 상세 — {customerId}</h1>
            <Link href="/admin/members" className="text-xs border border-gray-300 px-3 py-1.5 rounded text-gray-600 hover:bg-gray-50">← 목록으로</Link>
          </div>

          {error && <div className="mb-4 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>}
          {msg && <div className="mb-4 bg-green-50 border border-green-200 rounded px-4 py-2.5 text-xs text-green-700">{msg}</div>}

          {!m ? (
            <p className="text-sm text-gray-400">불러오는 중…</p>
          ) : (
            <>
              {/* 기본 정보 */}
              <div className="bg-white border border-kb-border rounded-lg mb-4 shadow-sm">
                <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light"><h2 className="text-sm font-semibold text-gray-700">기본 정보</h2></div>
                <div className="grid grid-cols-3 gap-0 divide-x divide-y divide-gray-100">
                  {[
                    ['고객번호', String(m.customerId)],
                    ['이름', m.partyName],
                    ['생년월일', fmtYmd(m.birthDate)],
                    ['성별', m.genderCode === 'M' ? '남' : m.genderCode === 'F' ? '여' : '-'],
                    ['국적', m.nationalityCode ?? '-'],
                    ['PEP', m.pep == null ? '-' : (m.pep ? '예' : '아니오')],
                    ['휴대폰', m.phone ?? '-'],
                    ['이메일', m.email ?? '-'],
                    ['가입일', fmtDate(m.joinedAt)],
                    ['최초가입일', fmtYmd(m.firstJoinDate)],
                    ['가입채널', m.joinChannelCode ?? '-'],
                    ['최근거래', fmtDateTime(m.lastTransactionAt)],
                  ].map(([k, v]) => (
                    <div key={k} className="px-4 py-3"><p className="text-xs text-gray-400 mb-0.5">{k}</p><p className="text-sm font-medium text-gray-800">{v}</p></div>
                  ))}
                </div>
              </div>

              {/* 상태 / 등급 */}
              <div className="bg-white border border-kb-border rounded-lg mb-4 shadow-sm">
                <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light"><h2 className="text-sm font-semibold text-gray-700">상태 / 등급</h2></div>
                <div className="grid grid-cols-4 gap-0 divide-x divide-gray-100">
                  <div className="px-4 py-3"><p className="text-xs text-gray-400 mb-0.5">회원 상태</p>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLOR[label] ?? 'bg-gray-100 text-gray-500'}`}>{label}</span></div>
                  <div className="px-4 py-3"><p className="text-xs text-gray-400 mb-0.5">고객 등급</p><p className="text-sm font-medium text-gray-800">{m.customerGradeCode ?? '-'}</p></div>
                  <div className="px-4 py-3"><p className="text-xs text-gray-400 mb-0.5">신용등급</p><p className="text-sm font-medium text-gray-800">{m.creditRatingCode ?? '-'}</p></div>
                  <div className="px-4 py-3"><p className="text-xs text-gray-400 mb-0.5">Party 상태</p><p className="text-sm font-medium text-gray-800">{m.partyStatusCode ?? '-'}</p></div>
                </div>
              </div>

              {/* 상태 변경 */}
              <div className="bg-white border border-kb-border rounded-lg mb-4 shadow-sm">
                <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light"><h2 className="text-sm font-semibold text-gray-700">상태 변경</h2></div>
                <div className="p-4">
                  <input value={reason} onChange={e => setReason(e.target.value)} placeholder="변경 사유 (예: 이상거래 감지, 고객 요청)"
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm mb-3" />
                  <div className="flex gap-2 flex-wrap">
                    {status === 'ACTIVE' && (
                      <button onClick={() => act(() => makeDormant(customerId, reason || undefined), '휴면 전환되었습니다.')} disabled={busy}
                        className="px-3 py-2 border border-gray-300 text-sm rounded text-gray-700 hover:bg-gray-50 disabled:opacity-50">휴면 전환</button>
                    )}
                    {(status === 'ACTIVE' || status === 'DORMANT') && (
                      <button onClick={() => act(() => suspendCustomer(customerId, reason || undefined), '정지 처리되었습니다.')} disabled={busy}
                        className="px-3 py-2 border border-red-400 text-sm rounded text-red-600 hover:bg-red-50 disabled:opacity-50">정지</button>
                    )}
                    {(status === 'DORMANT' || status === 'SUSPENDED') && (
                      <button onClick={() => act(() => reactivateCustomer(customerId, reason || undefined), '활성 복귀되었습니다.')} disabled={busy}
                        className="px-3 py-2 border border-green-500 text-sm rounded text-green-700 hover:bg-green-50 disabled:opacity-50">활성 복귀</button>
                    )}
                    {status !== 'CLOSED' && (
                      <button onClick={() => act(() => closeCustomer(customerId, { closeReasonCode: 'CUST_REQ', reasonDetail: reason || undefined }), '해지(탈퇴) 처리되었습니다.')} disabled={busy}
                        className="px-3 py-2 bg-red-500 text-white text-sm font-bold rounded hover:bg-red-600 disabled:opacity-50">해지(탈퇴)</button>
                    )}
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      </main>
    </div>
  )
}
