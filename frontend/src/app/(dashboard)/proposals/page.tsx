'use client'

import { useState, useMemo } from 'react'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { Card, CardContent } from '@/components/ui/card'
import { Pagination } from '@/components/ui/pagination'
import { StatusBadge } from '@/components/StatusBadge'
import { LoadingState, EmptyState } from '@/components/LoadingState'
import { useAuthContext } from '@/providers/AuthProvider'
import { proposalsApi } from '@/lib/api'
import { formatCurrency, formatDate } from '@/lib/utils'
import type { ProposalStatus } from '@/lib/types'

const PAGE_SIZE = 10

const STATUS_FILTERS: { value: ProposalStatus | ''; label: string }[] = [
  { value: '',           label: 'All' },
  { value: 'SUBMITTED',  label: 'Submitted' },
  { value: 'SHORTLISTED',label: 'Shortlisted' },
  { value: 'ACCEPTED',   label: 'Accepted' },
  { value: 'COMPLETING', label: 'Completing' },
  { value: 'PAID',       label: 'Paid' },
  { value: 'REJECTED',   label: 'Rejected' },
]

export default function ProposalsPage() {
  const { user }  = useAuthContext()
  const [status, setStatus] = useState<ProposalStatus | ''>('')
  const [page, setPage]     = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['proposals', user?.id, user?.role],
    queryFn:  () => {
      if (!user) return Promise.resolve([])
      if (user.role === 'ADMIN') return proposalsApi.getAll()
      if (user.role === 'FREELANCER') return proposalsApi.getByFreelancer(user.id)
      return proposalsApi.getAll()
    },
    enabled: !!user,
  })

  const filtered = useMemo(
    () => data?.filter((p) => !status || p.status === status) ?? [],
    [data, status],
  )

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paginated  = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

  const handleStatusChange = (val: ProposalStatus | '') => { setStatus(val); setPage(0) }

  return (
    <DashboardLayout title="Proposals">
      <div className="space-y-5">
        {/* Status filters */}
        <div className="flex flex-wrap gap-1.5">
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => handleStatusChange(f.value as ProposalStatus | '')}
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

        {isLoading ? (
          <LoadingState />
        ) : filtered.length === 0 ? (
          <EmptyState title="No proposals found" description="Browse open jobs and submit your first proposal." />
        ) : (
          <>
            <div className="space-y-3">
              {paginated.map((p) => (
                <Card key={p.id} className="hover:shadow-md transition-shadow">
                  <CardContent className="p-5">
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1 min-w-0">
                        <Link href={`/proposals/${p.id}`} className="text-sm font-semibold text-gray-900 hover:text-brand-600 dark:text-white">
                          Proposal #{p.id}
                        </Link>
                        <p className="mt-0.5 text-xs text-gray-400">
                          Job #{p.jobId} · Bid: {formatCurrency(p.bidAmount)} · {p.estimatedDays} days · {formatDate(p.createdAt)}
                        </p>
                        <p className="mt-2 text-sm text-gray-500 line-clamp-2">{p.coverLetter}</p>
                      </div>
                      <StatusBadge status={p.status} />
                    </div>
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
