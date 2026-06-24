'use client'

import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'

export function DashboardLayout({ children, title }: { children: React.ReactNode; title?: string }) {
  return (
    <div className="flex h-screen overflow-hidden bg-gray-50 dark:bg-gray-950">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Topbar title={title} />
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  )
}
