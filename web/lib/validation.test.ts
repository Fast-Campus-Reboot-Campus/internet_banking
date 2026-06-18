import { describe, it, expect } from 'vitest'
import { validateUserPassword, isValidLoginId } from '@/lib/validation'

describe('validateUserPassword — 영문·숫자·특수 8~12자리', () => {
  it('정책 충족 시 ok', () => {
    expect(validateUserPassword('abcd12!@')).toEqual({ ok: true })
  })
  it('8자 미만은 LENGTH', () => {
    expect(validateUserPassword('ab1!')).toEqual({ ok: false, reason: 'LENGTH' })
  })
  it('12자 초과는 LENGTH', () => {
    expect(validateUserPassword('abcdef123456!@')).toEqual({ ok: false, reason: 'LENGTH' })
  })
  it('숫자만은 조합 미충족 COMPOSITION', () => {
    expect(validateUserPassword('12345678')).toEqual({ ok: false, reason: 'COMPOSITION' })
  })
  it('특수문자 없으면 COMPOSITION', () => {
    expect(validateUserPassword('abcd1234')).toEqual({ ok: false, reason: 'COMPOSITION' })
  })
})

describe('isValidLoginId — 영문/숫자 6~12자리, 영문 1자 이상', () => {
  it('영문+숫자 조합 통과', () => {
    expect(isValidLoginId('user01')).toBe(true)
  })
  it('숫자만은 거부 (영문 필수)', () => {
    expect(isValidLoginId('123456')).toBe(false)
  })
  it('6자 미만 거부', () => {
    expect(isValidLoginId('usr1')).toBe(false)
  })
  it('12자 초과 거부', () => {
    expect(isValidLoginId('user123456789')).toBe(false)
  })
  it('특수문자 포함 거부', () => {
    expect(isValidLoginId('user_01')).toBe(false)
  })
})
