'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { StatusBadge } from '@/components/StatusBadge'
import { LoadingState } from '@/components/LoadingState'
import { proposalsApi, contractsApi, walletApi, jobsApi } from '@/lib/api'
import { useAuthContext } from '@/providers/AuthProvider'
import { formatCurrency, formatDate } from '@/lib/utils'

export default function SagaPage() {
  const { user } = useAuthContext()
  const [selected, setSelected] = useState('')
  const proposals = useQuery({ queryKey: ['workflow-proposals'], queryFn: proposalsApi.getAll })
  const contracts = useQuery({ queryKey: ['workflow-contracts'], queryFn: contractsApi.getAll })
  const payouts = useQuery({ queryKey: ['workflow-payouts'], queryFn: walletApi.getPayouts })
  const jobs = useQuery({ queryKey: ['workflow-jobs'], queryFn: () => jobsApi.getAll() })
  const visible = proposals.data?.filter(p => user?.role === 'ADMIN' || p.freelancerId === user?.id ||
    jobs.data?.some(j => j.id === p.jobId && j.clientId === user?.id)) ?? []
  const proposal = visible.find(p => String(p.id) === selected) ?? visible.at(-1)
  const contract = contracts.data?.find(c => c.proposalId === proposal?.id)
  const payout = contract && payouts.data?.find(p => p.contractId === contract.id)
  const error = proposals.isError || contracts.isError || payouts.isError || jobs.isError

  return <DashboardLayout title="Live Workflow">
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h2 className="text-xl font-bold">Proposal &rarr; Contract &rarr; Payout</h2>
        <p className="mt-2 text-sm text-gray-500">Current records from the proposal, contract, and wallet services. Refreshes every 5 seconds. This view shows persisted state, not a RabbitMQ event log.</p>
      </div>
      {error ? <p role="alert" className="text-red-600">Unable to load workflow records. Check the backend and retry.</p> :
        proposals.isLoading || contracts.isLoading || payouts.isLoading || jobs.isLoading ? <LoadingState /> : <>
          <label className="block text-sm font-medium" htmlFor="workflow-proposal">Choose a proposal</label>
          <select id="workflow-proposal" value={proposal?.id ?? ''} onChange={e => setSelected(e.target.value)}
            className="w-full rounded-lg border p-3 dark:bg-gray-900">
            {!visible.length && <option value="">No proposals yet</option>}
            {visible.map(p => <option key={p.id} value={p.id}>Proposal #{p.id} - {jobs.data?.find(j => j.id === p.jobId)?.title ?? `Job #${p.jobId}`}</option>)}
          </select>
          <div className="grid gap-4 md:grid-cols-3">
            <Card><CardHeader><CardTitle>1. Proposal</CardTitle></CardHeader><CardContent>
              {proposal ? <div className="space-y-3"><StatusBadge status={proposal.status} />
                <p>{formatCurrency(proposal.bidAmount)} / {proposal.estimatedDays} days</p>
                <p className="text-sm text-gray-500">Submitted {formatDate(proposal.createdAt)}</p>
                <Link className="text-brand-600 underline" href={`/proposals/${proposal.id}`}>Open proposal #{proposal.id}</Link>
              </div> : <p>No proposal selected.</p>}
            </CardContent></Card>
            <Card><CardHeader><CardTitle>2. Contract</CardTitle></CardHeader><CardContent>
              {contract ? <div className="space-y-3"><StatusBadge status={contract.status} />
                <p>{formatCurrency(contract.agreedAmount)}</p>
                <p className="text-sm text-gray-500">Created {formatDate(contract.createdAt)}</p>
                <Link className="text-brand-600 underline" href={`/contracts/${contract.id}`}>Open contract #{contract.id}</Link>
              </div> : <p className="text-sm text-gray-500">No contract recorded yet. Accept the proposal to request automatic creation.</p>}
            </CardContent></Card>
            <Card><CardHeader><CardTitle>3. Payout</CardTitle></CardHeader><CardContent>
              {payout ? <div className="space-y-3"><StatusBadge status={payout.status} />
                <p>{formatCurrency(payout.amount)}</p>
                <p className="text-sm text-gray-500">Payout #{payout.id} / Contract #{payout.contractId}</p>
                <Link className="text-brand-600 underline" href={user?.role === 'CLIENT' ? `/contracts/${contract!.id}` : '/wallet'}>View payout</Link>
              </div> : <p className="text-sm text-gray-500">No payout recorded yet. Mark accepted work complete to initiate it.</p>}
            </CardContent></Card>
          </div>
          <p className="text-sm text-gray-500">After work completion, the client releases the pending demo payout from the contract page. Payments are simulated by the backend; no real money moves.</p>
        </>}
    </div>
  </DashboardLayout>
}
