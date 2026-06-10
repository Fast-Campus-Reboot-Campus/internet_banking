import { NextRequest, NextResponse } from 'next/server'

const CUSTOMER_API_URL =
  process.env.CUSTOMER_API_URL ||
  process.env.NEXT_PUBLIC_API_URL ||
  'http://localhost:8081'

export async function POST(req: NextRequest) {
  try {
    const body = await req.text()

    const upstream = await fetch(`${CUSTOMER_API_URL}/api/v1/auth/cert-login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
      cache: 'no-store',
    })

    const text = await upstream.text()
    return new NextResponse(text, {
      status: upstream.status,
      headers: { 'Content-Type': upstream.headers.get('Content-Type') || 'application/json' },
    })
  } catch {
    return NextResponse.json(
      { code: 'CUSTOMER_SERVICE_UNAVAILABLE', message: '인증 서버와 통신할 수 없습니다.' },
      { status: 503 },
    )
  }
}
