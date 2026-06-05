import { NextRequest, NextResponse } from 'next/server'

const PAYMENT_API_URL = process.env.PAYMENT_API_URL || 'http://localhost:8080'

type RouteContext = { params: { path: string[] } }

async function proxy(request: NextRequest, context: RouteContext) {
  const path = context.params.path.join('/')
  const search = request.nextUrl.search
  const targetUrl = `${PAYMENT_API_URL}/${path}${search}`
  const body = request.method === 'GET' || request.method === 'HEAD' ? undefined : await request.text()

  try {
    const response = await fetch(targetUrl, {
      method: request.method,
      headers: {
        'Content-Type': request.headers.get('content-type') || 'application/json',
        ...(request.headers.get('X-User-Id') ? { 'X-User-Id': request.headers.get('X-User-Id')! } : {}),
        ...(request.headers.get('X-Auth-Token-Id') ? { 'X-Auth-Token-Id': request.headers.get('X-Auth-Token-Id')! } : {}),
        ...(request.headers.get('X-Idempotency-Key') ? { 'X-Idempotency-Key': request.headers.get('X-Idempotency-Key')! } : {}),
        ...(request.headers.get('X-Channel')    ? { 'X-Channel':    request.headers.get('X-Channel')! }    : {}),
        ...(request.headers.get('X-Request-Id') ? { 'X-Request-Id': request.headers.get('X-Request-Id')! } : {}),
        ...(request.headers.get('authorization') ? { 'Authorization': request.headers.get('authorization')! } : {}),
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
    return NextResponse.json({ detail: '결제 서비스에 연결할 수 없습니다.' }, { status: 502 })
  }
}

export async function GET(request: NextRequest, context: RouteContext) { return proxy(request, context) }
export async function POST(request: NextRequest, context: RouteContext) { return proxy(request, context) }
export async function PATCH(request: NextRequest, context: RouteContext) { return proxy(request, context) }
