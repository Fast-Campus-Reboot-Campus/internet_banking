'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { ADMIN_ACCOUNTS, ROLE_LABELS, AdminRole } from '@/lib/admin-mock-data'

const ROLE_DESCRIPTIONS: Record<AdminRole, string> = {
  ROLE_HQ_AUDIT:      '전 지점 모든 데이터 Full Access',
  ROLE_HQ_REVIEW:     '전 지점 대출 신청 고객 데이터 조회',
  ROLE_HQ_RISK:       '전 지점 자산/연체 데이터 가공본 (PII 마스킹)',
  ROLE_HQ_MARKETING:  '전 지점 고객 통계 데이터 (PII 마스킹)',
  ROLE_PRIMARY_OWNER: '담당 고객 상세 데이터 Full Access',
  ROLE_BRANCH_STAFF:  '소속 지점 고객 공통 테이블, 조회 사유 필수',
  ROLE_OTHER_BRANCH:  '원칙적 접근 차단 (임시 권한 위임 방식)',
}

const ROLE_BADGE_COLOR: Record<AdminRole, string> = {
  ROLE_HQ_AUDIT:      'bg-red-100 text-red-700',
  ROLE_HQ_REVIEW:     'bg-orange-100 text-orange-700',
  ROLE_HQ_RISK:       'bg-yellow-100 text-yellow-700',
  ROLE_HQ_MARKETING:  'bg-purple-100 text-purple-700',
  ROLE_PRIMARY_OWNER: 'bg-blue-100 text-blue-700',
  ROLE_BRANCH_STAFF:  'bg-green-100 text-green-700',
  ROLE_OTHER_BRANCH:  'bg-gray-100 text-gray-500',
}

export default function AdminLoginPage() {
  const router = useRouter()
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  function handleLogin() {
    if (!selectedId) { setError('계정을 선택해주세요.'); return }
    if (!password)   { setError('비밀번호를 입력해주세요.'); return }

    const account = ADMIN_ACCOUNTS.find((a) => a.id === selectedId)!
    // 목업: 비밀번호 "admin1234" 고정
    if (password !== 'admin1234') {
      setError('비밀번호가 올바르지 않습니다. (힌트: admin1234)')
      return
    }

    localStorage.setItem('admin_role', account.role)
    localStorage.setItem('admin_user', JSON.stringify(account))
    router.push('/admin/dashboard')
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="w-full max-w-2xl bg-white shadow-lg">
        {/* 헤더 */}
        <div className="px-8 py-6 border-b border-gray-200" style={{ backgroundColor: '#1a3a5c' }}>
          <p className="text-xs text-blue-300 mb-1">AXful Bank</p>
          <h1 className="text-xl font-bold text-white">관리자 시스템 로그인</h1>
          <p className="text-sm text-blue-200 mt-1">접근 권한에 따라 열람 가능한 데이터가 제한됩니다</p>
        </div>

        <div className="px-8 py-6">
          {/* 계정 선택 */}
          <p className="text-sm font-semibold text-gray-700 mb-3">계정 선택</p>
          <div className="space-y-2 mb-6 max-h-72 overflow-y-auto pr-1">
            {ADMIN_ACCOUNTS.map((account) => (
              <button
                key={account.id}
                onClick={() => { setSelectedId(account.id); setError('') }}
                className={`w-full text-left px-4 py-3 border rounded transition-colors
                  ${selectedId === account.id
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                  }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold
                      ${selectedId === account.id ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-600'}`}>
                      {account.name[0]}
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-gray-800">{account.name}</p>
                      <p className="text-xs text-gray-500">{account.branchName}</p>
                    </div>
                  </div>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${ROLE_BADGE_COLOR[account.role]}`}>
                    {ROLE_LABELS[account.role]}
                  </span>
                </div>
                {selectedId === account.id && (
                  <p className="text-xs text-blue-600 mt-2 pl-11">{ROLE_DESCRIPTIONS[account.role]}</p>
                )}
              </button>
            ))}
          </div>

          {/* 비밀번호 */}
          <div className="mb-4">
            <label className="block text-sm font-semibold text-gray-700 mb-1.5">비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => { setPassword(e.target.value); setError('') }}
              onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
              placeholder="비밀번호를 입력하세요"
              className="w-full border border-gray-300 px-3 py-2.5 text-sm outline-none focus:border-blue-400 rounded"
            />
          </div>

          {error && <p className="text-sm text-red-500 mb-3">{error}</p>}

          <button
            onClick={handleLogin}
            className="w-full py-3 text-sm font-bold text-white rounded transition-colors"
            style={{ backgroundColor: '#1a3a5c' }}
            onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#122a44')}
            onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = '#1a3a5c')}
          >
            로그인
          </button>

          <p className="text-center text-xs text-gray-400 mt-4">
            본 시스템의 모든 접근 이력은 감사 로그에 기록됩니다
          </p>
        </div>
      </div>
    </div>
  )
}
