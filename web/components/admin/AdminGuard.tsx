'use client'

import { useEffect, useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import { getAdminRoles, isEmployee } from '@/lib/admin-auth'

const PUBLIC_ADMIN_PATHS = ['/admin/login']

export default function AdminGuard({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const router = useRouter()
  const isPublic = PUBLIC_ADMIN_PATHS.some((p) => pathname.startsWith(p))
  const [authorized, setAuthorized] = useState(isPublic)

  useEffect(() => {
    if (isPublic) {
      setAuthorized(true)
      return
    }
    // 직원 역할(BankRole, CUSTOMER 제외)이 있어야 콘솔 접근 허용
    if (!isEmployee(getAdminRoles())) {
      router.replace('/admin/login')
    } else {
      setAuthorized(true)
    }
  }, [pathname, isPublic, router])

  if (!authorized) return null
  return <>{children}</>
}
