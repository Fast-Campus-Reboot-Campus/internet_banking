import type { Metadata } from 'next'
import { Noto_Sans_KR } from 'next/font/google'
import GlobalChrome from '@/components/layout/GlobalChrome'
import './globals.css'

const notoSansKR = Noto_Sans_KR({
  weight: ['400', '500', '600', '700'],
  subsets: ['latin'],
  display: 'swap',
})

export const metadata: Metadata = {
  title: 'AXful Bank',
  description: 'AXful Bank - AI 차세대 인터넷뱅크',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko" className={notoSansKR.className}>
      <body className="min-h-screen bg-white">
        {children}
        <GlobalChrome />
      </body>
    </html>
  )
}
