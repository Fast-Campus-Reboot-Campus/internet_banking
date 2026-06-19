import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { cn } from '@/lib/utils'

// 하네스 스모크 테스트: 변환 파이프라인(TS·JSX)·경로 별칭(@/)·jsdom·jest-dom 매처가
// 모두 동작하는지 검증한다. 프로덕션 컴포넌트는 건드리지 않는다.
describe('cn() — tailwind 클래스 병합', () => {
  it('충돌하는 클래스는 뒤가 이긴다', () => {
    expect(cn('px-2', 'px-4')).toBe('px-4')
  })

  it('falsy 값은 무시한다', () => {
    expect(cn('a', false, undefined, 'c')).toBe('a c')
  })
})

describe('하네스 스모크 — JSX·jsdom·jest-dom', () => {
  it('JSX를 jsdom에 렌더하고 DOM 매처로 단언한다', () => {
    render(<button type="button">확인</button>)
    expect(screen.getByRole('button', { name: '확인' })).toBeInTheDocument()
  })
})
