'use client'

import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { MetricCard } from '@/components/MetricCard'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { StatusBadge } from '@/components/StatusBadge'
import { useAuthContext } from '@/providers/AuthProvider'
import { proposalsApi, contractsApi, walletApi } from '@/lib/api'
import { formatCurrency, formatDate } from '@/lib/utils'
import { FileText, FileSignature, DollarSign, TrendingUp } from 'lucide-react'

export default function FreelancerDashboardPage() {
  const { user } = useAuthContext()

  const { data: proposals, isLoading: pLoading } = useQuery({
    queryKey: ['freelancer-proposals', user?.id],
    queryFn:  () => proposalsApi.getByFreelancer(user!.id),
    enabled:  !!user,
  })
  const { data: contracts, isLoading: cLoading } = useQuery({
    queryKey: ['freelancer-contracts', user?.id],
    queryFn:  () => contractsApi.getByFreelancer(user!.id),
    enabled:  !!user,
  })
  const { data: summary } = useQuery({
    queryKey: ['payout-summary', user?.id],
    queryFn:  () => walletApi.getFreelancerSummary(user!.id),
    enabled:  !!user,
  })

  const isLoading = pLoading || cLoading

  const activeConts     = contracts?.filter((c) => c.status === 'ACTIVE').length ?? 0
  const acceptedProps   = proposals?.filter((p) => p.status === 'ACCEPTED' || p.status === 'COMPLETING').length ?? 0
  const completionCount = contracts?.filter((c) => c.status === 'COMPLETED').length ?? 0
  const totalConts      = contracts?.length ?? 1
  const rate            = totalConts > 0 ? Math.round((completionCount / totalConts) * 100) : 0

  return (
    <DashboardLayout title="Freelancer Dashboard">
      <div className="space-y-6">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <MetricCard isLoading={isLoading} label="Active Proposals"  value={acceptedProps}                          icon={FileText}      color="brand" />
          <MetricCard isLoading={isLoading} label="Active Contracts"  value={activeConts}                            icon={FileSignature} color="amber" />
          <MetricCard isLoading={isLoading} label="Total Earned"      value={formatCurrency(summary?.totalPaid ?? 0)} icon={DollarSign}   color="emerald" />
          <MetricCard isLoading={isLoading} label="Completion Rate"   value={`${rate}%`}                             icon={TrendingUp}    color="emerald" />
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          {/* Recent Proposals */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>My Proposals</CardTitle>
              <Link href="/proposals">
                <Button size="sm" variant="outline">View all</Button>
              </Link>
            </CardHeader>
            <CardContent className="p-0">
              {proposals?.length === 0 ? (
                <p className="p-6 text-center text-sm text-gray-500">No proposals yet. Browse open jobs!</p>
              ) : (
                <ul className="divide-y divide-gray-100 dark:divide-gray-700">
                  {proposals?.slice(0, 5).map((p) => (
                    <li key={p.id} className="flex items-center justify-between px-6 py-4">
                      <div>
                        <Link href={`/proposals/${p.id}`} className="text-sm font-medium text-gray-900 hover:text-brand-600 dark:text-white">
                          Proposal #{p.id}
                        </Link>
                        <p className="text-xs text-gray-400">Job #{p.jobId} · {formatCurrency(p.bidAmount)}</p>
                      </div>
                      <StatusBadge status={p.status} />
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>

          {/* Active Contracts */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>My Contracts</CardTitle>
              <Link href="/contracts">
                <Button size="sm" variant="outline">View all</Button>
              </Link>
            </CardHeader>
            <CardContent className="p-0">
              {contracts?.length === 0 ? (
                <p className="p-6 text-center text-sm text-gray-500">No contracts yet.</p>
              ) : (
                <ul className="divide-y divide-gray-100 dark:divide-gray-700">
                  {contracts?.slice(0, 5).map((c) => (
                    <li key={c.id} className="flex items-center justify-between px-6 py-4">
                      <div>
                        <Link href={`/contracts/${c.id}`} className="text-sm font-medium text-gray-900 hover:text-brand-600 dark:text-white">
                          Contract #{c.id}
                        </Link>
                        <p className="text-xs text-gray-400">{formatCurrency(c.agreedAmount)} · {formatDate(c.startDate)}</p>
                      </div>
                      <StatusBadge status={c.status} />
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Payout Summary */}
        {summary && (
          <Card>
            <CardHeader><CardTitle>Payout Summary</CardTitle></CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
                {[
                  { label: 'Total Paid',    value: formatCurrency(summary.totalPaid),    color: 'text-emerald-600' },
                  { label: 'Pending',       value: formatCurrency(summary.totalPending), color: 'text-amber-600' },
                  { label: 'Failed',        value: formatCurrency(summary.totalFailed),  color: 'text-red-500' },
                  { label: 'Platform Fees', value: formatCurrency(summary.totalPlatformFees), color: 'text-gray-500' },
                ].map(({ label, value, color }) => (
                  <div key={label} className="rounded-lg bg-gray-50 p-4 dark:bg-gray-800">
                    <p className="text-xs text-gray-500">{label}</p>
                    <p className={`mt-1 text-lg font-bold ${color}`}>{value}</p>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </DashboardLayout>
  )
}
