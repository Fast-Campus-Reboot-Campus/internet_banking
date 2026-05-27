import AdminGuard from '@/components/admin/AdminGuard'

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-gray-50">
      <AdminGuard>{children}</AdminGuard>
    </div>
  )
}
