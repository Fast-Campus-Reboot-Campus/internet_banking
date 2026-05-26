'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import {
  AdminUser,
  MOCK_CUSTOMERS, MOCK_AUDIT_LOGS,
  canViewAuditLog,
} from '@/lib/admin-mock-data'
import AdminSidebar from '@/components/admin/AdminSidebar'

export default function AdminDashboardPage() {
  const [adminUser, setAdminUser] = useState<AdminUser | null>(null)

  useEffect(() => {
    try {
      const stored = localStorage.getItem('admin_user')
      if (stored) setAdminUser(JSON.parse(stored))
    } catch {}
  }, [])

  if (!adminUser) return null

  const role = adminUser.role
  const isHQ = role.startsWith('ROLE_HQ')
  const isOtherBranch = role === 'ROLE_OTHER_BRANCH'

  const visibleCustomers = isOtherBranch
    ? []
    : isHQ
      ? MOCK_CUSTOMERS
      : MOCK_CUSTOMERS.filter((c) => c.branchId === adminUser.branchId)

  const recentLogs = MOCK_AUDIT_LOGS.slice(0, 4)

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
          {isOtherBranch && (
            <div className="bg-red-50 border border-red-200 rounded px-4 py-3 flex items-start gap-3">
              <span className="text-red-500 text-lg">🚫</span>
              <div>
                <p className="text-sm font-semibold text-red-700">접근 제한 계정</p>
                <p className="text-sm text-red-600 mt-0.5">
                  타 지점 직원은 원칙적으로 고객 데이터에 접근할 수 없습니다.
                  접근이 필요한 경우 고객 스마트폰 인증 또는 신분증 확인 후 임시 권한을 요청하세요.
                </p>
              </div>
            </div>
          )}

          {role === 'ROLE_HQ_RISK' || role === 'ROLE_HQ_MARKETING' ? (
            <div className="bg-yellow-50 border border-yellow-200 rounded px-4 py-3 flex items-start gap-3">
              <span className="text-yellow-600 text-lg">🔒</span>
              <div>
                <p className="text-sm font-semibold text-yellow-800">개인식별정보(PII) 원천 차단</p>
                <p className="text-sm text-yellow-700 mt-0.5">
                  이름, 주민번호, 계좌번호 등 개인정보는 마스킹 처리된 데이터만 조회됩니다.
                </p>
              </div>
            </div>
          ) : null}

          {/* 통계 카드 */}
          <div className="grid grid-cols-3 gap-4">
            <StatCard
              label="접근 가능 고객 수"
              value={isOtherBranch ? '0' : visibleCustomers.length.toString()}
              sub={isHQ ? '전 지점' : adminUser.branchName}
              color="blue"
            />
            <StatCard
              label="오늘 감사 로그"
              value={canViewAuditLog(role) ? MOCK_AUDIT_LOGS.length.toString() : '-'}
              sub={canViewAuditLog(role) ? '건' : '조회 권한 없음'}
              color="green"
            />
            <StatCard
              label="내 접근 이력"
              value={MOCK_AUDIT_LOGS.filter((l) => l.accessorId === adminUser.id).length.toString()}
              sub="오늘"
              color="gray"
            />
          </div>

          {/* 접근 가능 고객 목록 (미리보기) */}
          {!isOtherBranch && (
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
                    <th className="px-5 py-2.5 text-left font-medium">지점</th>
                    <th className="px-5 py-2.5 text-left font-medium">리스크 등급</th>
                    <th className="px-5 py-2.5 text-left font-medium">가입일</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {visibleCustomers.slice(0, 4).map((c) => (
                    <tr key={c.id} className="hover:bg-kb-beige-light transition-colors">
                      <td className="px-5 py-3 font-medium text-gray-800">
                        {role === 'ROLE_HQ_RISK' || role === 'ROLE_HQ_MARKETING'
                          ? c.name[0] + '*'.repeat(c.name.length - 1)
                          : c.name}
                      </td>
                      <td className="px-5 py-3 text-gray-500">{c.branchName}</td>
                      <td className="px-5 py-3">
                        {role === 'ROLE_HQ_MARKETING' ? (
                          <span className="text-gray-400">-</span>
                        ) : (
                          <RiskBadge score={c.riskScore} />
                        )}
                      </td>
                      <td className="px-5 py-3 text-gray-500">{c.joinedAt}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          )}

          {/* 최근 감사 로그 */}
          {canViewAuditLog(role) && (
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
                  {recentLogs.map((log) => (
                    <tr key={log.id} className="hover:bg-kb-beige-light transition-colors">
                      <td className="px-5 py-3 text-gray-400 text-xs">{log.accessedAt}</td>
                      <td className="px-5 py-3 text-gray-700">{log.accessorName}</td>
                      <td className="px-5 py-3 text-gray-700">{log.targetCustomerName}</td>
                      <td className="px-5 py-3 text-gray-600">{log.action}</td>
                      <td className="px-5 py-3">
                        {log.reason
                          ? <span className="text-gray-600">{log.reason}</span>
                          : <span className="text-gray-300">-</span>}
                      </td>
                    </tr>
                  ))}
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

function RiskBadge({ score }: { score: number }) {
  if (score >= 70) return <span className="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-600 font-medium">고위험 ({score})</span>
  if (score >= 40) return <span className="text-xs px-2 py-0.5 rounded-full bg-yellow-100 text-yellow-600 font-medium">중위험 ({score})</span>
  return <span className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-600 font-medium">저위험 ({score})</span>
}
