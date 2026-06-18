// 이체 라우팅 판정 — 받는 은행 이름으로 당행(INTERNAL)/타행(EXTERNAL)과 은행코드를 결정한다.
// page 컴포넌트에 인라인돼 있던 로직을 회귀 방지를 위해 순수 함수로 추출했다.

export interface BankOption {
  code: string
  name: string
}

export type TransferRoutingResult =
  | { ok: true; transferType: 'INTERNAL' | 'EXTERNAL'; toBankCode: string }
  | { ok: false; reason: 'UNSUPPORTED_BANK' }

/** 당행(AXful) 이름·코드. 본행 계좌는 타인 계좌라도 INTERNAL 로 라우팅된다. */
export const HOME_BANK_NAME = 'AXful'
export const HOME_BANK_CODE = 'KB'

/**
 * 받는 은행 이름으로 이체 라우팅과 은행코드를 판정한다.
 *
 * - 당행(AXful)이면 타인 계좌라도 INTERNAL.
 * - 매핑된 그 외 은행은 EXTERNAL + 해당 코드.
 * - 매핑되지 않은 은행명(최근이체 등 외부 출처)은 INTERNAL 로 폴백하지 않고 차단한다.
 *   외부 이체가 내부로 오분류돼 타행 자금이 잘못 라우팅되는 것을 방지하기 위함.
 */
export function resolveTransferRouting(toBank: string, banks: BankOption[]): TransferRoutingResult {
  const isOwnBank = toBank === HOME_BANK_NAME
  const matchedBank = banks.find(b => b.name === toBank)
  if (!isOwnBank && !matchedBank) {
    return { ok: false, reason: 'UNSUPPORTED_BANK' }
  }
  return {
    ok: true,
    transferType: isOwnBank ? 'INTERNAL' : 'EXTERNAL',
    toBankCode: isOwnBank ? HOME_BANK_CODE : matchedBank!.code,
  }
}
