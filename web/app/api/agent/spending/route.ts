import { NextRequest, NextResponse } from 'next/server'

const AI_API_URL =
  process.env.NEXT_PUBLIC_AI_API_URL || 'http://localhost:8086'

export async function POST(req: NextRequest) {
  try {
    const customerId = req.headers.get('X-Customer-Id')
    if (!customerId) {
      return NextResponse.json({ error: '인증 정보가 없습니다.' }, { status: 401 })
    }

    const body = await req.json()

    const upstream = await fetch(`${AI_API_URL}/agent/spending/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...body, customer_id: customerId }),
      cache: 'no-store',
    })

    const data = await upstream.json()
    return NextResponse.json(data, { status: upstream.status })
  } catch {
    return NextResponse.json(
      { error: '지출 분석 서버와 통신할 수 없습니다.' },
      { status: 503 },
    )
  }
}
