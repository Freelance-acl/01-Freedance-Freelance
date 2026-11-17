import { cn, formatDate } from '@/lib/utils'
import type { SagaStep } from '@/lib/types'
import { CheckCircle, Circle, AlertCircle, Clock } from 'lucide-react'

const iconMap = {
  completed: { Icon: CheckCircle, color: 'text-emerald-500' },
  active:    { Icon: Clock,        color: 'text-brand-500 animate-pulse' },
  failed:    { Icon: AlertCircle,  color: 'text-red-500' },
  pending:   { Icon: Circle,       color: 'text-gray-300 dark:text-gray-600' },
}

export function SagaTimeline({ steps }: { steps: SagaStep[] }) {
  return (
    <ol className="relative space-y-0">
      {steps.map((step, i) => {
        const { Icon, color } = iconMap[step.status]
        const isLast = i === steps.length - 1
        return (
          <li key={step.id} className="flex gap-4">
            <div className="flex flex-col items-center">
              <Icon className={cn('h-6 w-6 shrink-0', color)} />
              {!isLast && (
                <div className={cn(
                  'mt-1 w-0.5 flex-1',
                  step.status === 'completed' ? 'bg-emerald-300 dark:bg-emerald-700' : 'bg-gray-200 dark:bg-gray-700',
                )} />
              )}
            </div>
            <div className={cn('pb-8', isLast && 'pb-0')}>
              <p className={cn(
                'text-sm font-semibold',
                step.status === 'pending' ? 'text-gray-400' : 'text-gray-900 dark:text-white',
              )}>
                {step.label}
              </p>
              <p className="mt-0.5 text-xs text-gray-500">{step.description}</p>
              {step.timestamp && (
                <p className="mt-1 text-xs text-gray-400">{formatDate(step.timestamp)}</p>
              )}
            </div>
          </li>
        )
      })}
    </ol>
  )
}
