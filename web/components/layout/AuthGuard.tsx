'use client'

import { useEffect, useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'

// 로그인 없이 접근 가능한 경로 prefix
const PUBLIC_PREFIXES = [
  '/login',
  '/logout',
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
    // accessToken(ID 로그인) 또는 access_token(인증서 로그인) 둘 다 허용
    const token = localStorage.getItem('accessToken') || localStorage.getItem('access_token')
    if (!token) {
      router.replace('/login')
    } else {
      // 두 키를 통일 — 이후 api.ts 인터셉터가 accessToken만 읽으므로 보정
      if (!localStorage.getItem('accessToken') && localStorage.getItem('access_token')) {
        localStorage.setItem('accessToken', localStorage.getItem('access_token')!)
      }
      setAuthorized(true)
    }
  }, [pathname, isPublic, router])

  if (!authorized) return null
  return <>{children}</>
}
