import { NextRequest, NextResponse } from 'next/server'

// tokenHash = "{timestamp(base36)}-{random}"
// 경과 시간으로 상태를 결정한다 (서버 상태 저장 불필요)
//   0 ~ 6s  : PENDING
//   6 ~ 10s : SCANNED
//   10s+    : APPROVED (mock 토큰 반환)
//   180s+   : EXPIRED

const MOCK_CUSTOMER_ID  = 1
const MOCK_ACCESS_TOKEN = 'mock.eyJjdXN0b21lcklkIjoxLCJuYW1lIjoi7ZmN6ri464ukIn0.mock'
const MOCK_REFRESH_TOKEN = 'mock_refresh_1'

export async function GET(req: NextRequest) {
  const token = req.nextUrl.searchParams.get('token') ?? ''
  const [tsPart] = token.split('-')
  const createdAt = parseInt(tsPart, 36)

  if (!createdAt || isNaN(createdAt)) {
    return NextResponse.json({ code: 'CUST_040', message: 'QR 토큰을 찾을 수 없습니다.', data: null }, { status: 404 })
  }

  const elapsed = Date.now() - createdAt

  if (elapsed >= 180_000) {
    return NextResponse.json({ code: 'OK', message: 'OK', data: { status: 'EXPIRED', customerId: null, accessToken: null, refreshToken: null } })
  }
  if (elapsed >= 10_000) {
    return NextResponse.json({ code: 'OK', message: 'OK', data: {
      status: 'APPROVED',
      customerId: MOCK_CUSTOMER_ID,
      accessToken: MOCK_ACCESS_TOKEN,
      refreshToken: MOCK_REFRESH_TOKEN,
    }})
  }
  if (elapsed >= 6_000) {
    return NextResponse.json({ code: 'OK', message: 'OK', data: { status: 'SCANNED', customerId: null, accessToken: null, refreshToken: null } })
  }

  return NextResponse.json({ code: 'OK', message: 'OK', data: { status: 'PENDING', customerId: null, accessToken: null, refreshToken: null } })
}
