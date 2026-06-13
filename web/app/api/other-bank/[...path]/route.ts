import { NextRequest, NextResponse } from 'next/server'

// 다온은행(payment-service-b) 호출용 BFF 프록시.
// 브라우저는 same-origin /api/other-bank 로만 요청하고, 실제 백엔드 주소는
// 서버 전용 env PAYMENT_B_API_URL 로 관리한다(배포 HTTPS 환경의 Mixed Content 회피).
const PAYMENT_B_API_URL = process.env.PAYMENT_B_API_URL || 'http://localhost:8180'

type RouteContext = { params: { path: string[] } }

async function proxy(request: NextRequest, context: RouteContext) {
  const path = context.params.path.join('/')
  const search = request.nextUrl.search
  const targetUrl = `${PAYMENT_B_API_URL}/${path}${search}`
  const body = request.method === 'GET' || request.method === 'HEAD' ? undefined : await request.text()

  try {
    const response = await fetch(targetUrl, {
      method: request.method,
      headers: {
        'Content-Type': request.headers.get('content-type') || 'application/json',
      },
      body,
      cache: 'no-store',
    })
    const responseBody = await response.text()
    return new NextResponse(responseBody, {
      status: response.status,
      headers: { 'Content-Type': response.headers.get('content-type') || 'application/json' },
    })
  } catch {
    return NextResponse.json({ detail: '다온은행 서비스에 연결할 수 없습니다.' }, { status: 502 })
  }
}

export async function GET(request: NextRequest, context: RouteContext) { return proxy(request, context) }
export async function POST(request: NextRequest, context: RouteContext) { return proxy(request, context) }
