/**
 * 브랜드 색상 상수 (인라인 style 전용).
 *
 * - className 에서는 Tailwind 토큰을 사용: `bg-kb-primary`, `text-kb-primary`, ...
 * - style={{}} 인라인에서는 아래 상수를 import 해서 사용: `style={{ color: KB_PRIMARY }}`
 *
 * ※ 값은 tailwind.config.ts 의 동명 토큰과 항상 일치시켜야 한다.
 */
export const KB_PRIMARY         = '#0D5C47' // 메인 브랜드 그린
export const KB_PRIMARY_BG      = '#F0FAF7' // 옅은 그린 배경
export const KB_PRIMARY_BORDER  = '#E2F5EF' // 그린 보더·구분선
export const KB_PRIMARY_SURFACE = '#F8FFFE' // 초옅은 배경
export const KB_PRIMARY_DARK    = '#3D4F47' // 진한 그린(헤더 등)

export const KB_MINT       = '#5BC9A8' // 액센트 민트
export const KB_MINT_DARK  = '#3FA889'
export const KB_MINT_LIGHT = '#A8E5D4'
