'use client'

import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { breakGlassApi } from '@/lib/loan-api'
import { useAdminRoles } from '@/components/admin/RoleGate'
import { isEmployee } from '@/lib/admin-auth'

export default function LoanBreakGlassPage() {
  const roles = useAdminRoles()
  const canUse = isEmployee(roles) // CUSTOMER 제외 전 직원

  const [applId, setApplId] = useState('')
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [result, setResult] = useState<{ logId: number; grantExpiresAt: string } | null>(null)

  function fail(m: string) { setErr(m); setTimeout(() => setErr(''), 5000) }

  async function submit() {
    if (!applId) { fail('신청 ID를 입력하세요.'); return }
    if (!reason.trim()) { fail('긴급 접근 사유는 필수입니다.'); return }
    setBusy(true); setErr(''); setResult(null)
    try {
      const res = await breakGlassApi.request({ applId: parseInt(applId), reason: reason.trim() })
      setResult(res.data)
    } catch (e: any) {
      fail(e?.response?.data?.message ?? '긴급 접근 처리 실패')
    } finally { setBusy(false) }
  }

  return (
    <div className="flex min-h-screen bg-gray-50">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-gray-200 px-6 py-3 text-xs text-gray-500">
          대출 &gt; <span className="text-gray-800 font-medium">긴급 접근(break-glass)</span>
        </div>

        <div className="px-6 py-5 max-w-2xl">
          <h1 className="text-lg font-bold text-gray-800 mb-1">긴급 접근 (break-glass)</h1>
          <p className="text-[12px] text-gray-500 mb-5">권한 범위를 벗어난 건에 대한 임시 접근을 신청합니다. 모든 신청은 감사로그에 기록되며 일정 시간 후 만료됩니다.</p>

          {roles.length === 0 ? (
            <p className="py-10 text-center text-sm text-gray-400">권한 확인 중...</p>
          ) : !canUse ? (
            <div className="px-4 py-3 bg-yellow-50 border border-yellow-300 text-yellow-800 text-sm rounded">
              이 기능은 직원(고객 제외) 권한이 필요합니다.
            </div>
          ) : (
            <>
              <div className="mb-4 px-4 py-3 bg-red-50 border border-red-200 text-red-700 text-[12px] rounded">
                ⚠ 긴급 접근은 사후 감사 대상입니다. 정당한 사유가 있을 때만 사용하세요.
              </div>

              {err && <div className="mb-4 px-4 py-2 bg-red-50 border border-red-300 text-red-700 text-sm rounded">{err}</div>}

              <div className="bg-white border border-gray-200 rounded-lg p-5 space-y-4">
                <div>
                  <label className="block text-[13px] font-semibold text-gray-700 mb-1.5">신청 ID (applId)</label>
                  <input type="number" value={applId} onChange={e => setApplId(e.target.value)}
                    className="w-full border border-gray-300 px-3 py-2 text-[13px] rounded focus:outline-none focus:border-blue-400" />
                </div>
                <div>
                  <label className="block text-[13px] font-semibold text-gray-700 mb-1.5">긴급 접근 사유</label>
                  <textarea value={reason} onChange={e => setReason(e.target.value)} rows={3}
                    placeholder="예) 고객 민원 처리를 위한 타지점 건 긴급 확인"
                    className="w-full border border-gray-300 px-3 py-2 text-[13px] rounded focus:outline-none focus:border-blue-400 resize-none" />
                </div>
                <button onClick={submit} disabled={busy}
                  className="w-full py-2.5 bg-red-600 text-white text-[13px] font-bold rounded hover:bg-red-700 disabled:opacity-50">
                  {busy ? '처리 중...' : '긴급 접근 신청'}
                </button>
              </div>

              {result && (
                <div className="mt-4 px-4 py-3 bg-green-50 border border-green-300 text-green-800 text-[13px] rounded">
                  긴급 접근이 부여되었습니다. (로그 #{result.logId})<br />
                  <span className="text-[12px] text-green-700">만료: {result.grantExpiresAt?.slice(0, 19).replace('T', ' ') ?? '-'}</span>
                </div>
              )}
            </>
          )}
        </div>
      </main>
    </div>
  )
}
