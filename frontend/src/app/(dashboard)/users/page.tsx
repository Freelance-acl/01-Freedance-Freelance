'use client'

import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import Link from 'next/link'
import { toast } from 'sonner'
import { UserX, Eye, Users } from 'lucide-react'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { LoadingState, EmptyState } from '@/components/LoadingState'
import { usersApi } from '@/lib/api'
import { cn, formatDate, getInitials } from '@/lib/utils'
import type { User, UserRole } from '@/lib/types'

type RoleFilter = 'ALL' | UserRole
type StatusFilter = 'ALL' | 'ACTIVE' | 'INACTIVE'

const ROLE_FILTERS: { label: string; value: RoleFilter }[] = [
  { label: 'All Roles', value: 'ALL' },
  { label: 'Admin',     value: 'ADMIN' },
  { label: 'Client',    value: 'CLIENT' },
  { label: 'Freelancer', value: 'FREELANCER' },
]

const STATUS_FILTERS: { label: string; value: StatusFilter }[] = [
  { label: 'All',      value: 'ALL' },
  { label: 'Active',   value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
]

const ROLE_VARIANT: Record<UserRole, 'default' | 'success' | 'warning' | 'neutral'> = {
  ADMIN:      'default',
  CLIENT:     'success',
  FREELANCER: 'warning',
}

export default function UsersPage() {
  const qc = useQueryClient()
  const [search, setSearch]       = useState('')
  const [roleFilter, setRole]     = useState<RoleFilter>('ALL')
  const [statusFilter, setStatus] = useState<StatusFilter>('ALL')

  const { data: users = [], isLoading } = useQuery<User[]>({
    queryKey: ['users'],
    queryFn: () => usersApi.getAll(),
  })

  const deactivate = useMutation({
    mutationFn: (id: number) => usersApi.deactivate(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] })
      toast.success('User deactivated.')
    },
    onError: () => toast.error('Failed to deactivate user.'),
  })

  const filtered = useMemo(() => {
    return users.filter((u) => {
      const matchSearch =
        !search ||
        u.name.toLowerCase().includes(search.toLowerCase()) ||
        u.email.toLowerCase().includes(search.toLowerCase())
      const matchRole   = roleFilter === 'ALL' || u.role === roleFilter
      const matchStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && u.active) ||
        (statusFilter === 'INACTIVE' && !u.active)
      return matchSearch && matchRole && matchStatus
    })
  }, [users, search, roleFilter, statusFilter])

  return (
    <DashboardLayout title="User Management">
      <div className="space-y-5">
        {/* Toolbar */}
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="w-full max-w-xs">
            <Input
              id="user-search"
              placeholder="Search by name or email…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="flex flex-wrap gap-2">
            {ROLE_FILTERS.map(({ label, value }) => (
              <button
                key={value}
                onClick={() => setRole(value)}
                className={cn(
                  'rounded-full px-3 py-1 text-xs font-medium transition-colors',
                  roleFilter === value
                    ? 'bg-brand-600 text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-300',
                )}
              >
                {label}
              </button>
            ))}
          </div>
          <div className="flex flex-wrap gap-2">
            {STATUS_FILTERS.map(({ label, value }) => (
              <button
                key={value}
                onClick={() => setStatus(value)}
                className={cn(
                  'rounded-full px-3 py-1 text-xs font-medium transition-colors',
                  statusFilter === value
                    ? 'bg-gray-800 text-white dark:bg-gray-200 dark:text-gray-900'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-300',
                )}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        {/* Stats row */}
        <p className="text-sm text-gray-500 dark:text-gray-400">
          Showing <span className="font-semibold text-gray-900 dark:text-white">{filtered.length}</span> of {users.length} users
        </p>

        {/* Table */}
        {isLoading ? (
          <LoadingState message="Loading users…" />
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={<Users className="h-8 w-8 text-gray-400" />}
            title="No users found"
            description="Try adjusting your search or filters."
          />
        ) : (
          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-100 dark:border-gray-800">
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-400">User</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-400">Role</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-400">Status</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-400">Joined</th>
                      <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-gray-400">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50 dark:divide-gray-800/60">
                    {filtered.map((user) => (
                      <tr key={user.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/30 transition-colors">
                        {/* User cell */}
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3">
                            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-100 text-brand-700 text-xs font-bold dark:bg-brand-900/40 dark:text-brand-300">
                              {getInitials(user.name)}
                            </div>
                            <div>
                              <p className="font-medium text-gray-900 dark:text-white">{user.name}</p>
                              <p className="text-xs text-gray-500">{user.email}</p>
                            </div>
                          </div>
                        </td>
                        {/* Role */}
                        <td className="px-4 py-3">
                          <Badge variant={ROLE_VARIANT[user.role]}>{user.role}</Badge>
                        </td>
                        {/* Status */}
                        <td className="px-4 py-3">
                          <Badge variant={user.active ? 'success' : 'danger'}>
                            {user.active ? 'Active' : 'Inactive'}
                          </Badge>
                        </td>
                        {/* Joined */}
                        <td className="px-4 py-3 text-gray-500 dark:text-gray-400">
                          {formatDate(user.createdAt)}
                        </td>
                        {/* Actions */}
                        <td className="px-4 py-3">
                          <div className="flex items-center justify-end gap-2">
                            <Link href={`/users/${user.id}`}>
                              <Button variant="ghost" size="sm">
                                <Eye className="h-3.5 w-3.5 mr-1" /> View
                              </Button>
                            </Link>
                            {user.active && user.role !== 'ADMIN' && (
                              <Button
                                variant="danger"
                                size="sm"
                                isLoading={deactivate.isPending && deactivate.variables === user.id}
                                onClick={() => deactivate.mutate(user.id)}
                              >
                                <UserX className="h-3.5 w-3.5 mr-1" /> Deactivate
                              </Button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </DashboardLayout>
  )
}
