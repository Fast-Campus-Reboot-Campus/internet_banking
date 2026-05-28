import { NextRequest, NextResponse } from 'next/server'

const CONSULTATION_API_URL = process.env.CONSULTATION_API_URL || 'http://localhost:8087'

type RouteContext = {
  params: {
    path: string[]
  }
}

async function proxy(request: NextRequest, context: RouteContext) {
  const path = context.params.path.join('/')
  const search = request.nextUrl.search
  const targetUrl = `${CONSULTATION_API_URL}/${path}${search}`
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
      headers: {
        'Content-Type': response.headers.get('content-type') || 'application/json',
      },
    })
  } catch {
    return NextResponse.json(
      { detail: '상담 서비스에 연결할 수 없습니다.' },
      { status: 502 },
    )
  }
}

export async function GET(request: NextRequest, context: RouteContext) {
  return proxy(request, context)
}

export async function POST(request: NextRequest, context: RouteContext) {
  return proxy(request, context)
}
