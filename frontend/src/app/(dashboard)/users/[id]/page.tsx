'use client'

import { useParams } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { UserX, Star, Briefcase, DollarSign, Mail, Shield } from 'lucide-react'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { LoadingState } from '@/components/LoadingState'
import { useAuthContext } from '@/providers/AuthProvider'
import { usersApi, walletApi } from '@/lib/api'
import { formatCurrency, formatDate, getInitials } from '@/lib/utils'
import type { User } from '@/lib/types'

export default function UserDetailPage() {
  const { id } = useParams<{ id: string }>()
  const userId = Number(id)
  const qc = useQueryClient()
  const { user: me } = useAuthContext()

  const { data: user, isLoading } = useQuery<User>({
    queryKey: ['user', userId],
    queryFn: () => usersApi.getById(userId),
  })

  const { data: payoutSummary } = useQuery({
    queryKey: ['wallet', 'summary', userId],
    queryFn: () => walletApi.getFreelancerSummary(userId),
    enabled: user?.role === 'FREELANCER',
  })

  const deactivate = useMutation({
    mutationFn: () => usersApi.deactivate(userId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['user', userId] })
      qc.invalidateQueries({ queryKey: ['users'] })
      toast.success('User deactivated.')
    },
    onError: () => toast.error('Failed to deactivate user.'),
  })

  if (isLoading) return <DashboardLayout title="User Profile"><LoadingState message="Loading user…" /></DashboardLayout>
  if (!user) return <DashboardLayout title="User Profile"><p className="text-gray-500">User not found.</p></DashboardLayout>

  const isAdmin = me?.role === 'ADMIN'

  return (
    <DashboardLayout title="User Profile">
      <div className="mx-auto max-w-3xl space-y-6">
        {/* Header card */}
        <Card>
          <CardContent className="p-6">
            <div className="flex items-start gap-5">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-brand-100 text-brand-700 text-xl font-bold dark:bg-brand-900/40 dark:text-brand-300">
                {getInitials(user.name)}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <h1 className="text-xl font-bold text-gray-900 dark:text-white">{user.name}</h1>
                  <Badge variant={user.role === 'ADMIN' ? 'default' : user.role === 'CLIENT' ? 'success' : 'warning'}>
                    {user.role}
                  </Badge>
                  <Badge variant={user.active ? 'success' : 'danger'}>
                    {user.active ? 'Active' : 'Inactive'}
                  </Badge>
                </div>
                {user.bio && (
                  <p className="mt-1.5 text-sm text-gray-600 dark:text-gray-400">{user.bio}</p>
                )}
                {user.rating && (
                  <div className="mt-1.5 flex items-center gap-1 text-sm text-amber-500">
                    <Star className="h-4 w-4 fill-current" />
                    <span className="font-medium">{user.rating.toFixed(1)}</span>
                    <span className="text-gray-400">/ 5</span>
                  </div>
                )}
              </div>
              {isAdmin && user.active && user.role !== 'ADMIN' && (
                <Button
                  variant="danger"
                  size="sm"
                  isLoading={deactivate.isPending}
                  onClick={() => deactivate.mutate()}
                >
                  <UserX className="h-3.5 w-3.5 mr-1" /> Deactivate
                </Button>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Account details */}
        <Card>
          <CardHeader><CardTitle>Account Details</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            {[
              { icon: Mail,   label: 'Email',   value: user.email },
              { icon: Shield, label: 'Role',    value: user.role },
              { icon: Shield, label: 'User ID', value: String(user.id) },
              { icon: Shield, label: 'Joined',  value: formatDate(user.createdAt) },
            ].map(({ icon: Icon, label, value }) => (
              <div key={label} className="flex items-center justify-between border-b border-gray-100 pb-3 last:border-0 dark:border-gray-700">
                <div className="flex items-center gap-2 text-sm text-gray-500">
                  <Icon className="h-4 w-4" />
                  {label}
                </div>
                <span className="text-sm font-medium text-gray-900 dark:text-white">{value}</span>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Skills (freelancer only) */}
        {user.role === 'FREELANCER' && user.skills && user.skills.length > 0 && (
          <Card>
            <CardHeader><CardTitle>Skills</CardTitle></CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-2">
                {user.skills.map((skill) => (
                  <span
                    key={skill}
                    className="rounded-full bg-brand-50 px-3 py-1 text-xs font-medium text-brand-700 dark:bg-brand-900/30 dark:text-brand-300"
                  >
                    {skill}
                  </span>
                ))}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Payout summary (freelancer only) */}
        {user.role === 'FREELANCER' && payoutSummary && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <DollarSign className="h-5 w-5 text-emerald-500" /> Earnings Summary
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                {[
                  { label: 'Total Paid',     value: formatCurrency(payoutSummary.totalPaid) },
                  { label: 'Pending',        value: formatCurrency(payoutSummary.totalPending) },
                  { label: 'Failed',         value: formatCurrency(payoutSummary.totalFailed) },
                  { label: 'Platform Fees',  value: formatCurrency(payoutSummary.totalPlatformFees) },
                  { label: 'Payout Count',   value: String(payoutSummary.payoutCount) },
                ].map(({ label, value }) => (
                  <div key={label} className="rounded-lg bg-gray-50 p-3 dark:bg-gray-800/50">
                    <p className="text-xs text-gray-500">{label}</p>
                    <p className="mt-1 font-semibold text-gray-900 dark:text-white">{value}</p>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Jobs count placeholder (client only) */}
        {user.role === 'CLIENT' && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Briefcase className="h-5 w-5 text-brand-500" /> Activity
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                View this client&apos;s jobs and contracts in the Jobs and Contracts sections.
              </p>
            </CardContent>
          </Card>
        )}
      </div>
    </DashboardLayout>
  )
}
