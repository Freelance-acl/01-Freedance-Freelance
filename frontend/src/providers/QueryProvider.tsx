'use client'

import { QueryClient, QueryClientProvider, QueryCache, MutationCache } from '@tanstack/react-query'
import { useState } from 'react'
import { toast } from 'sonner'

export default function QueryProvider({ children }: { children: React.ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        queryCache: new QueryCache({ onError: () => toast.error('Could not load backend data. Please retry.') }),
        mutationCache: new MutationCache({ onError: () => toast.error('The backend could not complete this action. Please retry.') }),
        defaultOptions: {
          queries: { staleTime: 0, retry: 1, refetchInterval: 5_000 },
        },
      }),
  )
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}
