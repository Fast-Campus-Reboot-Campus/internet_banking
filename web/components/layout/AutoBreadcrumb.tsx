'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { ReactNode } from 'react'
import { GNB_MENUS } from './Header'

// ============================================================
// 경로 기반 브레드크럼 생성기
// GNB_MENUS(드롭다운 컬럼 정의)를 단일 출처로 삼아
// "개인뱅킹 > 대분류 > 중분류 > 소분류" trail을 도출한다.
// 페이지마다 trail을 하드코딩하지 않으므로 드롭다운과 어긋날 일이 없다.
// ============================================================

export type Crumb = { label: string; href?: string }

const ROOT: Crumb = { label: '개인뱅킹', href: '/' }

function segs(p: string): string[] {
  return p.split('/').filter(Boolean)
}

function startsWith(path: string[], prefix: string[]): boolean {
  return prefix.every((s, i) => path[i] === s)
}

// 여러 경로의 공통 세그먼트 접두 (= 해당 그룹의 라우트 스코프)
function commonPrefix(paths: string[][]): string[] {
  if (paths.length === 0) return []
  const out: string[] = []
  for (let i = 0; i < paths[0].length; i++) {
    const s = paths[0][i]
    if (paths.every((p) => p[i] === s)) out.push(s)
    else break
  }
  return out
}

/**
 * pathname을 GNB_MENUS에 대조해 상위 trail을 도출한다.
 * - 대분류: 메뉴 항목 href들의 공통 접두(스코프)가 경로의 접두인 메뉴 중 가장 깊은 것
 * - 중분류: 카테고리 항목 href들의 공통 접두가 경로의 접두인 카테고리
 * - 소분류: 카테고리 스코프 다음 세그먼트까지 일치하는 항목([slug] 라우트도 폴더 단위로 매칭)
 */
export function resolveTrail(pathname: string): Crumb[] {
  const P = segs(pathname)
  const trail: Crumb[] = [ROOT]

  // 1) 대분류(menu)
  let bestMenu: (typeof GNB_MENUS)[number] | null = null
  let bestMenuScopeLen = -1
  for (const menu of GNB_MENUS) {
    const scope = commonPrefix(menu.megaMenu.flatMap((c) => c.items.map((i) => segs(i.href))))
    if (startsWith(P, scope) && scope.length > bestMenuScopeLen) {
      bestMenu = menu
      bestMenuScopeLen = scope.length
    }
  }
  if (!bestMenu) return trail
  trail.push({ label: bestMenu.label, href: bestMenu.href })

  // 2) 중분류(category)
  let bestCat: (typeof bestMenu.megaMenu)[number] | null = null
  let bestCatScope: string[] = []
  for (const cat of bestMenu.megaMenu) {
    const scope = commonPrefix(cat.items.map((i) => segs(i.href)))
    if (startsWith(P, scope) && scope.length > bestCatScope.length) {
      bestCat = cat
      bestCatScope = scope
    }
  }
  if (!bestCat) return trail
  trail.push({ label: bestCat.title, href: bestCat.items[0]?.href ?? bestCat.href })

  // 3) 소분류(item)
  for (const item of bestCat.items) {
    const itemKey = segs(item.href).slice(0, bestCatScope.length + 1)
    if (startsWith(P, itemKey)) {
      trail.push({ label: item.label, href: item.href })
      break
    }
  }
  return trail
}

interface AutoBreadcrumbProps {
  /** 현재 URL 대신 이 경로로 trail을 도출 (드롭다운에 없는 보조 페이지가 상위 항목에 붙고 싶을 때) */
  as?: string
  /** trail 끝에 현재 페이지명으로 덧붙일 항목 (드롭다운에 없는 하위 단계) */
  leaf?: string | Crumb
  /** trail 뒤에 구분자와 함께 붙는 부가 노드 (예: 도움말 링크) */
  trailing?: ReactNode
  align?: 'start' | 'end'
  className?: string
}

export default function AutoBreadcrumb({ as, leaf, trailing, align = 'start', className }: AutoBreadcrumbProps) {
  const pathname = usePathname()
  const base = resolveTrail(as ?? pathname)
  const items: Crumb[] = leaf
    ? [...base, typeof leaf === 'string' ? { label: leaf } : leaf]
    : base

  return (
    <nav
      className={
        className ??
        `text-[12px] text-kb-text-muted mb-4 flex items-center gap-1${align === 'end' ? ' justify-end' : ''}`
      }
    >
      {items.map((c, i) => {
        const last = i === items.length - 1
        return (
          <span key={i} className="flex items-center gap-1">
            {i > 0 && <span>›</span>}
            {!last && c.href ? (
              <Link href={c.href} className="hover:underline">
                {c.label}
              </Link>
            ) : (
              <span className={last ? 'text-kb-text font-medium' : undefined}>{c.label}</span>
            )}
          </span>
        )
      })}
      {trailing && (
        <span className="flex items-center gap-1">
          <span>›</span>
          {trailing}
        </span>
      )}
    </nav>
  )
}
