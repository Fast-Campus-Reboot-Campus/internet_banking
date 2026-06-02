import Header from '@/components/layout/Header'
import AuthGuard from '@/components/layout/AuthGuard'
import FloatingSidebar from '@/components/layout/FloatingSidebar'
import Footer from '@/components/layout/Footer'

export default function PersonalLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-white">
      <Header />
      <FloatingSidebar />
      <AuthGuard>{children}</AuthGuard>
      <Footer />
    </div>
  )
}
