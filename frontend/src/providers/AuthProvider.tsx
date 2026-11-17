'use client'

import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import type { AuthUser } from '@/lib/types'
import { getStoredUser, getToken, saveToken, clearAuth, isTokenExpired } from '@/lib/auth'
import { authApi } from '@/lib/api'
import type { LoginRequest, RegisterRequest } from '@/lib/types'

interface AuthCtx {
  user: AuthUser | null
  isLoading: boolean
  login: (_req: LoginRequest) => Promise<void>
  register: (_req: RegisterRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthCtx | null>(null)

export function useAuthContext(): AuthCtx {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuthContext must be inside AuthProvider')
  return ctx
}

export default function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser]           = useState<AuthUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const router                    = useRouter()
  const queryClient = useQueryClient()

  useEffect(() => {
    const token = getToken()
    if (token && !isTokenExpired(token) && getStoredUser()?.id) {
      setUser(getStoredUser())
    } else {
      clearAuth()
    }
    setIsLoading(false)

    const onExpired = () => { clearAuth(); setUser(null); router.push('/login') }
    window.addEventListener('auth:expired', onExpired)
    return () => window.removeEventListener('auth:expired', onExpired)
  }, [router, queryClient])

  const login = useCallback(async (req: LoginRequest) => {
    const res = await authApi.login(req)
    queryClient.clear()
    saveToken(res.token, res.user)
    setUser(res.user)
    router.push('/dashboard')
  }, [router, queryClient])

  const register = useCallback(async (req: RegisterRequest) => {
    const res = await authApi.register(req)
    queryClient.clear()
    saveToken(res.token, res.user)
    setUser(res.user)
    router.push('/dashboard')
  }, [router, queryClient])

  const logout = useCallback(() => {
    clearAuth()
    queryClient.clear()
    setUser(null)
    router.push('/login')
  }, [router, queryClient])

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
