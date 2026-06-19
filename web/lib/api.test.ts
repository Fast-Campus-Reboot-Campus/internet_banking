import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import type { AxiosError } from 'axios'
import { isAuthPath, isSilentPath, onResponseError } from '@/lib/api'

function err401(url: string, retry = false): AxiosError {
  return {
    config: { url, _retry: retry, headers: {} },
    response: { status: 401 },
    isAxiosError: true,
    name: 'AxiosError',
    message: '401',
  } as unknown as AxiosError
}

describe('isAuthPath / isSilentPath — 경로 분류', () => {
  it('cert/issue 는 인증경로 (갱신·리다이렉트 제외 — #54 회귀)', () => {
    expect(isAuthPath('/api/v1/auth/cert/issue')).toBe(true)
  })
  it('일반 API 는 인증경로 아님', () => {
    expect(isAuthPath('/api/v1/customers/1')).toBe(false)
  })
  it('/customers/me 는 silent, /auth/login 은 아님', () => {
    expect(isSilentPath('/api/v1/customers/me')).toBe(true)
    expect(isSilentPath('/api/v1/auth/login')).toBe(false)
  })
})

describe('onResponseError — 401 처리 분기', () => {
  const realLocation = window.location

  beforeEach(() => {
    localStorage.clear()
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: { href: '' },
    })
  })
  afterEach(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: realLocation,
    })
  })

  it('401 이 아니면 그대로 reject, 리다이렉트 없음', async () => {
    const e = { config: { url: '/x' }, response: { status: 500 } } as unknown as AxiosError
    await expect(onResponseError(e)).rejects.toBe(e)
    expect(window.location.href).toBe('')
  })

  it('인증경로(/auth/login) 401 — reject만, 세션 유지·리다이렉트 없음', async () => {
    localStorage.setItem('accessToken', 'tok')
    const e = err401('/api/v1/auth/login')
    await expect(onResponseError(e)).rejects.toBe(e)
    expect(window.location.href).toBe('')
    expect(localStorage.getItem('accessToken')).toBe('tok')
  })

  it('/auth/refresh 401 — 세션 종료 + /login 리다이렉트', async () => {
    localStorage.setItem('accessToken', 'tok')
    const e = err401('/api/v1/auth/refresh')
    await expect(onResponseError(e)).rejects.toBe(e)
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(window.location.href).toBe('/login')
  })

  it('일반 API 401 + refresh 토큰 없음 — 갱신 실패로 세션 종료 + 리다이렉트', async () => {
    localStorage.setItem('accessToken', 'tok') // refreshToken 없음 → 갱신 null
    const e = err401('/api/v1/customers/1')
    await expect(onResponseError(e)).rejects.toBe(e)
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(window.location.href).toBe('/login')
  })

  it('silent 경로(/customers/me) 401 — 세션 종료하되 리다이렉트 안 함', async () => {
    localStorage.setItem('accessToken', 'tok')
    const e = err401('/api/v1/customers/me')
    await expect(onResponseError(e)).rejects.toBe(e)
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(window.location.href).toBe('')
  })

  it('이미 재시도(_retry)한 요청 401 — 추가 갱신 없이 세션 종료 + 리다이렉트', async () => {
    localStorage.setItem('refreshToken', 'r') // 있어도 _retry 면 갱신 안 함
    const e = err401('/api/v1/customers/1', true)
    await expect(onResponseError(e)).rejects.toBe(e)
    expect(window.location.href).toBe('/login')
  })
})
