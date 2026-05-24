'use client'

import { useEffect, useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'

// 로그인 없이 접근 가능한 경로 prefix
const PUBLIC_PREFIXES = [
  '/login',
<<<<<<< HEAD
  '/personal',
=======
>>>>>>> 8336117 (feat(web): add Next.js 프론트엔드 (AX풀뱅크 인터넷뱅킹 클론))
  '/banking',
  '/cert',
  '/cert-cps',
  '/cert-biz',
  '/products',
<<<<<<< HEAD
  '/support',
=======
>>>>>>> 8336117 (feat(web): add Next.js 프론트엔드 (AX풀뱅크 인터넷뱅킹 클론))
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
<<<<<<< HEAD
    const token = localStorage.getItem('accessToken')
=======
    const token = localStorage.getItem('access_token')
>>>>>>> 8336117 (feat(web): add Next.js 프론트엔드 (AX풀뱅크 인터넷뱅킹 클론))
    if (!token) {
      router.replace('/login')
    } else {
      setAuthorized(true)
    }
  }, [pathname, isPublic, router])

  if (!authorized) return null
  return <>{children}</>
}
