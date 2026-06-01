'use client'

import { useState } from 'react'

type Props = {
  productName: string
  onClose: () => void
}

export default function CartModal({ productName, onClose }: Props) {
  const [memo, setMemo] = useState('')

  function handleConfirm() {
    alert(`'${productName}'을(를) 장바구니에 담았습니다.`)
    onClose()
  }

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center bg-black/40">
      <div className="bg-white shadow-2xl" style={{ width: 420 }}>

        {/* 헤더 */}
        <div className="flex items-center justify-between px-5 py-3 bg-kb-yellow">
          <span className="text-[16px] font-bold text-kb-text">장바구니 담기</span>
          <span className="text-[13px] font-bold text-kb-text flex items-center gap-1">
            <svg viewBox="0 0 20 20" fill="none" className="w-4 h-4" stroke="currentColor" strokeWidth="2">
              <path d="M10 2L3 7v6c0 4 2.5 7 7 8 4.5-1 7-4 7-8V7L10 2z" fill="#1A1A1A" stroke="none"/>
            </svg>
            AX풀뱅크
          </span>
        </div>

        {/* 안내 */}
        <div className="px-5 pt-4 pb-2">
          <p className="text-[13px] text-kb-text-muted">관심, 상품 가입계획 등을 메모해 담으시면 관리합니다.</p>
        </div>

        {/* 폼 테이블 */}
        <div className="px-5 pb-3">
          <table className="w-full border-collapse text-[13px]">
            <tbody>
              <tr>
                <td className="bg-kb-beige-light border border-kb-border px-3 py-3 font-semibold text-kb-text w-20 whitespace-nowrap">
                  상품명
                </td>
                <td className="border border-kb-border px-3 py-3 text-kb-text-body">
                  {productName}
                </td>
              </tr>
              <tr>
                <td className="bg-kb-beige-light border border-kb-border px-3 py-3 font-semibold text-kb-text align-top">
                  메모
                </td>
                <td className="border border-kb-border px-3 py-2">
                  <textarea
                    value={memo}
                    onChange={(e) => setMemo(e.target.value)}
                    className="w-full h-24 text-[13px] resize-none outline-none leading-relaxed"
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* 확인/취소 버튼 */}
        <div className="flex justify-center gap-2 py-3">
          <button
            onClick={handleConfirm}
            className="bg-kb-yellow px-7 py-2 text-[13px] font-bold text-kb-text hover:bg-kb-yellow-dark transition-colors"
          >
            확인
          </button>
          <button
            onClick={onClose}
            className="border border-kb-border px-7 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light transition-colors"
          >
            취소
          </button>
        </div>

        {/* 닫기 */}
        <div className="flex justify-end px-5 pb-3">
          <button
            onClick={onClose}
            className="text-[12px] text-kb-text-muted hover:text-kb-text flex items-center gap-1"
          >
            닫기 <span className="text-[11px]">✕</span>
          </button>
        </div>
      </div>
    </div>
  )
}
