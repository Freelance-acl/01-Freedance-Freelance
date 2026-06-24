'use client'

import { useState } from 'react'
import { useParams } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import Link from 'next/link'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { BackButton } from '@/components/ui/back-button'
import { StatusBadge } from '@/components/StatusBadge'
import { LoadingState } from '@/components/LoadingState'
import { useAuthContext } from '@/providers/AuthProvider'
import { contractsApi } from '@/lib/api'
import { formatCurrency, formatDate } from '@/lib/utils'
import {
  DollarSign, Calendar, User, FileSignature, AlertTriangle,
  CheckCircle2, Clock, ExternalLink,
} from 'lucide-react'

const STATUS_CONFIG = {
  ACTIVE: {
    icon: Clock,
    label: 'Active',
    desc: 'Work is in progress. The contract will complete when the freelancer marks the work done.',
    bannerClass: 'from-brand-600 to-violet-700',
    pillClass: 'bg-brand-100 text-brand-700 dark:bg-brand-900/40 dark:text-brand-300',
  },
  COMPLETED: {
    icon: CheckCircle2,
    label: 'Completed',
    desc: 'Work was delivered and payment was processed successfully via the saga.',
    bannerClass: 'from-emerald-600 to-teal-700',
    pillClass: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300',
  },
  TERMINATED: {
    icon: AlertTriangle,
    label: 'Terminated',
    desc: 'This contract was terminated before completion. Any escrow funds were refunded.',
    bannerClass: 'from-red-600 to-rose-700',
    pillClass: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300',
  },
}

export default function ContractDetailPage() {
  const { id }   = useParams<{ id: string }>()
  const { user } = useAuthContext()
  const qc       = useQueryClient()
  const [reason, setReason] = useState('')
  const [showTerminate, setShowTerminate] = useState(false)

  const { data: contract, isLoading } = useQuery({
    queryKey: ['contract', id],
    queryFn:  () => contractsApi.getById(Number(id)),
  })

  const terminateMutation = useMutation({
    mutationFn: () => contractsApi.terminate(Number(id), reason),
    onSuccess:  () => {
      qc.invalidateQueries({ queryKey: ['contract', id] })
      toast.success('Contract terminated.')
      setReason('')
      setShowTerminate(false)
    },
    onError: () => toast.error('Failed to terminate contract.'),
  })

  if (isLoading) return <DashboardLayout title="Contract Details"><LoadingState message="Loading contract…" /></DashboardLayout>
  if (!contract) return <DashboardLayout title="Contract Details"><p className="text-gray-500">Contract not found.</p></DashboardLayout>

  const canTerminate = (user?.role === 'ADMIN' || user?.role === 'CLIENT') && contract.status === 'ACTIVE'
  const cfg = STATUS_CONFIG[contract.status] ?? STATUS_CONFIG.ACTIVE
  const StatusIcon = cfg.icon

  return (
    <DashboardLayout title="Contract Details">
      <div className="mx-auto max-w-4xl space-y-6">

        {/* Back + breadcrumb */}
        <div className="flex items-center gap-3">
          <BackButton href="/contracts" label="All Contracts" />
          <span className="text-gray-300 dark:text-gray-600">/</span>
          <span className="text-sm text-gray-500 dark:text-gray-400">Contract #{contract.id}</span>
        </div>

        {/* Hero banner */}
        <div className={`relative overflow-hidden rounded-2xl bg-gradient-to-br ${cfg.bannerClass} p-8 text-white shadow-xl`}>
          <div className="pointer-events-none absolute right-0 top-0 h-48 w-48 rounded-bl-[4rem] bg-white/5" />
          <div className="relative">
            <div className="mb-2 flex items-center gap-3">
              <div className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold ${cfg.pillClass}`}>
                <StatusIcon className="h-3.5 w-3.5" />
                {cfg.label}
              </div>
              <span className="text-xs text-white/50">Contract #{contract.id}</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold">Contract #{contract.id}</h1>
            <p className="mt-1.5 text-sm text-white/70 max-w-lg">{cfg.desc}</p>

            <div className="mt-6 flex flex-wrap gap-8">
              <div>
                <p className="text-xs text-white/60 uppercase tracking-wide">Agreed Amount</p>
                <p className="text-3xl font-black">{formatCurrency(contract.agreedAmount)}</p>
              </div>
              <div>
                <p className="text-xs text-white/60 uppercase tracking-wide">Start Date</p>
                <p className="text-xl font-bold">{formatDate(contract.startDate)}</p>
              </div>
              {contract.endDate && (
                <div>
                  <p className="text-xs text-white/60 uppercase tracking-wide">End Date</p>
                  <p className="text-xl font-bold">{formatDate(contract.endDate)}</p>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          {/* Main */}
          <div className="space-y-5 lg:col-span-2">
            {/* Parties */}
            <Card>
              <CardHeader><CardTitle className="flex items-center gap-2"><User className="h-4 w-4 text-brand-500" /> Parties</CardTitle></CardHeader>
              <CardContent>
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="rounded-xl bg-gray-50 p-4 dark:bg-gray-800/50">
                    <p className="text-xs font-semibold uppercase tracking-wide text-gray-400 mb-2">Client</p>
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-100 text-brand-700 font-bold text-sm dark:bg-brand-900/40 dark:text-brand-300">
                        C
                      </div>
                      <div>
                        <p className="font-semibold text-gray-900 dark:text-white">Client #{contract.clientId}</p>
                        <Link href={`/users/${contract.clientId}`} className="text-xs text-brand-600 hover:underline dark:text-brand-400">
                          View profile →
                        </Link>
                      </div>
                    </div>
                  </div>
                  <div className="rounded-xl bg-gray-50 p-4 dark:bg-gray-800/50">
                    <p className="text-xs font-semibold uppercase tracking-wide text-gray-400 mb-2">Freelancer</p>
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-100 text-emerald-700 font-bold text-sm dark:bg-emerald-900/40 dark:text-emerald-300">
                        F
                      </div>
                      <div>
                        <p className="font-semibold text-gray-900 dark:text-white">Freelancer #{contract.freelancerId}</p>
                        <Link href={`/users/${contract.freelancerId}`} className="text-xs text-brand-600 hover:underline dark:text-brand-400">
                          View profile →
                        </Link>
                      </div>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Financial summary */}
            <Card>
              <CardHeader><CardTitle className="flex items-center gap-2"><DollarSign className="h-4 w-4 text-emerald-500" /> Financial Summary</CardTitle></CardHeader>
              <CardContent>
                <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                  {[
                    { label: 'Agreed Amount', value: formatCurrency(contract.agreedAmount), color: 'text-gray-900 dark:text-white' },
                    { label: 'Platform Fee (10%)', value: formatCurrency(contract.agreedAmount * 0.10), color: 'text-gray-500' },
                    { label: 'Freelancer Payout', value: formatCurrency(contract.agreedAmount * 0.90), color: 'text-emerald-600 dark:text-emerald-400 font-bold' },
                  ].map(({ label, value, color }) => (
                    <div key={label} className="rounded-xl bg-gray-50 p-4 dark:bg-gray-800/50">
                      <p className="text-xs text-gray-400 uppercase tracking-wide">{label}</p>
                      <p className={`mt-1 text-lg font-bold ${color}`}>{value}</p>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* Metadata */}
            {contract.metadata && Object.keys(contract.metadata).length > 0 && (
              <Card>
                <CardHeader><CardTitle className="text-sm">Metadata</CardTitle></CardHeader>
                <CardContent>
                  <pre className="overflow-x-auto rounded-lg bg-gray-900 p-4 text-xs text-green-300">
                    {JSON.stringify(contract.metadata, null, 2)}
                  </pre>
                </CardContent>
              </Card>
            )}

            {/* Terminate */}
            {canTerminate && (
              <Card className="border-red-200 dark:border-red-900/50">
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle className="text-red-600 flex items-center gap-2">
                      <AlertTriangle className="h-4 w-4" /> Terminate Contract
                    </CardTitle>
                    <button
                      onClick={() => setShowTerminate(!showTerminate)}
                      className="text-xs text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                    >
                      {showTerminate ? 'Cancel' : 'Open'}
                    </button>
                  </div>
                </CardHeader>
                {showTerminate && (
                  <CardContent className="space-y-4">
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                      Terminating a contract will trigger the saga compensation flow — any held funds will be refunded to the client.
                    </p>
                    <textarea
                      value={reason} onChange={(e) => setReason(e.target.value)}
                      placeholder="Reason for termination…"
                      className="min-h-24 w-full rounded-lg border border-red-200 px-3 py-2 text-sm focus:border-red-400 focus:outline-none focus:ring-1 focus:ring-red-400 dark:border-red-800 dark:bg-gray-800 dark:text-white"
                    />
                    <Button
                      variant="danger"
                      isLoading={terminateMutation.isPending}
                      disabled={!reason.trim()}
                      onClick={() => terminateMutation.mutate()}
                    >
                      Confirm Termination
                    </Button>
                  </CardContent>
                )}
              </Card>
            )}
          </div>

          {/* Sidebar */}
          <div className="space-y-4">
            <Card>
              <CardContent className="p-5 space-y-4">
                <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Contract Details</h3>
                {[
                  { label: 'Contract ID',  value: `#${contract.id}`,            icon: FileSignature },
                  { label: 'Job',          value: `#${contract.jobId}`,          icon: FileSignature },
                  { label: 'Proposal',     value: `#${contract.proposalId}`,     icon: FileSignature },
                  { label: 'Start Date',   value: formatDate(contract.startDate), icon: Calendar },
                  { label: 'End Date',     value: contract.endDate ? formatDate(contract.endDate) : '—', icon: Calendar },
                  { label: 'Amount',       value: formatCurrency(contract.agreedAmount), icon: DollarSign },
                ].map(({ label, value, icon: Icon }) => (
                  <div key={label} className="flex items-center gap-3">
                    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-gray-100 dark:bg-gray-800">
                      <Icon className="h-4 w-4 text-gray-500" />
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs text-gray-400">{label}</p>
                      <p className="text-sm font-medium text-gray-900 dark:text-white">{value}</p>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-5">
                <h3 className="mb-3 text-sm font-semibold text-gray-700 dark:text-gray-300">Related</h3>
                <div className="space-y-2">
                  <Link href={`/jobs/${contract.jobId}`} className="flex items-center justify-between rounded-lg border border-gray-200 px-3 py-2.5 text-sm hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800 transition-colors">
                    <span className="font-medium text-gray-700 dark:text-gray-300">Job #{contract.jobId}</span>
                    <ExternalLink className="h-3.5 w-3.5 text-gray-400" />
                  </Link>
                  <Link href={`/proposals/${contract.proposalId}`} className="flex items-center justify-between rounded-lg border border-gray-200 px-3 py-2.5 text-sm hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800 transition-colors">
                    <span className="font-medium text-gray-700 dark:text-gray-300">Proposal #{contract.proposalId}</span>
                    <ExternalLink className="h-3.5 w-3.5 text-gray-400" />
                  </Link>
                  <Link href="/wallet" className="flex items-center justify-between rounded-lg border border-gray-200 px-3 py-2.5 text-sm hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800 transition-colors">
                    <span className="font-medium text-gray-700 dark:text-gray-300">Wallet / Payouts</span>
                    <ExternalLink className="h-3.5 w-3.5 text-gray-400" />
                  </Link>
                  <Link href="/saga" className="flex items-center justify-between rounded-lg border border-brand-200 bg-brand-50 px-3 py-2.5 text-sm hover:bg-brand-100 dark:border-brand-800 dark:bg-brand-900/10 dark:hover:bg-brand-900/20 transition-colors">
                    <span className="font-medium text-brand-700 dark:text-brand-300">View Saga Timeline</span>
                    <ExternalLink className="h-3.5 w-3.5 text-brand-400" />
                  </Link>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
