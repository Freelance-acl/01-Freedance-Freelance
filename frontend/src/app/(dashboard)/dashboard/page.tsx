'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthContext } from '@/providers/AuthProvider'
import { LoadingState } from '@/components/LoadingState'

export default function DashboardRedirect() {
  const { user, isLoading } = useAuthContext()
  const router = useRouter()

  useEffect(() => {
    if (isLoading || !user) return
    const paths: Record<string, string> = {
      ADMIN:      '/dashboard/admin',
      CLIENT:     '/dashboard/client',
      FREELANCER: '/dashboard/freelancer',
    }
    router.replace(paths[user.role] ?? '/dashboard/client')
  }, [user, isLoading, router])

  return <LoadingState message="Redirecting to your dashboard..." />
}
