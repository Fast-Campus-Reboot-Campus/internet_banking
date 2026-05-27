'use client'

import { useEffect, useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'

// 로그인 없이 접근 가능한 경로 prefix
const PUBLIC_PREFIXES = [
  '/login',
  '/personal',
  '/banking',
  '/cert',
  '/cert-cps',
  '/cert-biz',
  '/products',
  '/support',
  '/security-install',
]

export default function AuthGuard({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const router = useRouter()

  const isPublic = PUBLIC_PREFIXES.some((p) => pathname.startsWith(p))
  const [authorized, setAuthorized] = useState(isPublic)

  useEffect(() => {
    if (isPublic) {
      setAuthorized(true)
      return
    }
    const token = localStorage.getItem('accessToken')
    if (!token) {
      router.replace('/login')
    } else {
      setAuthorized(true)
    }
  }, [pathname, isPublic, router])

  if (!authorized) return null
  return <>{children}</>
}
