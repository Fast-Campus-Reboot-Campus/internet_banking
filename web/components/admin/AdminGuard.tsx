'use client'

import { useEffect, useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'

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
    const role = localStorage.getItem('admin_role')
    if (!role) {
      router.replace('/admin/login')
    } else {
      setAuthorized(true)
    }
  }, [pathname, isPublic, router])

  if (!authorized) return null
  return <>{children}</>
}
