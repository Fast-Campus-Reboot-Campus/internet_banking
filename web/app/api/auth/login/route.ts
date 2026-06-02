import { NextRequest, NextResponse } from 'next/server'

const MOCK_USERS = [
  { customer_id: 1, email: 'test@axfulbank.com', password: 'password123', name: '홍길동' },
  { customer_id: 2, email: 'test2', password: '12341234', name: '홍길동' },
]

export async function POST(req: NextRequest) {
  const { email, password } = await req.json()

  const user = MOCK_USERS.find((u) => u.email === email && u.password === password)

  if (!user) {
    return NextResponse.json(
      { message: '이메일 또는 비밀번호가 올바르지 않습니다.' },
      { status: 401 }
    )
  }

  const access_token = `mock.${Buffer.from(JSON.stringify({ customer_id: user.customer_id, name: user.name, email: user.email })).toString('base64')}.${Date.now()}`
  const refresh_token = `mock_refresh_${user.customer_id}_${Date.now()}`

  const res = NextResponse.json({
    access_token,
    expires_in: 1800,
    token_type: 'Bearer',
    user: {
      customer_id: user.customer_id,
      name: user.name,
      email: user.email,
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
