import { NextResponse } from 'next/server'

export async function POST() {
  return NextResponse.json({ code: 'OK', message: 'OK', data: null })
}
