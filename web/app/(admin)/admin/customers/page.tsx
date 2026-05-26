'use client'

import { useEffect, useState } from 'react'
import {
  AdminUser, AdminRole,
  MOCK_CUSTOMERS, ROLE_LABELS,
  applyMasking, requiresReason,
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

export default function CustomersPage() {
  const [adminUser, setAdminUser] = useState<AdminUser | null>(null)
  const [reasonModal, setReasonModal] = useState<{ customerId: string } | null>(null)
  const [reasonInput, setReasonInput] = useState('')
  const [unlockedIds, setUnlockedIds] = useState<Set<string>>(new Set())
  const [search, setSearch] = useState('')

  useEffect(() => {
    try {
      const stored = localStorage.getItem('admin_user')
      if (stored) setAdminUser(JSON.parse(stored))
    } catch {}
  }, [])

  if (!adminUser) return null

  const role = adminUser.role
  const isOtherBranch = role === 'ROLE_OTHER_BRANCH'
  const isHQ = role.startsWith('ROLE_HQ')
  const needsReason = requiresReason(role)

  const baseCustomers = isOtherBranch
    ? []
    : isHQ
      ? MOCK_CUSTOMERS
      : MOCK_CUSTOMERS.filter((c) => c.branchId === adminUser.branchId)

  const filtered = baseCustomers.filter((c) =>
    c.name.includes(search) || c.accountNumber.includes(search)
  )

  function handleRowClick(customerId: string) {
    if (!needsReason || unlockedIds.has(customerId)) return
    setReasonModal({ customerId })
    setReasonInput('')
  }

  function handleReasonConfirm() {
    if (!reasonInput.trim() || !reasonModal) return
    setUnlockedIds((prev) => new Set([...prev, reasonModal.customerId]))
    setReasonModal(null)
  }

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />

      {/* 본문 */}
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-8 py-4 flex items-center justify-between">
          <h1 className="text-lg font-bold text-gray-800">고객 조회</h1>
          <span className={`text-xs px-2 py-1 rounded-full font-medium ${ROLE_BADGE_COLOR[role]}`}>
            {ROLE_LABELS[role]}
          </span>
        </div>

        <div className="px-8 py-6">
          {/* 접근 차단 */}
          {isOtherBranch ? (
            <div className="bg-red-50 border border-red-200 rounded px-6 py-8 text-center">
              <p className="text-3xl mb-3">🚫</p>
              <p className="text-base font-bold text-red-700">접근 권한이 없습니다</p>
              <p className="text-sm text-red-500 mt-2">
                타 지점 직원은 고객 데이터에 직접 접근할 수 없습니다.<br />
                고객 스마트폰 인증 또는 신분증 확인 후 임시 권한을 요청하세요.
              </p>
            </div>
          ) : (
            <>
              {/* 마스킹 안내 */}
              {(role === 'ROLE_HQ_RISK' || role === 'ROLE_HQ_MARKETING') && (
                <div className="mb-4 bg-yellow-50 border border-yellow-200 rounded px-4 py-3 text-sm text-yellow-700 flex items-center gap-2">
                  <span>🔒</span>
                  <span>개인식별정보(PII)가 마스킹 처리되어 표시됩니다.</span>
                </div>
              )}
              {needsReason && (
                <div className="mb-4 bg-blue-50 border border-blue-200 rounded px-4 py-3 text-sm text-blue-700 flex items-center gap-2">
                  <span>ℹ️</span>
                  <span>상세 정보 조회 시 조회 사유를 입력해야 합니다.</span>
                </div>
              )}

              {/* 검색 */}
              <div className="mb-4">
                <input
                  type="text"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="이름 또는 계좌번호 검색"
                  className="border border-gray-300 px-3 py-2 text-sm rounded outline-none focus:border-blue-400 w-64"
                />
              </div>

              {/* 테이블 */}
              <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="bg-kb-beige-light text-xs text-kb-text-muted uppercase border-b border-kb-border">
                      <th className="px-5 py-3 text-left font-medium">이름</th>
                      <th className="px-5 py-3 text-left font-medium">주민번호</th>
                      <th className="px-5 py-3 text-left font-medium">연락처</th>
                      <th className="px-5 py-3 text-left font-medium">계좌번호</th>
                      <th className="px-5 py-3 text-left font-medium">잔액</th>
                      <th className="px-5 py-3 text-left font-medium">지점</th>
                      {role !== 'ROLE_HQ_MARKETING' && (
                        <th className="px-5 py-3 text-left font-medium">리스크</th>
                      )}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {filtered.map((c) => {
                      const isLocked = needsReason && !unlockedIds.has(c.id)
                      const masked = applyMasking(c, role)!
                      return (
                        <tr
                          key={c.id}
                          onClick={() => handleRowClick(c.id)}
                          className={`transition-colors ${needsReason && isLocked ? 'cursor-pointer hover:bg-blue-50' : 'hover:bg-kb-beige-light'}`}
                        >
                          <td className="px-5 py-3 font-medium text-gray-800">
                            {isLocked ? (
                              <span className="flex items-center gap-1.5">
                                {masked.name}
                                <span className="text-xs text-blue-500">🔒 클릭하여 조회</span>
                              </span>
                            ) : masked.name}
                          </td>
                          <td className="px-5 py-3 text-gray-500 font-mono text-xs">
                            {isLocked ? '██████-███████' : masked.ssn}
                          </td>
                          <td className="px-5 py-3 text-gray-500">
                            {isLocked ? '010-████-████' : masked.phone}
                          </td>
                          <td className="px-5 py-3 text-gray-500 font-mono text-xs">
                            {isLocked ? '███-███-██████' : masked.accountNumber}
                          </td>
                          <td className="px-5 py-3 text-gray-700 font-medium">
                            {role === 'ROLE_HQ_MARKETING'
                              ? '-'
                              : isLocked
                                ? '-'
                                : `${masked.balance.toLocaleString()}원`}
                          </td>
                          <td className="px-5 py-3 text-gray-500">{c.branchName}</td>
                          {role !== 'ROLE_HQ_MARKETING' && (
                            <td className="px-5 py-3">
                              {isLocked ? '-' : <RiskBadge score={c.riskScore} />}
                            </td>
                          )}
                        </tr>
                      )
                    })}
                    {filtered.length === 0 && (
                      <tr>
                        <td colSpan={7} className="px-5 py-8 text-center text-gray-400 text-sm">
                          검색 결과가 없습니다.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
              <p className="mt-2 text-xs text-gray-400">총 {filtered.length}건</p>
            </>
          )}
        </div>
      </main>

      {/* 조회 사유 모달 */}
      {reasonModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white shadow-xl rounded w-[400px]">
            <div className="px-6 py-4 border-b border-gray-200">
              <p className="text-sm font-bold text-gray-800">조회 사유 입력</p>
              <p className="text-xs text-gray-500 mt-0.5">
                지점 직원의 상세 데이터 조회는 사유 입력이 필수입니다.
              </p>
            </div>
            <div className="px-6 py-4">
              <textarea
                value={reasonInput}
                onChange={(e) => setReasonInput(e.target.value)}
                placeholder="조회 사유를 입력하세요 (예: 대출 상담 요청, 이체 오류 민원 처리 등)"
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm outline-none focus:border-blue-400 resize-none h-24"
              />
            </div>
            <div className="px-6 pb-5 flex justify-end gap-2">
              <button
                onClick={() => setReasonModal(null)}
                className="px-4 py-2 text-sm border border-gray-300 rounded text-gray-600 hover:bg-kb-beige-light"
              >
                취소
              </button>
              <button
                onClick={handleReasonConfirm}
                disabled={!reasonInput.trim()}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded font-medium hover:bg-blue-700 disabled:opacity-50"
              >
                확인 및 조회
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function RiskBadge({ score }: { score: number }) {
  if (score >= 70) return <span className="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-600 font-medium">고위험</span>
  if (score >= 40) return <span className="text-xs px-2 py-0.5 rounded-full bg-yellow-100 text-yellow-600 font-medium">중위험</span>
  return <span className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-600 font-medium">저위험</span>
}
