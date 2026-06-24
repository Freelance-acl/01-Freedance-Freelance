import { Badge } from './ui/badge'
import type { BadgeProps } from './ui/badge'
import type { JobStatus, ProposalStatus, ContractStatus, PayoutStatus } from '@/lib/types'

type AnyStatus = JobStatus | ProposalStatus | ContractStatus | PayoutStatus

const STATUS_VARIANT: Record<string, BadgeProps['variant']> = {
  // Job
  OPEN:            'success',
  IN_PROGRESS:     'default',
  CLOSED:          'neutral',
  CANCELLED:       'danger',
  // Proposal
  SUBMITTED:       'warning',
  SHORTLISTED:     'default',
  ACCEPTED:        'success',
  COMPLETING:      'default',
  PAYMENT_PENDING: 'warning',
  PAID:            'success',
  PAYMENT_FAILED:  'danger',
  REFUNDED:        'neutral',
  WITHDRAWN:       'neutral',
  REJECTED:        'danger',
  // Contract
  ACTIVE:          'success',
  COMPLETED:       'default',
  TERMINATED:      'danger',
  // Payout
  PENDING:         'warning',
  FAILED:          'danger',
}

const STATUS_LABEL: Partial<Record<string, string>> = {
  IN_PROGRESS:     'In Progress',
  PAYMENT_PENDING: 'Payment Pending',
  PAYMENT_FAILED:  'Payment Failed',
}

export function StatusBadge({ status }: { status: AnyStatus }) {
  const variant = STATUS_VARIANT[status] ?? 'neutral'
  const label   = STATUS_LABEL[status] ?? status.charAt(0) + status.slice(1).toLowerCase().replace('_', ' ')
  return <Badge variant={variant}>{label}</Badge>
}
