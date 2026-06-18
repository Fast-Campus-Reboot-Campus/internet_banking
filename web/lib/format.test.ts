import { describe, it, expect } from 'vitest'
import { maskPhone } from '@/lib/format'

describe('maskPhone — 고객 휴대폰 PII 마스킹', () => {
  it('대시 포함 휴대폰 가운데를 가린다', () => {
    expect(maskPhone('010-1015-2015')).toBe('010-****-2015')
  })
  it('대시 없는 11자리도 마스킹 (가운데 제거)', () => {
    expect(maskPhone('01012345678')).toBe('010-****-5678')
  })
  it('지역번호 2자리(서울 02)도 처리', () => {
    expect(maskPhone('02-123-4567')).toBe('02-****-4567')
  })
  it('null/빈 문자열은 - 로 표시', () => {
    expect(maskPhone(null)).toBe('-')
    expect(maskPhone('')).toBe('-')
  })
  it('휴대폰 형식이 아니면 원문 유지', () => {
    expect(maskPhone('정보없음')).toBe('정보없음')
  })
})
