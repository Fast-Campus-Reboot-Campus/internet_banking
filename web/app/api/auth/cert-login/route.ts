import { NextRequest, NextResponse } from 'next/server'

const MOCK_CERTS = [
  { cert_id: 'cert_1', customer_id: 9001, pin: '123456', name: '홍길동', email: 'test@axful.com' },
]

export async function POST(req: NextRequest) {
  const { cert_id, pin } = await req.json()

  const cert = MOCK_CERTS.find((c) => c.cert_id === cert_id && c.pin === pin)

  if (!cert) {
    return NextResponse.json({ message: '비밀번호가 맞지 않습니다.' }, { status: 401 })
  }

  const access_token = `mock.${Buffer.from(
    JSON.stringify({ customer_id: cert.customer_id, name: cert.name, email: cert.email })
  ).toString('base64')}.${Date.now()}`

  const refresh_token = `mock_refresh_${cert.customer_id}_${Date.now()}`

  const res = NextResponse.json({
    access_token,
    expires_in: 1800,
    token_type: 'Bearer',
    user: {
      customer_id: cert.customer_id,
      name: cert.name,
      email: cert.email,
    },
  })

  res.cookies.set('refresh_token', refresh_token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: 60 * 60 * 24 * 7,
    path: '/',
  })

  return res
}
