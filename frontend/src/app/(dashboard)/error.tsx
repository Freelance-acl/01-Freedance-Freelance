'use client'

import { useEffect } from 'react'
import { Button } from '@/components/ui/button'

export default function DashboardError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    console.error(error)
  }, [error])

  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center px-4 text-center">
      <div className="rounded-full bg-red-100 p-4 dark:bg-red-900/20">
        <svg className="h-8 w-8 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
            d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
        </svg>
      </div>
      <h2 className="mt-4 text-xl font-bold text-gray-900 dark:text-white">Something went wrong</h2>
      <p className="mt-2 max-w-sm text-sm text-gray-500 dark:text-gray-400">
        An unexpected error occurred. You can try again or return to the dashboard.
      </p>
      {error.digest && (
        <p className="mt-1 font-mono text-xs text-gray-400">Error ID: {error.digest}</p>
      )}
      <div className="mt-6 flex gap-3">
        <Button onClick={reset}>Try Again</Button>
        <Button variant="outline" onClick={() => window.location.assign('/dashboard')}>
          Back to Dashboard
        </Button>
      </div>
    </div>
  )
}
