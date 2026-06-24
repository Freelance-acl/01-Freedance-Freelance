import { jwtDecode } from 'jwt-decode'
import type { AuthUser, UserRole } from './types'

const TOKEN_KEY = 'freelance_token'
const USER_KEY  = 'freelance_user'

interface JwtPayload {
  sub: string
  id: number
  name: string
  email: string
  role: UserRole
  exp: number
}

export function saveToken(token: string, user: AuthUser): void {
  if (typeof window === 'undefined') return
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function getToken(): string | null {
  if (typeof window === 'undefined') return null
  return localStorage.getItem(TOKEN_KEY)
}

export function getStoredUser(): AuthUser | null {
  if (typeof window === 'undefined') return null
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try { return JSON.parse(raw) as AuthUser }
  catch { return null }
}

export function clearAuth(): void {
  if (typeof window === 'undefined') return
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function decodeToken(token: string): AuthUser | null {
  try {
    const payload = jwtDecode<JwtPayload>(token)
    if (payload.exp * 1000 < Date.now()) return null
    return {
      id:    payload.id,
      name:  payload.name,
      email: payload.email,
      role:  payload.role,
    }
  } catch {
    return null
  }
}

export function isTokenExpired(token: string): boolean {
  try {
    const { exp } = jwtDecode<{ exp: number }>(token)
    return exp * 1000 < Date.now()
  } catch {
    return true
  }
}

export function isAuthenticated(): boolean {
  const token = getToken()
  if (!token) return false
  return !isTokenExpired(token)
}
