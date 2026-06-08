'use client'

import { useCallback, useEffect, useState } from 'react'
import { AdminUser } from '@/lib/admin-mock-data'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { useAdminRoles } from '@/components/admin/RoleGate'
import { canViewAuditLog, primaryRoleLabel } from '@/lib/admin-auth'
import { getAccessLogs, AccessLog, fmtDateTime, errMsg } from '@/lib/admin-customer-api'

// 백엔드 accessorRole 은 BankRole grade_code 스냅샷이다(프론트 AdminRole 과 어휘가 다름).
const BANK_ROLE_LABEL: Record<string, string> = {
  COMPLIANCE: '컴플라이언스', HQ_REVIEWER: '본사 심사', HQ_RISK: '본사 리스크',
  HQ_MARKETING: '본사 마케팅', BRANCH_MANAGER: '지점장', DEPUTY_MANAGER: '부지점장', TELLER: '창구직원',
}
const ACTION_LABEL: Record<string, string> = {
  CUSTOMER_DETAIL: '고객 상세 조회', CONTACT_VIEW: '연락처 열람',
}
const roleLabel = (r: string | null) => (r ? BANK_ROLE_LABEL[r] ?? r : '-')
const actionLabel = (a: string) => ACTION_LABEL[a] ?? a

export default function AuditLogPage() {
  const roles = useAdminRoles()
  const [adminUser, setAdminUser] = useState<AdminUser | null>(null)
  const [rows, setRows] = useState<AccessLog[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    try {
      const stored = localStorage.getItem('admin_user')
      if (stored) setAdminUser(JSON.parse(stored))
    } catch { /* noop */ }
  }, [])

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    getAccessLogs({ keyword: search.trim() || undefined, size: 100 })
      .then(res => setRows(res.content))
      .catch(e => setError(errMsg(e, '감사 로그를 불러오지 못했습니다.')))
      .finally(() => setLoading(false))
  }, [search])

  const allowed = canViewAuditLog(roles)

  useEffect(() => { if (allowed) load() }, [allowed]) // eslint-disable-line react-hooks/exhaustive-deps

  if (!adminUser) return null

  if (!allowed) {
    return (
      <div className="flex min-h-screen bg-kb-beige-light">
        <AdminSidebar />
        <main className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <p className="text-3xl mb-3">🚫</p>
            <p className="text-base font-bold text-gray-700">감사 로그 조회 권한이 없습니다</p>
            <p className="text-sm text-gray-400 mt-1">{primaryRoleLabel(roles)} 역할은 감사 로그에 접근할 수 없습니다.</p>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />

      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-8 py-4 flex items-center justify-between">
          <h1 className="text-lg font-bold text-gray-800">감사 로그</h1>
          <span className="text-xs text-gray-400">모든 조회 이력은 영구 적재됩니다</span>
        </div>

        <div className="px-8 py-6">
          <div className="mb-4 flex gap-2">
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') load() }}
              placeholder="접근자명, 고객명, 행위 검색"
              className="border border-gray-300 px-3 py-2 text-sm rounded outline-none focus:border-blue-400 w-64"
            />
            <button onClick={load} disabled={loading}
              className="px-3 py-2 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark disabled:opacity-50">
              {loading ? '조회 중…' : '조회'}
            </button>
          </div>

          {error && <div className="mb-3 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>}

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light text-xs text-kb-text-muted uppercase border-b border-kb-border">
                  <th className="px-5 py-3 text-left font-medium">시각</th>
                  <th className="px-5 py-3 text-left font-medium">접근자</th>
                  <th className="px-5 py-3 text-left font-medium">역할</th>
                  <th className="px-5 py-3 text-left font-medium">대상 고객</th>
                  <th className="px-5 py-3 text-left font-medium">행위</th>
                  <th className="px-5 py-3 text-left font-medium">조회 사유</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map((log) => (
                  <tr key={log.customerAccessLogId} className="hover:bg-kb-beige-light transition-colors">
                    <td className="px-5 py-3 text-gray-400 text-xs font-mono whitespace-nowrap">{fmtDateTime(log.accessedAt)}</td>
                    <td className="px-5 py-3 text-gray-800 font-medium">{log.accessorName ?? `#${log.accessorEmployeeId}`}</td>
                    <td className="px-5 py-3">
                      <span className="text-xs px-1.5 py-0.5 rounded-full font-medium bg-gray-100 text-gray-600">{roleLabel(log.accessorRole)}</span>
                    </td>
                    <td className="px-5 py-3 text-gray-700">{log.targetCustomerName ?? `#${log.targetCustomerId}`}</td>
                    <td className="px-5 py-3 text-gray-600">{actionLabel(log.accessActionCode)}</td>
                    <td className="px-5 py-3">
                      {log.accessReason
                        ? <span className="text-gray-700">{log.accessReason}</span>
                        : <span className="text-gray-300 text-xs">-</span>}
                    </td>
                  </tr>
                ))}
                {!loading && rows.length === 0 && !error && (
                  <tr>
                    <td colSpan={6} className="px-5 py-8 text-center text-gray-400 text-sm">조회된 이력이 없습니다.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <p className="mt-2 text-xs text-gray-400">총 {rows.length}건</p>
        </div>
      </main>
    </div>
  )
}
