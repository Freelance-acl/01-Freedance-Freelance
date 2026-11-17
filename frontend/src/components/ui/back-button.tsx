'use client'

import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft } from 'lucide-react'
import { cn } from '@/lib/utils'

interface BackButtonProps {
  href?: string
  label?: string
  className?: string
}

export function BackButton({ href, label = 'Back', className }: BackButtonProps) {
  const router = useRouter()

  const base = cn(
    'inline-flex items-center gap-1.5 rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm font-medium text-gray-600',
    'hover:border-gray-300 hover:bg-gray-50 hover:text-gray-900',
    'dark:border-gray-700 dark:bg-gray-800/60 dark:text-gray-400 dark:hover:border-gray-600 dark:hover:bg-gray-800 dark:hover:text-white',
    'transition-all shadow-sm',
    className,
  )

  if (href) {
    return (
      <Link href={href} className={base}>
        <ArrowLeft className="h-3.5 w-3.5" />
        {label}
      </Link>
    )
  }

  return (
    <button onClick={() => router.back()} className={base}>
      <ArrowLeft className="h-3.5 w-3.5" />
      {label}
    </button>
  )
}
