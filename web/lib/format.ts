// 표시용 포매팅 헬퍼.

/**
 * 직원이 고객 휴대폰번호를 조회할 때의 PII 마스킹.
 * 가운데 자리를 ****로 가린다 (예: 010-1015-2015 → 010-****-2015).
 * 대시 유무와 무관하게 매칭하며, 휴대폰 형식이 아니면 원문을 그대로 반환한다.
 * null/빈 문자열은 '-' 로 표시한다.
 */
export function maskPhone(phone: string | null): string {
  if (!phone) return '-'
  return phone.replace(/^(\d{2,3})-?\d{3,4}-?(\d{4})$/, '$1-****-$2')
}

/**
 * 직원이 고객 이메일을 조회할 때의 마스킹.
 * 첫 글자만 남기고 도메인(@ 이후)은 유지한다 (예: hong@axful.com → h****@axful.com).
 * 이메일 형식이 아니면 원문을 그대로 반환하고, null/빈 문자열은 '-' 로 표시한다.
 */
export function maskEmail(email: string | null): string {
  if (!email) return '-'
  return email.replace(/^(.).*(@.*)$/, '$1****$2')
}
