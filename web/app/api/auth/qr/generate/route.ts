import { NextResponse } from 'next/server'

export async function POST() {
  // 생성 시각을 tokenHash에 인코딩 → 상태 조회 시 경과 시간으로 상태 결정
  const timestamp   = Date.now().toString(36)
  const random      = Math.random().toString(36).slice(2, 8)
  const tokenHash   = `${timestamp}-${random}`
  const confirmCode = String(Math.floor(1000 + Math.random() * 9000))
  const expiryAt    = new Date(Date.now() + 3 * 60 * 1000).toISOString()

  return NextResponse.json({ code: 'OK', message: 'OK', data: { tokenHash, confirmCode, expiryAt } })
}
