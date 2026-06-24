export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-brand-50 to-indigo-100 p-4 dark:from-gray-900 dark:to-gray-950">
      {children}
    </div>
  )
}
