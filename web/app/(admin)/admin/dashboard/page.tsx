'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { AdminUser } from '@/lib/admin-mock-data'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { useAdminRoles } from '@/components/admin/RoleGate'
import { getAdminRoles, canViewAuditLog, isMaskingRole, isHeadOffice } from '@/lib/admin-auth'
import {
  searchCustomers, getAccessLogs,
  CustomerSummary, AccessLog, STATUS_LABEL, fmtDateTime, errMsg,
} from '@/lib/admin-customer-api'

/** access_action_code → 화면 라벨 */
const ACTION_LABEL: Record<string, string> = {
  CUSTOMER_DETAIL: '고객 상세 조회',
  CONTACT_VIEW: '연락처 조회',
}

export default function AdminDashboardPage() {
  const roles = useAdminRoles()
  const [adminUser, setAdminUser] = useState<AdminUser | null>(null)
  const [customers, setCustomers] = useState<CustomerSummary[]>([])
  const [customerTotal, setCustomerTotal] = useState(0)
  const [logs, setLogs] = useState<AccessLog[]>([])
  const [logTotal, setLogTotal] = useState(0)
  const [myLogTotal, setMyLogTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    try {
      const stored = localStorage.getItem('admin_user')
      if (stored) setAdminUser(JSON.parse(stored))
    } catch { /* noop */ }
  }, [])

  const load = useCallback(async (user: AdminUser) => {
    setLoading(true)
    setError(null)
    const roles = getAdminRoles()
    try {
      // 접근 가능 고객 — 역할/지점 스코프는 백엔드 internal API 가 적용한다.
      const c = await searchCustomers({ size: 4 })
      setCustomers(c.content)
      setCustomerTotal(c.totalElements)
      // 감사 로그 — 조회 권한 있는 역할만. '내 접근 이력'은 직원ID 필터가 없어 이름 키워드로 근사.
      if (canViewAuditLog(roles)) {
        const [recent, mine] = await Promise.all([
          getAccessLogs({ size: 4 }),
          getAccessLogs({ keyword: user.name, size: 1 }),
        ])
        setLogs(recent.content)
        setLogTotal(recent.totalElements)
        setMyLogTotal(mine.totalElements)
      } else {
        setLogs([]); setLogTotal(0); setMyLogTotal(0)
      }
    } catch (e) {
      setError(errMsg(e, '대시보드 데이터를 불러오지 못했습니다.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { if (adminUser) load(adminUser) }, [adminUser, load])

  if (!adminUser) return null

  const isHQ = isHeadOffice(roles)
  const masking = isMaskingRole(roles)
  const canAudit = canViewAuditLog(roles)
  const maskName = (n: string | null) =>
    n ? (masking ? n[0] + '*'.repeat(Math.max(0, n.length - 1)) : n) : '-'

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />

      {/* 본문 */}
      <main className="flex-1 overflow-auto">
        {/* 상단 바 */}
        <div className="bg-white border-b border-kb-border px-8 py-4 flex items-center justify-between">
          <h1 className="text-lg font-bold text-gray-800">대시보드</h1>
          <span className="text-sm text-gray-500">
            {new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })} 기준
          </span>
        </div>

        <div className="px-8 py-6 space-y-6">
          {/* 권한 안내 배너 */}
          {masking && (
            <div className="bg-yellow-50 border border-yellow-200 rounded px-4 py-3 flex items-start gap-3">
              <span className="text-yellow-600 text-lg">🔒</span>
              <div>
                <p className="text-sm font-semibold text-yellow-800">개인식별정보(PII) 원천 차단</p>
                <p className="text-sm text-yellow-700 mt-0.5">
                  이름, 주민번호, 계좌번호 등 개인정보는 마스킹 처리된 데이터만 조회됩니다.
                </p>
              </div>
            </div>
          )}

          {error && (
            <div className="bg-red-50 border border-red-200 rounded px-4 py-2.5 text-sm text-red-700">{error}</div>
          )}

          {/* 통계 카드 */}
          <div className="grid grid-cols-3 gap-4">
            <StatCard
              label="접근 가능 고객 수"
              value={loading ? '…' : customerTotal.toLocaleString()}
              sub={isHQ ? '전 지점' : adminUser.branchName}
              color="blue"
            />
            <StatCard
              label="감사 로그"
              value={loading ? '…' : (canAudit ? logTotal.toLocaleString() : '-')}
              sub={canAudit ? '전체 건' : '조회 권한 없음'}
              color="green"
            />
            <StatCard
              label="내 접근 이력"
              value={loading ? '…' : (canAudit ? myLogTotal.toLocaleString() : '-')}
              sub="건"
              color="gray"
            />
          </div>

          {/* 접근 가능 고객 목록 (미리보기) */}
          <section className="bg-white border border-kb-border rounded-lg shadow-sm">
              <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
                <h2 className="text-sm font-bold text-gray-700">접근 가능 고객</h2>
                <Link href="/admin/customers" className="text-xs text-blue-600 hover:underline">
                  전체 보기 →
                </Link>
              </div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-kb-beige-light text-xs text-kb-text-muted uppercase">
                    <th className="px-5 py-2.5 text-left font-medium">이름</th>
                    <th className="px-5 py-2.5 text-left font-medium">등급</th>
                    <th className="px-5 py-2.5 text-left font-medium">상태</th>
                    <th className="px-5 py-2.5 text-left font-medium">가입일</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {customers.map((c) => (
                    <tr key={c.customerId} className="hover:bg-kb-beige-light transition-colors">
                      <td className="px-5 py-3 font-medium text-gray-800">{maskName(c.partyName)}</td>
                      <td className="px-5 py-3 text-gray-600 text-xs">{c.customerGradeCode ?? '-'}</td>
                      <td className="px-5 py-3 text-gray-600 text-xs">{STATUS_LABEL[c.customerStatusCode] ?? c.customerStatusCode}</td>
                      <td className="px-5 py-3 text-gray-500">{c.joinedAt?.slice(0, 10) ?? '-'}</td>
                    </tr>
                  ))}
                  {!loading && customers.length === 0 && !error && (
                    <tr><td colSpan={4} className="px-5 py-8 text-center text-gray-400 text-sm">표시할 고객이 없습니다.</td></tr>
                  )}
                </tbody>
              </table>
            </section>

          {/* 최근 감사 로그 */}
          {canAudit && (
            <section className="bg-white border border-kb-border rounded-lg shadow-sm">
              <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
                <h2 className="text-sm font-bold text-gray-700">최근 감사 로그</h2>
                <Link href="/admin/audit-log" className="text-xs text-blue-600 hover:underline">
                  전체 보기 →
                </Link>
              </div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-kb-beige-light text-xs text-kb-text-muted uppercase">
                    <th className="px-5 py-2.5 text-left font-medium">시각</th>
                    <th className="px-5 py-2.5 text-left font-medium">접근자</th>
                    <th className="px-5 py-2.5 text-left font-medium">대상 고객</th>
                    <th className="px-5 py-2.5 text-left font-medium">행위</th>
                    <th className="px-5 py-2.5 text-left font-medium">조회 사유</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {logs.map((log) => (
                    <tr key={log.customerAccessLogId} className="hover:bg-kb-beige-light transition-colors">
                      <td className="px-5 py-3 text-gray-400 text-xs">{fmtDateTime(log.accessedAt)}</td>
                      <td className="px-5 py-3 text-gray-700">{log.accessorName ?? '-'}</td>
                      <td className="px-5 py-3 text-gray-700">{log.targetCustomerName ?? '-'}</td>
                      <td className="px-5 py-3 text-gray-600">{ACTION_LABEL[log.accessActionCode] ?? log.accessActionCode}</td>
                      <td className="px-5 py-3">
                        {log.accessReason
                          ? <span className="text-gray-600">{log.accessReason}</span>
                          : <span className="text-gray-300">-</span>}
                      </td>
                    </tr>
                  ))}
                  {!loading && logs.length === 0 && !error && (
                    <tr><td colSpan={5} className="px-5 py-8 text-center text-gray-400 text-sm">감사 로그가 없습니다.</td></tr>
                  )}
                </tbody>
              </table>
            </section>
          )}
        </div>
      </main>
    </div>
  )
}

function StatCard({ label, value, sub, color }: { label: string; value: string; sub: string; color: 'blue' | 'green' | 'gray' }) {
  const colors = {
    blue:  'border-blue-200 bg-blue-50 text-blue-700',
    green: 'border-green-200 bg-green-50 text-green-700',
    gray:  'border-gray-200 bg-gray-50 text-gray-700',
  }
  return (
    <div className={`border rounded px-5 py-4 ${colors[color]}`}>
      <p className="text-xs font-medium opacity-70">{label}</p>
      <p className="text-2xl font-bold mt-1">{value}</p>
      <p className="text-xs opacity-60 mt-0.5">{sub}</p>
    </div>
  )
}
