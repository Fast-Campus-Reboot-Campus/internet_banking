import { NextRequest, NextResponse } from 'next/server'

// tokenHash = "{timestamp(base36)}-{random}"
// 경과 시간으로 상태 결정 (서버 상태 저장 불필요)
//   0 ~  6s : PENDING
//   6 ~ 12s : SCANNED
//  12s+     : APPROVED (mock 인증서 정보 반환)
// 180s+     : EXPIRED

function fmt(d: Date) {
  return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`
}

export async function GET(req: NextRequest) {
  const token = req.nextUrl.searchParams.get('token') ?? ''
  const [tsPart] = token.split('-')
  const createdAt = parseInt(tsPart, 36)

  if (!createdAt || isNaN(createdAt)) {
    return NextResponse.json({ code: 'CUST_040', message: 'QR 토큰을 찾을 수 없습니다.', data: null }, { status: 404 })
  }

  const elapsed = Date.now() - createdAt

  if (elapsed >= 180_000) {
    return NextResponse.json({ code: 'OK', message: 'OK', data: { status: 'EXPIRED', serialNumber: null, issuedDate: null, expiryDate: null } })
  }

  if (elapsed >= 12_000) {
    const today  = new Date()
    const expiry = new Date(today)
    expiry.setFullYear(expiry.getFullYear() + 3)
    const serial = `AXFUL-MOCK-${token.split('-')[1]?.toUpperCase() ?? 'DEMO'}`
    return NextResponse.json({ code: 'OK', message: 'OK', data: {
      status:       'APPROVED',
      serialNumber: serial,
      issuedDate:   fmt(today),
      expiryDate:   fmt(expiry),
    } })
  }

  if (elapsed >= 6_000) {
    return NextResponse.json({ code: 'OK', message: 'OK', data: { status: 'SCANNED', serialNumber: null, issuedDate: null, expiryDate: null } })
  }

  return NextResponse.json({ code: 'OK', message: 'OK', data: { status: 'PENDING', serialNumber: null, issuedDate: null, expiryDate: null } })
}
