'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { MetricCard } from '@/components/MetricCard'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { StatusBadge } from '@/components/StatusBadge'
import { LoadingState, EmptyState } from '@/components/LoadingState'
import { useAuthContext } from '@/providers/AuthProvider'
import { walletApi } from '@/lib/api'
import { formatCurrency, formatDate } from '@/lib/utils'
import { DollarSign, Clock, AlertTriangle, RefreshCw } from 'lucide-react'

export default function WalletPage() {
  const { user } = useAuthContext()
  const qc       = useQueryClient()

  const { data: payouts, isLoading: payoutsLoading } = useQuery({
    queryKey: ['payouts'],
    queryFn:  walletApi.getPayouts,
  })
  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ['payout-summary', user?.id],
    queryFn:  () => walletApi.getFreelancerSummary(user!.id),
    enabled:  !!user && user.role === 'FREELANCER',
  })
  const { data: fees } = useQuery({
    queryKey: ['platform-fees'],
    queryFn:  walletApi.getPlatformFees,
    enabled:  user?.role === 'ADMIN',
  })
  const { data: promoCodes } = useQuery({
    queryKey: ['promo-codes'],
    queryFn:  walletApi.getPromoCodes,
    enabled:  user?.role === 'ADMIN',
  })

  const retryMutation = useMutation({
    mutationFn: (id: number) => walletApi.retryPayout(id),
    onSuccess:  () => { qc.invalidateQueries({ queryKey: ['payouts'] }); toast.success('Retry queued.') },
  })

  if (payoutsLoading || summaryLoading) return <DashboardLayout title="Wallet"><LoadingState /></DashboardLayout>

  return (
    <DashboardLayout title="Wallet">
      <div className="space-y-6">
        {/* Freelancer summary */}
        {summary && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <MetricCard label="Total Earned"    value={formatCurrency(summary.totalPaid)}    icon={DollarSign}    color="emerald" />
            <MetricCard label="Pending"         value={formatCurrency(summary.totalPending)} icon={Clock}         color="amber" />
            <MetricCard label="Failed"          value={formatCurrency(summary.totalFailed)}  icon={AlertTriangle} color="red" />
            <MetricCard label="Platform Fees"   value={formatCurrency(summary.totalPlatformFees)} icon={DollarSign} color="brand" />
          </div>
        )}

        {/* Payouts table */}
        <Card>
          <CardHeader><CardTitle>Payout History</CardTitle></CardHeader>
          {payouts?.length === 0 ? (
            <CardContent><EmptyState title="No payouts yet" /></CardContent>
          ) : (
            <CardContent className="p-0">
              <table className="w-full text-sm">
                <thead className="border-b border-gray-100 bg-gray-50 dark:border-gray-700 dark:bg-gray-800/50">
                  <tr>
                    {['ID', 'Contract', 'Amount', 'Platform Fee', 'Status', 'Date', ''].map((h) => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-500">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                  {payouts?.map((p) => (
                    <tr key={p.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/30">
                      <td className="px-4 py-3 text-gray-500">#{p.id}</td>
                      <td className="px-4 py-3 text-gray-500">#{p.contractId}</td>
                      <td className="px-4 py-3 font-medium text-gray-900 dark:text-white">{formatCurrency(p.amount)}</td>
                      <td className="px-4 py-3 text-gray-500">{formatCurrency(p.platformFee)}</td>
                      <td className="px-4 py-3"><StatusBadge status={p.status} /></td>
                      <td className="px-4 py-3 text-gray-400">{formatDate(p.createdAt)}</td>
                      <td className="px-4 py-3">
                        {p.status === 'FAILED' && (
                          <Button size="sm" variant="outline" isLoading={retryMutation.isPending}
                            onClick={() => retryMutation.mutate(p.id)}>
                            <RefreshCw className="h-3 w-3" /> Retry
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </CardContent>
          )}
        </Card>

        {/* Admin: Platform fees breakdown */}
        {fees && (
          <Card>
            <CardHeader><CardTitle>Platform Fee Breakdown</CardTitle></CardHeader>
            <CardContent className="p-0">
              <table className="w-full text-sm">
                <thead className="border-b border-gray-100 bg-gray-50 dark:border-gray-700 dark:bg-gray-800/50">
                  <tr>
                    {['Category', 'Payouts', 'Volume', 'Fees', 'Avg Rate'].map((h) => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-500">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                  {fees.map((f) => (
                    <tr key={f.category}>
                      <td className="px-4 py-3 font-medium text-gray-900 dark:text-white">{f.category}</td>
                      <td className="px-4 py-3 text-gray-500">{f.payoutCount}</td>
                      <td className="px-4 py-3 text-gray-500">{formatCurrency(f.totalVolume)}</td>
                      <td className="px-4 py-3 font-medium text-brand-600">{formatCurrency(f.totalFees)}</td>
                      <td className="px-4 py-3 text-gray-500">{(f.averageFeeRate * 100).toFixed(0)}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </CardContent>
          </Card>
        )}

        {/* Admin: Promo codes */}
        {promoCodes && (
          <Card>
            <CardHeader><CardTitle>Promo Codes</CardTitle></CardHeader>
            <CardContent className="p-0">
              <table className="w-full text-sm">
                <thead className="border-b border-gray-100 bg-gray-50 dark:border-gray-700 dark:bg-gray-800/50">
                  <tr>
                    {['Code', 'Discount', 'Used / Max', 'Expires', 'Status'].map((h) => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-500">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                  {promoCodes.map((c) => (
                    <tr key={c.id}>
                      <td className="px-4 py-3 font-mono font-semibold text-brand-600">{c.code}</td>
                      <td className="px-4 py-3 text-gray-500">{c.discountPercent}%</td>
                      <td className="px-4 py-3 text-gray-500">{c.usageCount} / {c.maxUsage}</td>
                      <td className="px-4 py-3 text-gray-500">{formatDate(c.expiresAt)}</td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${c.active ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'}`}>
                          {c.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </CardContent>
          </Card>
        )}
      </div>
    </DashboardLayout>
  )
}
