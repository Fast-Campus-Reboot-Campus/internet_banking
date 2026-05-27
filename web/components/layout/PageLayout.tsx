'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { GNB_MENUS } from './Header'
import { ReactNode } from 'react'

interface SidebarProps {
  category?: string
  categoryLabel?: string
}

export function Sidebar({ category, categoryLabel }: SidebarProps) {
  const pathname = usePathname()
  const menu = GNB_MENUS.find((m) => m.id === category)

  if (!menu) return null

  // megaMenu 카테고리 타이틀을 사이드바 항목으로 사용
  const sidebarItems = menu.megaMenu.map((cat) => ({
    label: cat.title,
    href: cat.href,
  }))

  return (
    <aside className="sidemenu">
      <div className="sidemenu-section">
        {categoryLabel || menu.label}
      </div>
      <nav className="py-2">
        {sidebarItems.map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(item.href + '/')
          return (
            <Link
              key={item.href}
              href={item.href}
              className={isActive ? 'sidemenu-item-active' : 'sidemenu-item'}
            >
              {item.label}
            </Link>
          )
        })}
      </nav>
    </aside>
  )
}

interface BreadcrumbProps {
  items: Array<{ label: string; href?: string }>
}

export function Breadcrumb({ items }: BreadcrumbProps) {
  return (
    <nav className="breadcrumb mb-4">
      {items.map((item, idx) => {
        const isLast = idx === items.length - 1
        return (
          <span key={idx} className="flex items-center gap-2">
            {idx > 0 && <span className="breadcrumb-separator">{'>'}</span>}
            {isLast ? (
              <span className="breadcrumb-current">{item.label}</span>
            ) : item.href ? (
              <Link href={item.href} className="hover:text-kb-text">
                {item.label}
              </Link>
            ) : (
              <span>{item.label}</span>
            )}
          </span>
        )
      })}
    </nav>
  )
}

interface PageLayoutProps {
  category: string
  categoryLabel: string
  title: string
  breadcrumb: Array<{ label: string; href?: string }>
  children: ReactNode
}

export default function PageLayout({
  category,
  categoryLabel,
  title,
  breadcrumb,
  children,
}: PageLayoutProps) {
  return (
    <div className="max-w-kb-container mx-auto px-6 py-12">
      <div className="flex gap-8">
        <Sidebar category={category} categoryLabel={categoryLabel} />
        <main className="flex-1 min-w-0">
          <Breadcrumb items={breadcrumb} />
          <h1 className="page-title">{title}</h1>
          <div>{children}</div>
        </main>
      </div>
    </div>
  )
}

export function StubPage({ title }: { title: string }) {
  return (
    <div className="stub-placeholder">
      <p className="text-h2 mb-2">{title}</p>
      <p className="text-sm">구현 예정 페이지입니다</p>
    </div>
  )
}
