'use client'

import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import type { AuthUser } from '@/lib/types'
import { getStoredUser, getToken, saveToken, clearAuth, isTokenExpired } from '@/lib/auth'
import { authApi } from '@/lib/api'
import type { LoginRequest, RegisterRequest } from '@/lib/types'

interface AuthCtx {
  user: AuthUser | null
  isLoading: boolean
  login: (req: LoginRequest) => Promise<void>
  register: (req: RegisterRequest) => Promise<void>
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

  useEffect(() => {
    const token = getToken()
    if (token && !isTokenExpired(token)) {
      setUser(getStoredUser())
    } else {
      clearAuth()
    }
    setIsLoading(false)

    const onExpired = () => { clearAuth(); setUser(null); router.push('/login') }
    window.addEventListener('auth:expired', onExpired)
    return () => window.removeEventListener('auth:expired', onExpired)
  }, [router])

  const login = useCallback(async (req: LoginRequest) => {
    const res = await authApi.login(req)
    saveToken(res.token, res.user)
    setUser(res.user)
    router.push('/dashboard')
  }, [router])

  const register = useCallback(async (req: RegisterRequest) => {
    const res = await authApi.register(req)
    saveToken(res.token, res.user)
    setUser(res.user)
    router.push('/dashboard')
  }, [router])

  const logout = useCallback(() => {
    clearAuth()
    setUser(null)
    router.push('/login')
  }, [router])

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
