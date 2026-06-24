'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { cn } from '@/lib/utils'
import { useAuthContext } from '@/providers/AuthProvider'
import {
  LayoutDashboard, Briefcase, FileText, FileSignature,
  Wallet, GitBranch, Activity, Settings, LogOut, ChevronRight, Users,
} from 'lucide-react'

const NAV_ITEMS = [
  { href: '/dashboard',     label: 'Dashboard',    icon: LayoutDashboard, roles: ['ADMIN', 'CLIENT', 'FREELANCER'] },
  { href: '/jobs',          label: 'Jobs',         icon: Briefcase,       roles: ['ADMIN', 'CLIENT', 'FREELANCER'] },
  { href: '/proposals',     label: 'Proposals',    icon: FileText,        roles: ['ADMIN', 'CLIENT', 'FREELANCER'] },
  { href: '/contracts',     label: 'Contracts',    icon: FileSignature,   roles: ['ADMIN', 'CLIENT', 'FREELANCER'] },
  { href: '/wallet',        label: 'Wallet',       icon: Wallet,          roles: ['ADMIN', 'FREELANCER'] },
  { href: '/users',         label: 'Users',        icon: Users,           roles: ['ADMIN'] },
  { href: '/saga',          label: 'Saga Flow',    icon: GitBranch,       roles: ['ADMIN', 'CLIENT', 'FREELANCER'] },
  { href: '/observability', label: 'Observability',icon: Activity,        roles: ['ADMIN'] },
  { href: '/settings',      label: 'Settings',     icon: Settings,        roles: ['ADMIN', 'CLIENT', 'FREELANCER'] },
] as const

export function Sidebar() {
  const pathname = usePathname()
  const { user, logout } = useAuthContext()

  const visible = NAV_ITEMS.filter(
    (item) => !user || (item.roles as readonly string[]).includes(user.role),
  )

  return (
    <aside className="flex h-full w-64 flex-col border-r border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-900">
      {/* Brand */}
      <div className="flex h-16 items-center gap-2.5 border-b border-gray-200 px-5 dark:border-gray-800">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 text-white text-sm font-bold">F</div>
        <span className="text-lg font-bold text-gray-900 dark:text-white">Freedance</span>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto px-3 py-4">
        <ul className="space-y-0.5">
          {visible.map(({ href, label, icon: Icon }) => {
            const active = pathname === href || pathname.startsWith(href + '/')
            return (
              <li key={href}>
                <Link
                  href={href}
                  className={cn(
                    'group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                    active
                      ? 'bg-brand-50 text-brand-700 dark:bg-brand-900/30 dark:text-brand-300'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-white',
                  )}
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  {label}
                  {active && <ChevronRight className="ml-auto h-3.5 w-3.5 opacity-50" />}
                </Link>
              </li>
            )
          })}
        </ul>
      </nav>

      {/* User footer */}
      {user && (
        <div className="border-t border-gray-200 p-3 dark:border-gray-800">
          <div className="flex items-center gap-3 rounded-lg px-2 py-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-brand-700 text-xs font-bold dark:bg-brand-900/40 dark:text-brand-300">
              {user.name.charAt(0).toUpperCase()}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-gray-900 dark:text-white">{user.name}</p>
              <p className="truncate text-xs text-gray-500">{user.role}</p>
            </div>
            <button onClick={logout} title="Log out" className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200">
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </aside>
  )
}
