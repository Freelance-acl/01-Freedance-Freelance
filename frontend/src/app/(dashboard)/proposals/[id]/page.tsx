'use client'

import { useParams, useRouter } from 'next/navigation'
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
import { proposalsApi } from '@/lib/api'
import { formatCurrency, formatDate } from '@/lib/utils'
import {
  DollarSign, Clock, Calendar, CheckCircle2,
  XCircle, SendHorizonal, AlertTriangle, FileText, ExternalLink,
} from 'lucide-react'

export default function ProposalDetailPage() {
  const { id }   = useParams<{ id: string }>()
  const { user } = useAuthContext()
  const router   = useRouter()
  const qc       = useQueryClient()

  const { data: proposal, isLoading } = useQuery({
    queryKey: ['proposal', id],
    queryFn:  () => proposalsApi.getById(Number(id)),
  })

  const acceptMutation = useMutation({
    mutationFn: () => proposalsApi.accept(Number(id)),
    onSuccess:  () => { qc.invalidateQueries({ queryKey: ['proposal', id] }); toast.success('Proposal accepted — contract will be created automatically.') },
  })
  const rejectMutation = useMutation({
    mutationFn: () => proposalsApi.reject(Number(id)),
    onSuccess:  () => { qc.invalidateQueries({ queryKey: ['proposal', id] }); toast.success('Proposal rejected.') },
  })
  const completingMutation = useMutation({
    mutationFn: () => proposalsApi.markCompleting(Number(id)),
    onSuccess:  () => { qc.invalidateQueries({ queryKey: ['proposal', id] }); toast.success('Marked as completing — payment will be initiated.') },
  })
  const withdrawMutation = useMutation({
    mutationFn: () => proposalsApi.withdraw(Number(id)),
    onSuccess:  () => { router.push('/proposals'); toast.success('Proposal withdrawn.') },
  })

  if (isLoading) return <DashboardLayout title="Proposal Details"><LoadingState message="Loading proposal…" /></DashboardLayout>
  if (!proposal) return <DashboardLayout title="Proposal Details"><p className="text-gray-500">Proposal not found.</p></DashboardLayout>

  const isClient     = user?.role === 'CLIENT' || user?.role === 'ADMIN'
  const isFreelancer = user?.role === 'FREELANCER'
  const canAccept    = isClient && proposal.status === 'SUBMITTED'
  const canReject    = isClient && (proposal.status === 'SUBMITTED' || proposal.status === 'SHORTLISTED')
  const canComplete  = isFreelancer && proposal.status === 'ACCEPTED'
  const canWithdraw  = isFreelancer && proposal.status === 'SUBMITTED'
  const hasActions   = canAccept || canReject || canComplete || canWithdraw

  return (
    <DashboardLayout title="Proposal Details">
      <div className="mx-auto max-w-3xl space-y-6">

        {/* Back + breadcrumb */}
        <div className="flex items-center gap-3">
          <BackButton href="/proposals" label="All Proposals" />
          <span className="text-gray-300 dark:text-gray-600">/</span>
          <span className="text-sm text-gray-500 dark:text-gray-400">Proposal #{proposal.id}</span>
        </div>

        {/* Hero banner */}
        <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-emerald-600 to-teal-700 p-7 text-white shadow-xl">
          <div className="pointer-events-none absolute right-0 top-0 h-40 w-40 rounded-bl-[3rem] bg-white/5" />
          <div className="relative">
            <div className="mb-3 flex flex-wrap items-center gap-2">
              <StatusBadge status={proposal.status} />
              <span className="text-xs text-white/60">Proposal #{proposal.id}</span>
            </div>
            <div className="flex flex-wrap gap-6 text-sm">
              <div>
                <p className="text-white/60 text-xs uppercase tracking-wide">Bid Amount</p>
                <p className="text-3xl font-black text-white">{formatCurrency(proposal.bidAmount)}</p>
              </div>
              <div>
                <p className="text-white/60 text-xs uppercase tracking-wide">Timeline</p>
                <p className="text-2xl font-bold text-white">{proposal.estimatedDays} days</p>
              </div>
              <div>
                <p className="text-white/60 text-xs uppercase tracking-wide">Submitted</p>
                <p className="text-base font-semibold text-white/90">{formatDate(proposal.createdAt)}</p>
              </div>
            </div>
            <div className="mt-4 flex flex-wrap gap-5 text-sm text-white/70">
              <span className="flex items-center gap-1.5"><FileText className="h-4 w-4" /> Job #{proposal.jobId}</span>
              <Link href={`/jobs/${proposal.jobId}`} className="flex items-center gap-1 text-white/80 hover:text-white underline underline-offset-2 text-xs">
                View Job <ExternalLink className="h-3 w-3" />
              </Link>
            </div>
          </div>
        </div>

        {/* Saga status notes */}
        {proposal.status === 'ACCEPTED' && (
          <div className="flex items-start gap-3 rounded-xl border border-brand-200 bg-brand-50 p-4 dark:border-brand-800 dark:bg-brand-900/20">
            <CheckCircle2 className="h-5 w-5 text-brand-600 dark:text-brand-400 shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-semibold text-brand-700 dark:text-brand-300">Contract Created via Saga</p>
              <p className="mt-0.5 text-sm text-brand-600/80 dark:text-brand-400/80">
                The choreography saga has automatically created a contract. Check the{' '}
                <Link href="/contracts" className="underline font-medium">Contracts</Link> page for details.
              </p>
            </div>
          </div>
        )}
        {proposal.status === 'COMPLETING' && (
          <div className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 dark:border-amber-800 dark:bg-amber-900/20">
            <Clock className="h-5 w-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-semibold text-amber-700 dark:text-amber-300">Payment Processing</p>
              <p className="mt-0.5 text-sm text-amber-600/80 dark:text-amber-400/80">
                Work is marked complete. The wallet service is initiating the payout. Check your{' '}
                <Link href="/wallet" className="underline font-medium">Wallet</Link> for status.
              </p>
            </div>
          </div>
        )}
        {proposal.status === 'PAYMENT_FAILED' && (
          <div className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 p-4 dark:border-red-800 dark:bg-red-900/20">
            <AlertTriangle className="h-5 w-5 text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-semibold text-red-700 dark:text-red-300">Payment Failed</p>
              <p className="mt-0.5 text-sm text-red-600/80 dark:text-red-400/80">
                The payout failed. The saga compensation flow is handling the refund. Contact support if this persists.
              </p>
            </div>
          </div>
        )}

        <div className="grid gap-5 lg:grid-cols-3">
          {/* Cover letter */}
          <div className="space-y-5 lg:col-span-2">
            <Card>
              <CardHeader><CardTitle className="flex items-center gap-2"><FileText className="h-4 w-4 text-emerald-500" /> Cover Letter</CardTitle></CardHeader>
              <CardContent>
                <div className="rounded-lg bg-gray-50 p-5 dark:bg-gray-800/50">
                  <p className="text-gray-700 dark:text-gray-300 leading-relaxed whitespace-pre-line">{proposal.coverLetter}</p>
                </div>
              </CardContent>
            </Card>

            {/* Action panel */}
            {hasActions && (
              <Card className="border-gray-200 dark:border-gray-700">
                <CardContent className="p-5">
                  <p className="mb-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Actions</p>
                  <div className="flex flex-wrap gap-3">
                    {canAccept && (
                      <Button variant="success" isLoading={acceptMutation.isPending} onClick={() => acceptMutation.mutate()}>
                        <CheckCircle2 className="h-4 w-4" /> Accept Proposal
                      </Button>
                    )}
                    {canReject && (
                      <Button variant="danger" isLoading={rejectMutation.isPending} onClick={() => rejectMutation.mutate()}>
                        <XCircle className="h-4 w-4" /> Reject
                      </Button>
                    )}
                    {canComplete && (
                      <Button isLoading={completingMutation.isPending} onClick={() => completingMutation.mutate()}>
                        <SendHorizonal className="h-4 w-4" /> Mark as Complete
                      </Button>
                    )}
                    {canWithdraw && (
                      <Button variant="outline" isLoading={withdrawMutation.isPending} onClick={() => withdrawMutation.mutate()}>
                        Withdraw Proposal
                      </Button>
                    )}
                  </div>
                  {canAccept && (
                    <p className="mt-3 text-xs text-gray-400">
                      Accepting will trigger the saga: a contract will be created automatically and other proposals will be rejected.
                    </p>
                  )}
                </CardContent>
              </Card>
            )}
          </div>

          {/* Sidebar details */}
          <div className="space-y-4">
            <Card>
              <CardContent className="p-5 space-y-4">
                <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Proposal Info</h3>
                {[
                  { label: 'Bid Amount',  value: formatCurrency(proposal.bidAmount), icon: DollarSign },
                  { label: 'Timeline',    value: `${proposal.estimatedDays} days`,    icon: Clock },
                  { label: 'Submitted',   value: formatDate(proposal.createdAt),      icon: Calendar },
                  ...(proposal.updatedAt ? [{ label: 'Updated', value: formatDate(proposal.updatedAt), icon: Calendar }] : []),
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
                  <Link href={`/jobs/${proposal.jobId}`} className="flex items-center justify-between rounded-lg border border-gray-200 px-3 py-2.5 text-sm hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800 transition-colors">
                    <span className="font-medium text-gray-700 dark:text-gray-300">Job #{proposal.jobId}</span>
                    <ExternalLink className="h-3.5 w-3.5 text-gray-400" />
                  </Link>
                  <Link href="/contracts" className="flex items-center justify-between rounded-lg border border-gray-200 px-3 py-2.5 text-sm hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800 transition-colors">
                    <span className="font-medium text-gray-700 dark:text-gray-300">Contracts</span>
                    <ExternalLink className="h-3.5 w-3.5 text-gray-400" />
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
