'use client'

import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { SagaTimeline } from '@/components/SagaTimeline'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { mockSagaSteps } from '@/lib/mock-data'

const FLOW_DOCS = [
  {
    title: '1. Proposal Submission',
    desc: 'Freelancer submits a proposal. proposal-service publishes PROPOSAL_SUBMITTED to RabbitMQ exchange.',
    color: 'border-brand-400',
  },
  {
    title: '2. Proposal Accepted',
    desc: 'Client accepts. proposal-service publishes PROPOSAL_ACCEPTED. contract-service listens and creates a Contract.',
    color: 'border-amber-400',
  },
  {
    title: '3. Contract Created',
    desc: 'contract-service emits CONTRACT_CREATED. proposal-service updates status to ACCEPTED.',
    color: 'border-emerald-400',
  },
  {
    title: '4. Work Completing',
    desc: 'Freelancer marks work done. proposal-service publishes PROPOSAL_COMPLETING → wallet-service initiates payout.',
    color: 'border-purple-400',
  },
  {
    title: '5. Payment Result',
    desc: 'wallet-service publishes PAYMENT_COMPLETED or PAYMENT_FAILED. contract-service marks contract COMPLETED or handles refund.',
    color: 'border-pink-400',
  },
]

export default function SagaPage() {
  const steps = mockSagaSteps

  return (
    <DashboardLayout title="Saga Flow">
      <div className="grid gap-6 lg:grid-cols-5">
        {/* Timeline */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Live Saga Timeline</CardTitle>
            <CardDescription>Choreography events for Contract #301</CardDescription>
          </CardHeader>
          <CardContent>
            <SagaTimeline steps={steps} />
          </CardContent>
        </Card>

        {/* Architecture explanation */}
        <div className="space-y-4 lg:col-span-3">
          <Card>
            <CardHeader>
              <CardTitle>Choreography Architecture</CardTitle>
              <CardDescription>
                FreeDance implements a <strong>choreography-based saga</strong> — no central orchestrator.
                Each service publishes domain events; other services react asynchronously via RabbitMQ.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {FLOW_DOCS.map((step) => (
                <div key={step.title} className={`rounded-lg border-l-4 ${step.color} bg-gray-50 p-4 dark:bg-gray-800/50`}>
                  <p className="text-sm font-semibold text-gray-900 dark:text-white">{step.title}</p>
                  <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">{step.desc}</p>
                </div>
              ))}
            </CardContent>
          </Card>

          {/* RabbitMQ config highlight */}
          <Card>
            <CardHeader>
              <CardTitle>RabbitMQ Exchange & Queues</CardTitle>
            </CardHeader>
            <CardContent>
              <pre className="overflow-x-auto rounded-lg bg-gray-900 p-4 text-xs text-green-300">
{`Exchange:  freelance.events  (topic, durable)
Queues:
  contract.created          → proposal-service
  proposal.accepted         → contract-service
  proposal.completing       → wallet-service
  payment.completed         → contract-service
  payment.failed            → contract-service
  user.deactivated          → contract-service

Dead-Letter:
  freelance.dlx / freelance.dlq
  (failed messages held for inspection)`}
              </pre>
            </CardContent>
          </Card>
        </div>
      </div>
    </DashboardLayout>
  )
}
