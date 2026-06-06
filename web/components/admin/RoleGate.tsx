'use client'

import { useEffect, useState } from 'react'
import { getAdminRoles, hasAnyRole } from '@/lib/admin-auth'

/**
 * 관리자 콘솔에서 현재 로그인 직원의 실제 역할 배열을 반환하는 훅.
 * 하이드레이션 불일치를 피하려 마운트 후 localStorage 에서 읽는다(초기엔 빈 배열).
 */
export function useAdminRoles(): string[] {
  const [roles, setRoles] = useState<string[]>([])
  useEffect(() => { setRoles(getAdminRoles()) }, [])
  return roles
}

/**
 * 역할 기반 show/hide 래퍼. anyOf 중 하나라도 보유하면 children 을, 아니면 fallback(기본 null)을 렌더.
 * ROLE_ADMIN 은 항상 통과(hasAnyRole 규칙).
 *
 * <RoleGate anyOf={[BankRole.BRANCH_MANAGER]}><Btn label="최종 결재" .../></RoleGate>
 */
export default function RoleGate({
  anyOf,
  children,
  fallback = null,
}: {
  anyOf: string[]
  children: React.ReactNode
  fallback?: React.ReactNode
}) {
  const roles = useAdminRoles()
  return <>{hasAnyRole(roles, ...anyOf) ? children : fallback}</>
}
