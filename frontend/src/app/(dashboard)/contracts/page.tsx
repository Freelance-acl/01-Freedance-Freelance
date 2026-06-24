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
import { contractsApi } from '@/lib/api'
import { formatCurrency, formatDate } from '@/lib/utils'
import type { ContractStatus } from '@/lib/types'

const PAGE_SIZE = 10

const FILTERS: { value: ContractStatus | ''; label: string }[] = [
  { value: '',          label: 'All' },
  { value: 'ACTIVE',    label: 'Active' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'TERMINATED',label: 'Terminated' },
]

export default function ContractsPage() {
  const { user }  = useAuthContext()
  const [status, setStatus] = useState<ContractStatus | ''>('')
  const [page, setPage]     = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['contracts', user?.id, user?.role],
    queryFn:  () => {
      if (!user) return Promise.resolve([])
      if (user.role === 'ADMIN')      return contractsApi.getAll()
      if (user.role === 'CLIENT')     return contractsApi.getByClient(user.id)
      if (user.role === 'FREELANCER') return contractsApi.getByFreelancer(user.id)
      return contractsApi.getAll()
    },
    enabled: !!user,
  })

  const filtered = useMemo(
    () => data?.filter((c) => !status || c.status === status) ?? [],
    [data, status],
  )

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paginated  = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

  const handleStatusChange = (val: ContractStatus | '') => { setStatus(val); setPage(0) }

  return (
    <DashboardLayout title="Contracts">
      <div className="space-y-5">
        <div className="flex gap-1.5">
          {FILTERS.map((f) => (
            <button key={f.value} onClick={() => handleStatusChange(f.value as ContractStatus | '')}
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
          <EmptyState title="No contracts found" />
        ) : (
          <>
            <div className="space-y-3">
              {paginated.map((c) => (
                <Card key={c.id} className="hover:shadow-md transition-shadow">
                  <CardContent className="p-5">
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <Link href={`/contracts/${c.id}`} className="text-sm font-semibold text-gray-900 hover:text-brand-600 dark:text-white">
                          Contract #{c.id}
                        </Link>
                        <p className="mt-0.5 text-xs text-gray-400">
                          Job #{c.jobId} · {formatCurrency(c.agreedAmount)} · Started {formatDate(c.startDate)}
                          {c.endDate && ` · Ended ${formatDate(c.endDate)}`}
                        </p>
                      </div>
                      <StatusBadge status={c.status} />
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
