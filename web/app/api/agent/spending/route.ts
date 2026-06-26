import { NextRequest, NextResponse } from 'next/server'

const AI_API_URL =
  process.env.NEXT_PUBLIC_AI_API_URL || 'http://localhost:8086'

export async function POST(req: NextRequest) {
  try {
    const authorization = req.headers.get('Authorization')
    if (!authorization) {
      return NextResponse.json({ error: '인증 정보가 없습니다.' }, { status: 401 })
    }

    const body = await req.json()

    // Authorization 헤더를 upstream으로 그대로 전달 — customer_id 추출은 goal-agent에서 처리
    const upstream = await fetch(`${AI_API_URL}/agent/spending/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authorization,
      },
      body: JSON.stringify(body),
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
