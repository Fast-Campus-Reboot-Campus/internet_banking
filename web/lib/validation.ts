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
