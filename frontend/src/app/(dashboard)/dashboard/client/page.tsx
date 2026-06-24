'use client'

import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { MetricCard } from '@/components/MetricCard'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { StatusBadge } from '@/components/StatusBadge'
import { useAuthContext } from '@/providers/AuthProvider'
import { jobsApi, contractsApi } from '@/lib/api'
import { formatCurrency, formatDate } from '@/lib/utils'
import { Briefcase, FileSignature, DollarSign, Plus } from 'lucide-react'

export default function ClientDashboardPage() {
  const { user } = useAuthContext()

  const { data: jobs, isLoading: jobsLoading } = useQuery({
    queryKey: ['client-jobs', user?.id],
    queryFn:  () => jobsApi.getByClient(user!.id),
    enabled:  !!user,
  })
  const { data: contracts, isLoading: contractsLoading } = useQuery({
    queryKey: ['client-contracts', user?.id],
    queryFn:  () => contractsApi.getByClient(user!.id),
    enabled:  !!user,
  })

  const isLoading = jobsLoading || contractsLoading

  const openJobs    = jobs?.filter((j) => j.status === 'OPEN').length ?? 0
  const activeConts = contracts?.filter((c) => c.status === 'ACTIVE').length ?? 0
  const totalSpent  = contracts?.filter((c) => c.status === 'COMPLETED')
    .reduce((sum, c) => sum + c.agreedAmount, 0) ?? 0

  return (
    <DashboardLayout title="Client Dashboard">
      <div className="space-y-6">
        {/* Metrics */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <MetricCard isLoading={isLoading} label="Total Jobs Posted"  value={jobs?.length ?? 0}         icon={Briefcase}     color="brand" />
          <MetricCard isLoading={isLoading} label="Open Jobs"          value={openJobs}                  icon={Briefcase}     color="emerald" />
          <MetricCard isLoading={isLoading} label="Active Contracts"   value={activeConts}               icon={FileSignature} color="amber" />
          <MetricCard isLoading={isLoading} label="Total Spent"        value={formatCurrency(totalSpent)} icon={DollarSign}   color="emerald" />
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          {/* Recent Jobs */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>My Jobs</CardTitle>
              <Link href="/jobs/create">
                <Button size="sm"><Plus className="h-3.5 w-3.5" /> Post Job</Button>
              </Link>
            </CardHeader>
            <CardContent className="p-0">
              {jobs?.length === 0 ? (
                <p className="p-6 text-center text-sm text-gray-500">No jobs posted yet.</p>
              ) : (
                <ul className="divide-y divide-gray-100 dark:divide-gray-700">
                  {jobs?.slice(0, 5).map((job) => (
                    <li key={job.id} className="flex items-center justify-between px-6 py-4">
                      <div>
                        <Link href={`/jobs/${job.id}`} className="text-sm font-medium text-gray-900 hover:text-brand-600 dark:text-white">
                          {job.title}
                        </Link>
                        <p className="text-xs text-gray-400">{formatDate(job.createdAt)}</p>
                      </div>
                      <StatusBadge status={job.status} />
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>

          {/* Recent Contracts */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>Contracts</CardTitle>
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
                        <p className="text-xs text-gray-400">{formatCurrency(c.agreedAmount)}</p>
                      </div>
                      <StatusBadge status={c.status} />
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </DashboardLayout>
  )
}
