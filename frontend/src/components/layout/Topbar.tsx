'use client'

import { useState, useRef, useEffect } from 'react'
import { usePathname } from 'next/navigation'
import Link from 'next/link'
import { Bell, ChevronRight } from 'lucide-react'
import { useAuthContext } from '@/providers/AuthProvider'
import { getInitials } from '@/lib/utils'
import type { UserRole } from '@/lib/types'

// ─── Breadcrumbs ──────────────────────────────────────────────────────────────

const SEGMENT_LABELS: Record<string, string> = {
  dashboard:     'Dashboard',
  jobs:          'Jobs',
  proposals:     'Proposals',
  contracts:     'Contracts',
  wallet:        'Wallet',
  users:         'Users',
  saga:          'Live Workflow',
  observability: 'Observability',
  settings:      'Settings',
  create:        'Create',
  admin:         'Admin',
  client:        'Client',
  freelancer:    'Freelancer',
}

function buildCrumbs(pathname: string) {
  const segments = pathname.split('/').filter(Boolean)
  const crumbs: { label: string; href: string }[] = []
  let path = ''
  for (const seg of segments) {
    path += `/${seg}`
    const isId = /^\d+$/.test(seg)
    const label = isId ? `#${seg}` : (SEGMENT_LABELS[seg] ?? seg)
    crumbs.push({ label, href: path })
  }
  return crumbs
}

// ─── Notification data ────────────────────────────────────────────────────────

interface Notification {
  id: number
  icon: React.ElementType
  iconColor: string
  text: string
  time: string
}

const NOTIFICATIONS: Record<UserRole, Notification[]> = { ADMIN: [], CLIENT: [], FREELANCER: [] }

export function Topbar({ title }: { title?: string }) {
  const { user }   = useAuthContext()
  const pathname   = usePathname()
  const crumbs     = buildCrumbs(pathname)

  const [open, setOpen]       = useState(false)
  const [readIds, setReadIds] = useState<Set<number>>(new Set())
  const panelRef              = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    function handleClick(e: MouseEvent) {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [open])

  const role          = (user?.role ?? 'CLIENT') as UserRole
  const notifications = NOTIFICATIONS[role] ?? []
  const unread        = notifications.filter((n) => !readIds.has(n.id)).length

  function markAllRead() {
    setReadIds(new Set(notifications.map((n) => n.id)))
  }

  return (
    <header className="flex h-16 shrink-0 items-center justify-between border-b border-gray-200 bg-white px-6 dark:border-gray-800 dark:bg-gray-900">
      {/* Left: title + breadcrumbs */}
      <div className="min-w-0 flex-1">
        <h1 className="text-base font-semibold text-gray-900 dark:text-white truncate">
          {title ?? 'Dashboard'}
        </h1>
        {crumbs.length > 1 && (
          <nav aria-label="Breadcrumb" className="hidden sm:flex items-center gap-1 mt-0.5">
            {crumbs.map((crumb, i) => (
              <span key={crumb.href} className="flex items-center gap-1">
                {i > 0 && <ChevronRight className="h-3 w-3 text-gray-300 dark:text-gray-600 shrink-0" />}
                {i === crumbs.length - 1 ? (
                  <span className="text-xs font-medium text-gray-500 dark:text-gray-400 truncate max-w-[120px]">
                    {crumb.label}
                  </span>
                ) : (
                  <Link href={crumb.href} className="text-xs text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300 transition-colors truncate max-w-[100px]">
                    {crumb.label}
                  </Link>
                )}
              </span>
            ))}
          </nav>
        )}
      </div>

      {/* Right: notifications + user */}
      <div className="flex items-center gap-2 ml-4">
        <span className="rounded-full bg-gray-100 px-2 py-1 text-xs text-gray-600 dark:bg-gray-800 dark:text-gray-300">{process.env.NEXT_PUBLIC_MOCK_MODE === 'true' ? 'Mock data' : 'Live API'}</span>

        {/* Bell + dropdown */}
        <div className="relative" ref={panelRef}>
          <button
            onClick={() => setOpen((v) => !v)}
            className="relative rounded-xl p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300 transition-colors"
            aria-label="Notifications"
          >
            <Bell className="h-4 w-4" />
            {unread > 0 ? (
              <span className="absolute right-1 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-brand-500 text-[9px] font-bold text-white ring-1 ring-white dark:ring-gray-900">
                {unread > 9 ? '9+' : unread}
              </span>
            ) : (
              null
            )}
          </button>

          {open && (
            <div className="absolute right-0 top-full z-50 mt-2 w-80 rounded-2xl border border-gray-200 bg-white shadow-xl dark:border-gray-700 dark:bg-gray-900">
              {/* Header */}
              <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3 dark:border-gray-700">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-semibold text-gray-900 dark:text-white">Notifications</span>
                  {unread > 0 && (
                    <span className="rounded-full bg-brand-100 px-1.5 py-0.5 text-[10px] font-bold text-brand-700 dark:bg-brand-900/40 dark:text-brand-300">
                      {unread} new
                    </span>
                  )}
                </div>
                {unread > 0 && (
                  <button
                    onClick={markAllRead}
                    className="text-xs text-brand-600 hover:text-brand-700 dark:text-brand-400 dark:hover:text-brand-300 font-medium transition-colors"
                  >
                    Mark all read
                  </button>
                )}
              </div>

              {notifications.length === 0 && <p className="px-4 py-6 text-sm text-gray-500">No live notification feed is configured.</p>}
              {/* List */}
              <ul className="max-h-80 overflow-y-auto divide-y divide-gray-50 dark:divide-gray-800">
                {notifications.map((n) => {
                  const Icon    = n.icon
                  const isRead  = readIds.has(n.id)
                  return (
                    <li
                      key={n.id}
                      onClick={() => setReadIds((s) => { const next = new Set(s); next.add(n.id); return next })}
                      className={`flex cursor-pointer items-start gap-3 px-4 py-3 transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/60 ${isRead ? 'opacity-60' : ''}`}
                    >
                      <div className={`mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full ${n.iconColor}`}>
                        <Icon className="h-3.5 w-3.5" />
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className={`text-xs leading-snug ${isRead ? 'text-gray-500 dark:text-gray-400' : 'font-medium text-gray-800 dark:text-gray-200'}`}>
                          {n.text}
                        </p>
                        <p className="mt-0.5 text-[10px] text-gray-400">{n.time}</p>
                      </div>
                      {!isRead && (
                        <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" />
                      )}
                    </li>
                  )
                })}
              </ul>

              {/* Footer */}
              <div className="border-t border-gray-100 px-4 py-2.5 dark:border-gray-700">
                <p className="text-center text-xs text-gray-400 dark:text-gray-500">
                  Notifications are not connected. View current records on the Workflow page.
                </p>
              </div>
            </div>
          )}
        </div>

        {/* User avatar */}
        {user && (
          <Link
            href="/settings"
            className="flex items-center gap-2.5 rounded-xl px-2 py-1.5 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
          >
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-brand-700 text-xs font-bold dark:bg-brand-900/40 dark:text-brand-300 ring-2 ring-brand-200 dark:ring-brand-800">
              {getInitials(user.name)}
            </div>
            <div className="hidden sm:block text-left">
              <p className="text-sm font-semibold text-gray-900 dark:text-white leading-tight">{user.name}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400 leading-tight">{user.role}</p>
            </div>
          </Link>
        )}
      </div>
    </header>
  )
}
