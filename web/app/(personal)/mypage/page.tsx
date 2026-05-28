'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { api } from '@/lib/api'

interface MyPageData {
  customerId: number
  loginId: string
  name: string
  email: string | null
  phone: string | null
  zipCode: string | null
  address: string | null
  addressDetail: string | null
  birthDate: string
  genderCode: string
  customerGradeCode: string
  customerStatusCode: string
  joinedAt: string
  lastLoginAt: string | null
}

function maskId(loginId: string) {
  if (loginId.length <= 4) return loginId + '****'
  return loginId.slice(0, 4) + '****'
}

function maskPhone(phone: string | null) {
  if (!phone) return '-'
  const digits = phone.replace(/\D/g, '')
  if (digits.length === 11) {
    return `${digits.slice(0, 3)} ) ${digits.slice(3, 5)}** - ${digits.slice(7, 9)}**`
  }
  return phone
}

function maskEmail(email: string | null) {
  if (!email) return '-'
  const [local, domain] = email.split('@')
  if (!domain) return email
  return local.slice(0, 4) + '****@' + domain
}

function gradeLabel(code: string) {
  const map: Record<string, string> = {
    VIP: 'VIP', GOLD: '골드', SILVER: '실버', FAMILY: '패밀리', NORMAL: '일반',
  }
  return map[code] ?? code
}

export default function MyKBPage() {
  const router = useRouter()
  const [data, setData] = useState<MyPageData | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      router.replace('/login')
      return
    }
    api.get('/api/v1/customers/me')
      .then(res => setData(res.data.data))
      .catch(() => {
        setError('정보를 불러오지 못했습니다. 다시 로그인해주세요.')
        localStorage.removeItem('accessToken')
        localStorage.removeItem('customerId')
      })
  }, [router])

  if (error) {
    return (
      <div className="max-w-kb-container mx-auto px-6 py-20 text-center">
        <p className="text-kb-red mb-4">{error}</p>
        <Link href="/login" className="text-kb-blue hover:underline">로그인 페이지로 이동</Link>
      </div>
    )
  }

  if (!data) {
    return (
      <div className="max-w-kb-container mx-auto px-6 py-20 text-center text-kb-text-muted text-[14px]">
        불러오는 중...
      </div>
    )
  }

  const lastLoginDate = data.lastLoginAt ? new Date(data.lastLoginAt) : new Date(data.joinedAt)
  const joinedStr = `${lastLoginDate.getFullYear()}.${String(lastLoginDate.getMonth() + 1).padStart(2, '0')}.${String(lastLoginDate.getDate()).padStart(2, '0')} ${String(lastLoginDate.getHours()).padStart(2, '0')}:${String(lastLoginDate.getMinutes()).padStart(2, '0')}`

  return (
    <div className="max-w-kb-container mx-auto px-6 py-10 pb-16">
      {/* 브레드크럼 */}
      <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
        <span>개인뱅킹</span><span>&gt;</span>
        <span className="font-semibold text-kb-text">My AXful</span>
      </div>

      <h1 className="text-2xl font-bold text-kb-text mb-1">My AXful</h1>
      <p className="text-[12px] text-kb-text-muted mb-6">최종 접속시간 : {joinedStr}</p>

      {/* 인터넷뱅킹정보 */}
      <section className="mb-6">
        <div className="flex justify-between items-center mb-2">
          <h2 className="text-lg font-bold text-kb-text">인터넷뱅킹정보</h2>
          <button className="border border-kb-border px-4 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
            사용자 암호 재설정
          </button>
        </div>
        <table className="w-full border-collapse text-[13px]">
          <tbody>
            <tr>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[160px]">사용자 ID</td>
              <td className="border border-kb-border px-4 py-3 text-kb-text-body">{maskId(data.loginId)}</td>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[120px]">보안매체</td>
              <td className="border border-kb-border px-4 py-3 text-kb-text-body">보안카드</td>
            </tr>
            <tr>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text">보안수준</td>
              <td className="border border-kb-border px-4 py-3">
                <div className="flex items-center gap-3">
                  <span className="text-kb-text-body">요주의</span>
                  <button className="border border-kb-border px-3 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">보안수준</button>
                </div>
              </td>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text">이체한도</td>
              <td className="border border-kb-border px-4 py-3">
                <div className="flex items-center gap-3">
                  <div className="text-[12px] text-kb-text-body">
                    <p>(1일) 1,000,000</p>
                    <p>(1회) 1,000,000</p>
                  </div>
                  <button className="border border-kb-border px-3 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">이체한도금액</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      {/* 기본정보 */}
      <section className="mb-6">
        <div className="flex justify-between items-center mb-2">
          <h2 className="text-lg font-bold text-kb-text">기본정보</h2>
          <div className="flex gap-2">
            <button className="border border-kb-border px-4 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">본인정보 이용/제공 조회</button>
            <Link href="/settings" className="border border-kb-border px-4 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">정보수정</Link>
          </div>
        </div>
        <table className="w-full border-collapse text-[13px]">
          <tbody>
            <tr>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[160px]">성명</td>
              <td className="border border-kb-border px-4 py-3 text-kb-text-body w-[300px]">{data.name}</td>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[120px]">생년월일</td>
              <td className="border border-kb-border px-4 py-3 text-kb-text-body">
                {data.birthDate.slice(0, 4)}.{data.birthDate.slice(4, 6)}.{data.birthDate.slice(6, 8)}
              </td>
            </tr>
            <tr>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text">휴대폰번호</td>
              <td className="border border-kb-border px-4 py-3 text-kb-text-body">{maskPhone(data.phone)}</td>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text">자택 전화번호</td>
              <td className="border border-kb-border px-4 py-3 text-kb-text-muted">-</td>
            </tr>
            <tr>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text">자택주소</td>
              <td className="border border-kb-border px-4 py-3 text-kb-text-muted">
                {data.address ? `${data.address} ${data.addressDetail ?? ''}`.trim() : '-'}
              </td>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text">이메일</td>
              <td className="border border-kb-border px-4 py-3 text-kb-text-body">{maskEmail(data.email)}</td>
            </tr>
          </tbody>
        </table>
      </section>

      {/* 부가정보 */}
      <section className="mb-8">
        <h2 className="text-lg font-bold text-kb-text mb-4">부가정보</h2>
        <table className="w-full border-collapse text-[13px]">
          <tbody>
            <tr>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[160px]">스타클럽</td>
              <td className="border border-kb-border px-4 py-3 text-kb-text-body w-[300px]">{gradeLabel(data.customerGradeCode)}</td>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[120px]">포인트리</td>
              <td className="border border-kb-border px-4 py-3">
                <div className="flex items-center gap-3">
                  <span className="text-kb-text-muted text-[12px]">조회 후 확인 가능</span>
                  <button className="border border-kb-border px-3 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">포인트리 확인</button>
                </div>
              </td>
            </tr>
            <tr>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text">제휴쿠폰</td>
              <td className="border border-kb-border px-4 py-3">
                <button className="border border-kb-border px-3 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">내 쿠폰 확인</button>
              </td>
              <td className="border border-kb-border bg-kb-beige-light px-4 py-3 font-semibold text-kb-text">AXful금융쿠폰</td>
              <td className="border border-kb-border px-4 py-3">
                <button className="border border-kb-border px-3 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">내 쿠폰 확인</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      {/* 하단 카드 2개 */}
      <div className="grid grid-cols-2 gap-6">
        <Link href="#"
          className="border border-kb-border-dark rounded-xl p-6 flex items-center justify-between hover:bg-kb-beige-light transition-colors group">
          <div>
            <p className="text-base font-bold text-kb-text group-hover:text-kb-taupe">출금계좌 등록/변경 &gt;</p>
            <p className="text-sm text-kb-text-muted mt-1">출금할 때 자주 사용하는 계좌를 출금계좌로 등록하세요.</p>
          </div>
          <div className="w-10 h-10 flex items-center justify-center flex-shrink-0">
            <span className="text-2xl">📋</span>
          </div>
        </Link>
        <Link href="#"
          className="border border-kb-border-dark rounded-xl p-6 flex items-center justify-between hover:bg-kb-beige-light transition-colors group">
          <div>
            <p className="text-base font-bold text-kb-text group-hover:text-kb-taupe">자주쓰는 계좌 등록/변경 &gt;</p>
            <p className="text-sm text-kb-text-muted mt-1">입금할 때 자주 사용하는 계좌를 등록하세요.</p>
          </div>
          <div className="w-10 h-10 flex items-center justify-center flex-shrink-0">
            <span className="text-2xl">✅</span>
          </div>
        </Link>
      </div>
    </div>
  )
}
