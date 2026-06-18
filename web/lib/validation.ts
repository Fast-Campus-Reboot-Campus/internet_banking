// 입력 형식 검증 헬퍼 (순수 함수).
// 컴포넌트의 setError/alert 등 부수효과는 호출측에 두고, 여기서는 판정만 한다.

/** 사용자암호 정책(#48): 영문·숫자·특수문자 조합 8~12자리. */
export type PasswordPolicyResult = { ok: true } | { ok: false; reason: 'LENGTH' | 'COMPOSITION' }

export function validateUserPassword(pw: string): PasswordPolicyResult {
  if (pw.length < 8 || pw.length > 12) return { ok: false, reason: 'LENGTH' }
  const hasLetter = /[A-Za-z]/.test(pw)
  const hasDigit = /[0-9]/.test(pw)
  const hasSpecial = /[^A-Za-z0-9]/.test(pw)
  if (!(hasLetter && hasDigit && hasSpecial)) return { ok: false, reason: 'COMPOSITION' }
  return { ok: true }
}

/** 인터넷뱅킹 ID 형식: 특수문자 제외 영문/숫자 6~12자리, 영문 1자 이상. */
export function isValidLoginId(id: string): boolean {
  return /^[A-Za-z0-9]{6,12}$/.test(id) && /[A-Za-z]/.test(id)
}

/** 법인 회원가입 1단계 입력. */
export interface CorporateJoinInput {
  corpName: string
  corpEnglishName: string
  corpRegNo: string
  bizRegNo: string
  tradeName: string
  openingDate: string
  ntsIndustryCode: string
  ksicCode: string
  bizItemCode: string
}

/**
 * 법인정보 입력 검증. 첫 번째로 걸리는 오류 메시지를 반환하고, 모두 통과하면 null.
 * (필드 순서·메시지는 화면 동작과 1:1로 유지한다)
 */
export function validateCorporateJoin(f: CorporateJoinInput): string | null {
  if (!f.corpName) return '법인명을 입력해주세요.'
  if (!/^[A-Za-z0-9 .,&()'\-]+$/.test(f.corpEnglishName)) return '영문 법인명을 영문으로 입력해주세요.'
  if (!/^\d{6}-\d{7}$/.test(f.corpRegNo)) return '법인등록번호를 형식(123456-1234567)에 맞게 입력해주세요.'
  if (!/^\d{3}-\d{2}-\d{5}$/.test(f.bizRegNo)) return '사업자등록번호를 형식(123-45-67890)에 맞게 입력해주세요.'
  if (!f.tradeName) return '상호명을 입력해주세요.'
  if (!/^\d{8}$/.test(f.openingDate) ||
      isNaN(Date.parse(`${f.openingDate.slice(0, 4)}-${f.openingDate.slice(4, 6)}-${f.openingDate.slice(6, 8)}`)))
    return '개업일자를 정확히 입력해주세요. (YYYYMMDD)'
  if (!/^\d{6}$/.test(f.ntsIndustryCode)) return '국세청 업종코드 6자리를 입력해주세요.'
  if (!/^\d{5}$/.test(f.ksicCode)) return '표준산업분류(KSIC) 5자리 숫자를 입력해주세요.'
  if (!f.bizItemCode) return '업태/종목을 입력해주세요.'
  return null
}
