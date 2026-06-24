'use client'

import { useState, useMemo } from 'react'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Pagination } from '@/components/ui/pagination'
import { StatusBadge } from '@/components/StatusBadge'
import { LoadingState, EmptyState } from '@/components/LoadingState'
import { useAuthContext } from '@/providers/AuthProvider'
import { jobsApi } from '@/lib/api'
import { formatCurrency, formatDate, truncate } from '@/lib/utils'
import { Plus, Search } from 'lucide-react'
import type { JobStatus } from '@/lib/types'

const PAGE_SIZE = 9

const STATUS_FILTERS: { value: JobStatus | ''; label: string }[] = [
  { value: '',            label: 'All' },
  { value: 'OPEN',       label: 'Open' },
  { value: 'IN_PROGRESS',label: 'In Progress' },
  { value: 'CLOSED',     label: 'Closed' },
]

export default function JobsPage() {
  const { user } = useAuthContext()
  const [search, setSearch]   = useState('')
  const [status, setStatus]   = useState<JobStatus | ''>('')
  const [page, setPage]       = useState(0)

  const { data: jobs, isLoading } = useQuery({
    queryKey: ['jobs', status],
    queryFn:  () => jobsApi.getAll(status || undefined),
  })

  const filtered = useMemo(() => jobs?.filter((j) =>
    !search || j.title.toLowerCase().includes(search.toLowerCase()),
  ) ?? [], [jobs, search])

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paginated  = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

  const handleSearchChange = (val: string) => { setSearch(val); setPage(0) }
  const handleStatusChange = (val: JobStatus | '') => { setStatus(val); setPage(0) }

  return (
    <DashboardLayout title="Jobs">
      <div className="space-y-5">
        {/* Toolbar */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative flex-1 min-w-48">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              className="h-9 w-full rounded-lg border border-gray-300 pl-9 pr-4 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-800 dark:text-white"
              placeholder="Search jobs…"
              value={search}
              onChange={(e) => handleSearchChange(e.target.value)}
            />
          </div>
          <div className="flex gap-1">
            {STATUS_FILTERS.map((f) => (
              <button
                key={f.value}
                onClick={() => handleStatusChange(f.value as JobStatus | '')}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                  status === f.value
                    ? 'bg-brand-600 text-white'
                    : 'text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800'
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>
          {(user?.role === 'CLIENT' || user?.role === 'ADMIN') && (
            <Link href="/jobs/create" className="ml-auto">
              <Button><Plus className="h-4 w-4" /> Post Job</Button>
            </Link>
          )}
        </div>

        {/* List */}
        {isLoading ? (
          <LoadingState />
        ) : filtered.length === 0 ? (
          <EmptyState title="No jobs found" description="Try adjusting your search or filters." />
        ) : (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {paginated.map((job) => (
                <Card key={job.id} className="hover:shadow-md transition-shadow">
                  <CardContent className="p-5">
                    <div className="mb-3 flex items-start justify-between gap-2">
                      <h3 className="font-semibold text-gray-900 dark:text-white leading-snug">
                        <Link href={`/jobs/${job.id}`} className="hover:text-brand-600">{job.title}</Link>
                      </h3>
                      <StatusBadge status={job.status} />
                    </div>
                    <p className="mb-4 text-sm text-gray-500">{truncate(job.description, 100)}</p>
                    <div className="flex items-center justify-between text-xs text-gray-400">
                      <span className="inline-flex items-center gap-1 rounded-md bg-gray-100 px-2 py-1 dark:bg-gray-800">{job.category}</span>
                      <span>{formatCurrency(job.budget)}</span>
                    </div>
                    <p className="mt-2 text-xs text-gray-400">{formatDate(job.createdAt)}</p>
                  </CardContent>
                </Card>
              ))}
            </div>
            <Pagination
              page={page}
              totalPages={totalPages}
              totalItems={filtered.length}
              pageSize={PAGE_SIZE}
              onPageChange={setPage}
            />
          </>
        )}
      </div>
    </DashboardLayout>
  )
}
