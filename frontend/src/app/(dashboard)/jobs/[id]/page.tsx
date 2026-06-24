'use client'

import { useState } from 'react'
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
import { jobsApi, proposalsApi } from '@/lib/api'
import { formatCurrency, formatDate, formatRelativeTime } from '@/lib/utils'
import { DollarSign, Calendar, Tag, Users, Send, X, Briefcase, Clock } from 'lucide-react'

export default function JobDetailPage() {
  const { id }    = useParams<{ id: string }>()
  const { user }  = useAuthContext()
  const router    = useRouter()
  const qc        = useQueryClient()
  const [bid, setBid]           = useState('')
  const [letter, setLetter]     = useState('')
  const [days, setDays]         = useState('')
  const [showForm, setShowForm] = useState(false)

  const { data: job, isLoading } = useQuery({
    queryKey: ['job', id],
    queryFn:  () => jobsApi.getById(Number(id)),
  })
  const { data: proposals } = useQuery({
    queryKey: ['proposals-for-job', id],
    queryFn:  () => proposalsApi.getByJob(Number(id)),
    enabled:  user?.role === 'CLIENT' || user?.role === 'ADMIN',
  })

  const submitMutation = useMutation({
    mutationFn: () => proposalsApi.submit({
      jobId: Number(id), freelancerId: user!.id,
      bidAmount: Number(bid), coverLetter: letter, estimatedDays: Number(days),
    }),
    onSuccess: () => {
      toast.success('Proposal submitted!')
      setShowForm(false)
      setBid(''); setLetter(''); setDays('')
    },
    onError: () => toast.error('Failed to submit proposal.'),
  })

  const closeMutation = useMutation({
    mutationFn: () => jobsApi.close(Number(id)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['job', id] })
      toast.success('Job closed.')
    },
  })

  if (isLoading) return <DashboardLayout title="Job Details"><LoadingState message="Loading job…" /></DashboardLayout>
  if (!job) return <DashboardLayout title="Job Details"><p className="text-gray-500">Job not found.</p></DashboardLayout>

  const canSubmit = user?.role === 'FREELANCER' && job.status === 'OPEN'
  const canClose  = (user?.role === 'CLIENT' || user?.role === 'ADMIN') && job.status === 'OPEN'

  return (
    <DashboardLayout title="Job Details">
      <div className="mx-auto max-w-4xl space-y-6">

        {/* Back + breadcrumb */}
        <div className="flex items-center gap-3">
          <BackButton href="/jobs" label="All Jobs" />
          <span className="text-gray-300 dark:text-gray-600">/</span>
          <span className="text-sm text-gray-500 dark:text-gray-400 truncate max-w-xs">{job.title}</span>
        </div>

        {/* Hero banner */}
        <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-brand-600 to-violet-700 p-8 text-white shadow-xl">
          <div className="pointer-events-none absolute right-0 top-0 h-48 w-48 rounded-bl-[4rem] bg-white/5" />
          <div className="relative">
            <div className="mb-4 flex flex-wrap items-start justify-between gap-4">
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-white/20 px-3 py-1 text-xs font-semibold backdrop-blur">
                  {job.category}
                </span>
                <StatusBadge status={job.status} />
              </div>
              {canClose && (
                <Button
                  variant="outline"
                  size="sm"
                  isLoading={closeMutation.isPending}
                  onClick={() => closeMutation.mutate()}
                  className="border-white/30 text-white hover:bg-white/10 dark:border-white/30 dark:text-white"
                >
                  Close Job
                </Button>
              )}
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold leading-snug">{job.title}</h1>
            <div className="mt-4 flex flex-wrap gap-5 text-sm text-white/80">
              <span className="flex items-center gap-1.5">
                <DollarSign className="h-4 w-4 text-emerald-300" />
                <span className="font-bold text-white text-lg">{formatCurrency(job.budget)}</span>
                <span>budget</span>
              </span>
              <span className="flex items-center gap-1.5">
                <Calendar className="h-4 w-4" />
                {formatDate(job.createdAt)}
              </span>
              <span className="flex items-center gap-1.5">
                <Users className="h-4 w-4" />
                {proposals?.length ?? 0} proposal{proposals?.length !== 1 ? 's' : ''}
              </span>
            </div>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          {/* Main content */}
          <div className="space-y-5 lg:col-span-2">
            {/* Description */}
            <Card>
              <CardHeader><CardTitle className="flex items-center gap-2"><Briefcase className="h-4 w-4 text-brand-500" /> Project Description</CardTitle></CardHeader>
              <CardContent>
                <p className="text-gray-700 dark:text-gray-300 leading-relaxed whitespace-pre-line">{job.description}</p>
              </CardContent>
            </Card>

            {/* Requirements */}
            {job.requirements && (
              <Card>
                <CardHeader><CardTitle className="flex items-center gap-2"><Tag className="h-4 w-4 text-violet-500" /> Requirements</CardTitle></CardHeader>
                <CardContent>
                  <p className="text-gray-700 dark:text-gray-300 leading-relaxed">{job.requirements}</p>
                </CardContent>
              </Card>
            )}

            {/* Proposal submission form */}
            {canSubmit && !showForm && (
              <div className="rounded-xl border-2 border-dashed border-brand-200 bg-brand-50/50 p-6 dark:border-brand-800 dark:bg-brand-900/10">
                <p className="font-semibold text-gray-900 dark:text-white">Ready to bid on this project?</p>
                <p className="mt-1 text-sm text-gray-500">Submit a proposal and share why you&apos;re the best fit.</p>
                <Button className="mt-4" onClick={() => setShowForm(true)}>
                  <Send className="h-4 w-4" /> Submit Proposal
                </Button>
              </div>
            )}

            {showForm && (
              <Card className="border-brand-200 dark:border-brand-800">
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle>Your Proposal</CardTitle>
                    <button onClick={() => setShowForm(false)} className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800">
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="flex flex-col gap-1.5">
                      <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Bid Amount (USD)</label>
                      <div className="relative">
                        <DollarSign className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                        <input
                          type="number" value={bid} onChange={(e) => setBid(e.target.value)}
                          className="h-10 w-full rounded-lg border border-gray-300 pl-9 pr-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-800 dark:text-white"
                          placeholder="3000"
                        />
                      </div>
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Estimated Days</label>
                      <div className="relative">
                        <Clock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                        <input
                          type="number" value={days} onChange={(e) => setDays(e.target.value)}
                          className="h-10 w-full rounded-lg border border-gray-300 pl-9 pr-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-800 dark:text-white"
                          placeholder="14"
                        />
                      </div>
                    </div>
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Cover Letter</label>
                    <textarea
                      value={letter} onChange={(e) => setLetter(e.target.value)}
                      className="min-h-32 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-800 dark:text-white"
                      placeholder="Explain your experience, approach, and why you're the best fit for this project…"
                    />
                  </div>
                  <div className="flex gap-3">
                    <Button
                      onClick={() => submitMutation.mutate()}
                      isLoading={submitMutation.isPending}
                      disabled={!bid || !letter || !days}
                    >
                      <Send className="h-4 w-4" /> Submit Proposal
                    </Button>
                    <Button variant="outline" onClick={() => setShowForm(false)}>Cancel</Button>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Proposals list (client/admin) */}
            {proposals && proposals.length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Users className="h-4 w-4 text-brand-500" />
                    Proposals
                    <span className="ml-auto rounded-full bg-brand-100 px-2.5 py-0.5 text-xs font-bold text-brand-700 dark:bg-brand-900/40 dark:text-brand-300">
                      {proposals.length}
                    </span>
                  </CardTitle>
                </CardHeader>
                <CardContent className="p-0">
                  <ul className="divide-y divide-gray-100 dark:divide-gray-800">
                    {proposals.map((p) => (
                      <li key={p.id} className="px-6 py-4 hover:bg-gray-50 dark:hover:bg-gray-800/30 transition-colors">
                        <div className="flex items-start justify-between gap-4">
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-3">
                              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-100 text-brand-700 text-xs font-bold dark:bg-brand-900/40 dark:text-brand-300">
                                #{p.id}
                              </div>
                              <div>
                                <Link href={`/proposals/${p.id}`} className="text-sm font-semibold text-gray-900 hover:text-brand-600 dark:text-white">
                                  Proposal #{p.id}
                                </Link>
                                <p className="text-xs text-gray-400">
                                  {formatCurrency(p.bidAmount)} · {p.estimatedDays} days · {formatRelativeTime(p.createdAt)}
                                </p>
                              </div>
                            </div>
                            <p className="mt-2 text-sm text-gray-500 line-clamp-2 ml-11">{p.coverLetter}</p>
                          </div>
                          <StatusBadge status={p.status} />
                        </div>
                      </li>
                    ))}
                  </ul>
                </CardContent>
              </Card>
            )}
          </div>

          {/* Sidebar */}
          <div className="space-y-4">
            <Card>
              <CardContent className="p-5 space-y-4">
                <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Job Details</h3>
                {[
                  { label: 'Budget',   value: formatCurrency(job.budget),  icon: DollarSign },
                  { label: 'Category', value: job.category,                icon: Tag },
                  { label: 'Posted',   value: formatDate(job.createdAt),   icon: Calendar },
                  { label: 'Status',   value: job.status,                  icon: Briefcase },
                ].map(({ label, value, icon: Icon }) => (
                  <div key={label} className="flex items-center gap-3">
                    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-gray-100 dark:bg-gray-800">
                      <Icon className="h-4 w-4 text-gray-500" />
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs text-gray-400">{label}</p>
                      <p className="text-sm font-medium text-gray-900 dark:text-white truncate">{value}</p>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-5">
                <h3 className="mb-3 text-sm font-semibold text-gray-700 dark:text-gray-300">Quick Actions</h3>
                <div className="space-y-2">
                  <Link href="/jobs" className="flex w-full items-center gap-2 rounded-lg border border-gray-200 px-4 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-800 transition-colors">
                    ← Back to Jobs
                  </Link>
                  <Link href="/proposals" className="flex w-full items-center gap-2 rounded-lg border border-gray-200 px-4 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-800 transition-colors">
                    My Proposals
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
