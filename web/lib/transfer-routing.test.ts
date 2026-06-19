import { describe, it, expect } from 'vitest'
import { resolveTransferRouting, type BankOption } from '@/lib/transfer-routing'
import { MOCK_BANKS } from '@/lib/mock-data'

const banks: BankOption[] = [
  { code: 'KB', name: 'AXful' },
  { code: 'SHB', name: '신한' },
]

describe('resolveTransferRouting — 이체 라우팅 판정', () => {
  it('당행(AXful)은 INTERNAL + 본행코드(KB)로 라우팅 (타인 계좌라도)', () => {
    const r = resolveTransferRouting('AXful', banks)
    expect(r).toEqual({ ok: true, transferType: 'INTERNAL', toBankCode: 'KB' })
  })

  it('매핑된 타행은 EXTERNAL + 해당 은행코드', () => {
    const r = resolveTransferRouting('신한', banks)
    expect(r).toEqual({ ok: true, transferType: 'EXTERNAL', toBankCode: 'SHB' })
  })

  it('매핑되지 않은 은행명은 INTERNAL 폴백 없이 차단한다 (타행 자금 오라우팅 방어)', () => {
    const r = resolveTransferRouting('존재하지않는은행', banks)
    expect(r).toEqual({ ok: false, reason: 'UNSUPPORTED_BANK' })
  })

  it('빈 은행명도 당행이 아니고 미매핑이므로 차단', () => {
    expect(resolveTransferRouting('', banks)).toEqual({ ok: false, reason: 'UNSUPPORTED_BANK' })
  })

  it('실제 MOCK_BANKS 기준: AXful→INTERNAL/KB, 신한→EXTERNAL/SHB', () => {
    expect(resolveTransferRouting('AXful', MOCK_BANKS)).toMatchObject({ transferType: 'INTERNAL', toBankCode: 'KB' })
    expect(resolveTransferRouting('신한', MOCK_BANKS)).toMatchObject({ transferType: 'EXTERNAL', toBankCode: 'SHB' })
  })
})
