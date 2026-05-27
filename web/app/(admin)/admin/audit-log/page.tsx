'use client'

import { useEffect, useState } from 'react'
import {
  AdminUser, AdminRole, AuditLog,
  MOCK_AUDIT_LOGS, ROLE_LABELS, canViewAuditLog,
} from '@/lib/admin-mock-data'
import AdminSidebar from '@/components/admin/AdminSidebar'

const ROLE_BADGE_COLOR: Record<AdminRole, string> = {
  ROLE_HQ_AUDIT:      'bg-red-100 text-red-700',
  ROLE_HQ_REVIEW:     'bg-orange-100 text-orange-700',
  ROLE_HQ_RISK:       'bg-yellow-100 text-yellow-700',
  ROLE_HQ_MARKETING:  'bg-purple-100 text-purple-700',
  ROLE_PRIMARY_OWNER: 'bg-blue-100 text-blue-700',
  ROLE_BRANCH_STAFF:  'bg-green-100 text-green-700',
  ROLE_OTHER_BRANCH:  'bg-gray-100 text-gray-500',
}

export default function AuditLogPage() {
  const [adminUser, setAdminUser] = useState<AdminUser | null>(null)
  const [search, setSearch] = useState('')

  useEffect(() => {
    try {
      const stored = localStorage.getItem('admin_user')
      if (stored) setAdminUser(JSON.parse(stored))
    } catch {}
  }, [])

  if (!adminUser) return null

  const role = adminUser.role

  if (!canViewAuditLog(role)) {
    return (
      <div className="flex min-h-screen bg-kb-beige-light">
        <AdminSidebar />
        <main className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <p className="text-3xl mb-3">🚫</p>
            <p className="text-base font-bold text-gray-700">감사 로그 조회 권한이 없습니다</p>
            <p className="text-sm text-gray-400 mt-1">{ROLE_LABELS[role]} 역할은 감사 로그에 접근할 수 없습니다.</p>
          </div>
        </main>
      </div>
    )
  }

  // BRANCH_STAFF는 자기 지점 로그만 조회
  const baseLog: AuditLog[] =
    role === 'ROLE_BRANCH_STAFF'
      ? MOCK_AUDIT_LOGS.filter((l) => l.branchId === adminUser.branchId)
      : MOCK_AUDIT_LOGS

  // HQ_REVIEW는 본인에게 배정된 심사 건만
  const filtered = (
    role === 'ROLE_HQ_REVIEW'
      ? baseLog.filter((l) => l.accessorId === adminUser.id)
      : baseLog
  ).filter((l) =>
    l.accessorName.includes(search) ||
    l.targetCustomerName.includes(search) ||
    l.action.includes(search)
  )

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />

      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-8 py-4 flex items-center justify-between">
          <h1 className="text-lg font-bold text-gray-800">감사 로그</h1>
          <div className="flex items-center gap-3">
            <span className="text-xs text-gray-400">모든 조회 이력은 영구 적재됩니다</span>
            <span className={`text-xs px-2 py-1 rounded-full font-medium ${ROLE_BADGE_COLOR[role]}`}>
              {ROLE_LABELS[role]}
            </span>
          </div>
        </div>

        <div className="px-8 py-6">
          {/* 접근 범위 안내 */}
          <div className="mb-4 bg-kb-beige-light border border-kb-border rounded px-4 py-3 text-sm text-gray-600">
            {role === 'ROLE_HQ_AUDIT' && '전 지점 모든 접근 이력을 조회할 수 있습니다.'}
            {role === 'ROLE_HQ_REVIEW' && '본인에게 자동 배정된 심사 건의 접근 이력만 조회됩니다.'}
            {role === 'ROLE_PRIMARY_OWNER' && '담당 고객에 대한 접근 이력을 조회할 수 있습니다.'}
            {role === 'ROLE_BRANCH_STAFF' && '소속 지점 내 접근 이력만 조회됩니다.'}
          </div>

          {/* 검색 */}
          <div className="mb-4">
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="접근자명, 고객명, 행위 검색"
              className="border border-gray-300 px-3 py-2 text-sm rounded outline-none focus:border-blue-400 w-64"
            />
          </div>

          {/* 테이블 */}
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
                {filtered.map((log) => (
                  <tr key={log.id} className="hover:bg-kb-beige-light transition-colors">
                    <td className="px-5 py-3 text-gray-400 text-xs font-mono whitespace-nowrap">
                      {log.accessedAt}
                    </td>
                    <td className="px-5 py-3 text-gray-800 font-medium">{log.accessorName}</td>
                    <td className="px-5 py-3">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${ROLE_BADGE_COLOR[log.accessorRole]}`}>
                        {ROLE_LABELS[log.accessorRole].split(' ')[0]}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-gray-700">{log.targetCustomerName}</td>
                    <td className="px-5 py-3 text-gray-600">{log.action}</td>
                    <td className="px-5 py-3">
                      {log.reason
                        ? <span className="text-gray-700">{log.reason}</span>
                        : <span className="text-gray-300 text-xs">-</span>}
                    </td>
                  </tr>
                ))}
                {filtered.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-5 py-8 text-center text-gray-400 text-sm">
                      조회된 이력이 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <p className="mt-2 text-xs text-gray-400">총 {filtered.length}건</p>
        </div>
      </main>
    </div>
  )
}

