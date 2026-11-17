'use client'

import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { useSyncExternalStore } from 'react'
import { useQueryClient } from '@tanstack/react-query'

export function DashboardLayout({ children, title }: { children: React.ReactNode; title?: string }) {
  const client = useQueryClient()
  const hasErrors = useSyncExternalStore(
    onChange => client.getQueryCache().subscribe(onChange),
    () => client.getQueryCache().findAll({ type: 'active' }).some(query => query.state.status === 'error'),
    () => false,
  )
  return (
    <div className="flex h-screen overflow-hidden bg-gray-50 dark:bg-gray-950">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Topbar title={title} />
        <main className="flex-1 overflow-y-auto p-6">
          {hasErrors && <p role="alert" className="mb-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">Some backend data could not be loaded. The displayed totals may be incomplete. Please retry.</p>}
          {children}
        </main>
      </div>
    </div>
  )
}
