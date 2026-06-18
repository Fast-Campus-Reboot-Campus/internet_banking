import { describe, it, expect } from 'vitest'
import { validateUserPassword, isValidLoginId, validateCorporateJoin, type CorporateJoinInput } from '@/lib/validation'

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

describe('validateCorporateJoin — 법인정보 입력 검증', () => {
  const valid: CorporateJoinInput = {
    corpName: '테스트법인',
    corpEnglishName: 'Test Corp',
    corpRegNo: '123456-1234567',
    bizRegNo: '123-45-67890',
    tradeName: '테스트상호',
    openingDate: '20200101',
    ntsIndustryCode: '123456',
    ksicCode: '64191',
    bizItemCode: '소프트웨어 개발',
  }

  it('모든 필드 정상이면 null', () => {
    expect(validateCorporateJoin(valid)).toBeNull()
  })
  it('영문 법인명에 한글이면 거부', () => {
    expect(validateCorporateJoin({ ...valid, corpEnglishName: '테스트' }))
      .toBe('영문 법인명을 영문으로 입력해주세요.')
  })
  it('법인등록번호 형식 불일치', () => {
    expect(validateCorporateJoin({ ...valid, corpRegNo: '1234561234567' }))
      .toBe('법인등록번호를 형식(123456-1234567)에 맞게 입력해주세요.')
  })
  it('개업일자 — 8자리지만 존재하지 않는 날짜(13월)는 거부', () => {
    expect(validateCorporateJoin({ ...valid, openingDate: '20201301' }))
      .toBe('개업일자를 정확히 입력해주세요. (YYYYMMDD)')
  })
  it('국세청 업종코드 6자리 아니면 거부', () => {
    expect(validateCorporateJoin({ ...valid, ntsIndustryCode: '12345' }))
      .toBe('국세청 업종코드 6자리를 입력해주세요.')
  })
  it('KSIC 5자리 아니면 거부', () => {
    expect(validateCorporateJoin({ ...valid, ksicCode: '6419' }))
      .toBe('표준산업분류(KSIC) 5자리 숫자를 입력해주세요.')
  })
  it('첫 번째 오류부터 보고 (순서 보존: corpName 먼저)', () => {
    expect(validateCorporateJoin({ ...valid, corpName: '', corpEnglishName: '한글' }))
      .toBe('법인명을 입력해주세요.')
  })
})
