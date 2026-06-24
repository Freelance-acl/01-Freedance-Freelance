import { Card, CardContent } from './ui/card'
import { cn } from '@/lib/utils'
import type { LucideIcon } from 'lucide-react'

interface MetricCardProps {
  label: string
  value: string | number
  icon?: LucideIcon
  trend?: { value: number; label: string }
  color?: 'brand' | 'emerald' | 'amber' | 'red'
  isLoading?: boolean
  className?: string
}

const colorMap = {
  brand:   'text-brand-600 bg-brand-50   dark:bg-brand-900/20   dark:text-brand-400',
  emerald: 'text-emerald-600 bg-emerald-50 dark:bg-emerald-900/20 dark:text-emerald-400',
  amber:   'text-amber-600   bg-amber-50   dark:bg-amber-900/20   dark:text-amber-400',
  red:     'text-red-600     bg-red-50     dark:bg-red-900/20     dark:text-red-400',
}

export function MetricCard({ label, value, icon: Icon, trend, color = 'brand', isLoading, className }: MetricCardProps) {
  const isUp = trend && trend.value >= 0

  if (isLoading) {
    return (
      <Card className={cn('hover:shadow-md transition-shadow', className)}>
        <CardContent className="p-6">
          <div className="flex items-start justify-between">
            <div className="flex flex-col gap-2 flex-1">
              <div className="h-4 w-24 animate-pulse rounded-md bg-gray-200 dark:bg-gray-700" />
              <div className="h-7 w-16 animate-pulse rounded-md bg-gray-200 dark:bg-gray-700" />
              <div className="h-3 w-20 animate-pulse rounded-md bg-gray-100 dark:bg-gray-800" />
            </div>
            <div className="h-10 w-10 animate-pulse rounded-lg bg-gray-200 dark:bg-gray-700" />
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className={cn('hover:shadow-md transition-shadow', className)}>
      <CardContent className="p-6">
        <div className="flex items-start justify-between">
          <div className="flex flex-col gap-1">
            <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
            <p className="text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
            {trend && (
              <p className={cn('text-xs font-medium', isUp ? 'text-emerald-600' : 'text-red-500')}>
                {isUp ? '+' : ''}{trend.value}% {trend.label}
              </p>
            )}
          </div>
          {Icon && (
            <div className={cn('rounded-lg p-2.5', colorMap[color])}>
              <Icon className="h-5 w-5" />
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
