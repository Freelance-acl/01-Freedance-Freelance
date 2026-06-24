import axios from 'axios'
import { getToken } from './auth'
import type {
  AuthResponse, LoginRequest, RegisterRequest,
  User, Job, Proposal, Contract, Payout,
  AdminDashboard, TopFreelancer, FreelancerPayoutSummary,
  PlatformFeeAnalytics, PromoCode, ContractAnalytics,
} from './types'
import * as mock from './mock-data'

const MOCK = process.env.NEXT_PUBLIC_MOCK_MODE === 'true'

function delay<T>(data: T, ms = 350): Promise<T> {
  return new Promise((res) => setTimeout(() => res(data), ms))
}

// ─── Axios Client ─────────────────────────────────────────────────────────────

export const apiClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401 && typeof window !== 'undefined') {
      window.dispatchEvent(new Event('auth:expired'))
    }
    return Promise.reject(err)
  },
)

// ─── Auth ─────────────────────────────────────────────────────────────────────

export const authApi = {
  login: async (req: LoginRequest): Promise<AuthResponse> => {
    if (MOCK) {
      const user = mock.mockUsers.find(
        (u) => u.email === req.email && u.active,
      )
      if (!user) throw new Error('Invalid credentials')
      return delay({ token: 'mock.jwt.token', user })
    }
    const { data } = await apiClient.post<AuthResponse>('/users/auth/login', req)
    return data
  },

  register: async (req: RegisterRequest): Promise<AuthResponse> => {
    if (MOCK) {
      const user: User = {
        id: Date.now(), name: req.name, email: req.email,
        role: req.role, active: true, createdAt: new Date().toISOString(),
      }
      return delay({ token: 'mock.jwt.token', user })
    }
    const { data } = await apiClient.post<AuthResponse>('/users/auth/register', req)
    return data
  },
}

// ─── Users ────────────────────────────────────────────────────────────────────

export const usersApi = {
  getAll: async (): Promise<User[]> => {
    if (MOCK) return delay(mock.mockUsers)
    const { data } = await apiClient.get<User[]>('/users')
    return data
  },

  getById: async (id: number): Promise<User> => {
    if (MOCK) {
      const u = mock.mockUsers.find((u) => u.id === id)
      if (!u) throw new Error('User not found')
      return delay(u)
    }
    const { data } = await apiClient.get<User>(`/users/${id}`)
    return data
  },

  updateProfile: async (id: number, body: Partial<User>): Promise<User> => {
    if (MOCK) {
      const u = mock.mockUsers.find((u) => u.id === id)!
      return delay({ ...u, ...body })
    }
    const { data } = await apiClient.put<User>(`/users/${id}`, body)
    return data
  },

  deactivate: async (id: number): Promise<void> => {
    if (MOCK) return delay(undefined)
    await apiClient.post(`/users/${id}/deactivate`)
  },

  getTopFreelancers: async (limit = 5): Promise<TopFreelancer[]> => {
    if (MOCK) return delay(mock.mockTopFreelancers.slice(0, limit))
    const { data } = await apiClient.get<TopFreelancer[]>(`/users/top-freelancers?limit=${limit}`)
    return data
  },
}

// ─── Jobs ─────────────────────────────────────────────────────────────────────

export const jobsApi = {
  getAll: async (status?: string): Promise<Job[]> => {
    if (MOCK) {
      const jobs = status
        ? mock.mockJobs.filter((j) => j.status === status)
        : mock.mockJobs
      return delay(jobs)
    }
    const params = status ? `?status=${status}` : ''
    const { data } = await apiClient.get<Job[]>(`/jobs${params}`)
    return data
  },

  getById: async (id: number): Promise<Job> => {
    if (MOCK) {
      const j = mock.mockJobs.find((j) => j.id === id)
      if (!j) throw new Error('Job not found')
      return delay(j)
    }
    const { data } = await apiClient.get<Job>(`/jobs/${id}`)
    return data
  },

  create: async (body: Partial<Job>): Promise<Job> => {
    if (MOCK) {
      const job: Job = {
        id: Date.now(), title: body.title!, description: body.description!,
        budget: body.budget!, clientId: body.clientId!, category: body.category!,
        status: 'OPEN', createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
      return delay(job)
    }
    const { data } = await apiClient.post<Job>('/jobs', body)
    return data
  },

  update: async (id: number, body: Partial<Job>): Promise<Job> => {
    if (MOCK) {
      const j = mock.mockJobs.find((j) => j.id === id)!
      return delay({ ...j, ...body })
    }
    const { data } = await apiClient.put<Job>(`/jobs/${id}`, body)
    return data
  },

  close: async (id: number): Promise<void> => {
    if (MOCK) return delay(undefined)
    await apiClient.post(`/jobs/${id}/close`)
  },

  getByClient: async (clientId: number): Promise<Job[]> => {
    if (MOCK) return delay(mock.mockJobs.filter((j) => j.clientId === clientId))
    const { data } = await apiClient.get<Job[]>(`/jobs?clientId=${clientId}`)
    return data
  },

  search: async (keyword: string): Promise<Job[]> => {
    if (MOCK) {
      const kw = keyword.toLowerCase()
      return delay(mock.mockJobs.filter(
        (j) => j.title.toLowerCase().includes(kw) || j.description.toLowerCase().includes(kw),
      ))
    }
    const { data } = await apiClient.get<Job[]>(`/jobs/search?keyword=${encodeURIComponent(keyword)}`)
    return data
  },
}

// ─── Proposals ───────────────────────────────────────────────────────────────

export const proposalsApi = {
  getAll: async (): Promise<Proposal[]> => {
    if (MOCK) return delay(mock.mockProposals)
    const { data } = await apiClient.get<Proposal[]>('/proposals')
    return data
  },

  getById: async (id: number): Promise<Proposal> => {
    if (MOCK) {
      const p = mock.mockProposals.find((p) => p.id === id)
      if (!p) throw new Error('Proposal not found')
      return delay(p)
    }
    const { data } = await apiClient.get<Proposal>(`/proposals/${id}`)
    return data
  },

  getByJob: async (jobId: number): Promise<Proposal[]> => {
    if (MOCK) return delay(mock.mockProposals.filter((p) => p.jobId === jobId))
    const { data } = await apiClient.get<Proposal[]>(`/proposals?jobId=${jobId}`)
    return data
  },

  getByFreelancer: async (freelancerId: number): Promise<Proposal[]> => {
    if (MOCK) return delay(mock.mockProposals.filter((p) => p.freelancerId === freelancerId))
    const { data } = await apiClient.get<Proposal[]>(`/proposals?freelancerId=${freelancerId}`)
    return data
  },

  submit: async (body: Partial<Proposal>): Promise<Proposal> => {
    if (MOCK) {
      const p: Proposal = {
        id: Date.now(), jobId: body.jobId!, freelancerId: body.freelancerId!,
        bidAmount: body.bidAmount!, coverLetter: body.coverLetter!,
        estimatedDays: body.estimatedDays!, status: 'SUBMITTED',
        createdAt: new Date().toISOString(),
      }
      return delay(p)
    }
    const { data } = await apiClient.post<Proposal>('/proposals', body)
    return data
  },

  accept: async (id: number): Promise<Proposal> => {
    if (MOCK) {
      const p = mock.mockProposals.find((p) => p.id === id)!
      return delay({ ...p, status: 'ACCEPTED' })
    }
    const { data } = await apiClient.post<Proposal>(`/proposals/${id}/accept`)
    return data
  },

  reject: async (id: number): Promise<void> => {
    if (MOCK) return delay(undefined)
    await apiClient.post(`/proposals/${id}/reject`)
  },

  markCompleting: async (id: number): Promise<Proposal> => {
    if (MOCK) {
      const p = mock.mockProposals.find((p) => p.id === id)!
      return delay({ ...p, status: 'COMPLETING' })
    }
    const { data } = await apiClient.post<Proposal>(`/proposals/${id}/completing`)
    return data
  },

  withdraw: async (id: number): Promise<void> => {
    if (MOCK) return delay(undefined)
    await apiClient.post(`/proposals/${id}/withdraw`)
  },
}

// ─── Contracts ───────────────────────────────────────────────────────────────

export const contractsApi = {
  getAll: async (): Promise<Contract[]> => {
    if (MOCK) return delay(mock.mockContracts)
    const { data } = await apiClient.get<Contract[]>('/contracts')
    return data
  },

  getById: async (id: number): Promise<Contract> => {
    if (MOCK) {
      const c = mock.mockContracts.find((c) => c.id === id)
      if (!c) throw new Error('Contract not found')
      return delay(c)
    }
    const { data } = await apiClient.get<Contract>(`/contracts/${id}`)
    return data
  },

  getByFreelancer: async (freelancerId: number): Promise<Contract[]> => {
    if (MOCK) return delay(mock.mockContracts.filter((c) => c.freelancerId === freelancerId))
    const { data } = await apiClient.get<Contract[]>(`/contracts/freelancer/${freelancerId}`)
    return data
  },

  getByClient: async (clientId: number): Promise<Contract[]> => {
    if (MOCK) return delay(mock.mockContracts.filter((c) => c.clientId === clientId))
    const { data } = await apiClient.get<Contract[]>(`/contracts/client/${clientId}`)
    return data
  },

  terminate: async (id: number, reason: string): Promise<void> => {
    if (MOCK) return delay(undefined)
    await apiClient.post(`/contracts/${id}/terminate`, { reason })
  },

  getAnalytics: async (): Promise<ContractAnalytics> => {
    if (MOCK) return delay(mock.mockContractAnalytics)
    const { data } = await apiClient.get<ContractAnalytics>('/contracts/analytics')
    return data
  },

  getAdminDashboard: async (): Promise<AdminDashboard> => {
    if (MOCK) return delay(mock.mockAdminDashboard)
    const { data } = await apiClient.get<AdminDashboard>('/contracts/admin/dashboard')
    return data
  },
}

// ─── Wallet ───────────────────────────────────────────────────────────────────

export const walletApi = {
  getPayouts: async (): Promise<Payout[]> => {
    if (MOCK) return delay(mock.mockPayouts)
    const { data } = await apiClient.get<Payout[]>('/wallet/payouts')
    return data
  },

  getPayoutById: async (id: number): Promise<Payout> => {
    if (MOCK) {
      const p = mock.mockPayouts.find((p) => p.id === id)!
      return delay(p)
    }
    const { data } = await apiClient.get<Payout>(`/wallet/payouts/${id}`)
    return data
  },

  getFreelancerSummary: async (freelancerId: number): Promise<FreelancerPayoutSummary> => {
    if (MOCK) return delay({ ...mock.mockPayoutSummary, freelancerId })
    const { data } = await apiClient.get<FreelancerPayoutSummary>(
      `/wallet/freelancer/${freelancerId}/summary`,
    )
    return data
  },

  getPlatformFees: async (): Promise<PlatformFeeAnalytics[]> => {
    if (MOCK) return delay(mock.mockPlatformFees)
    const { data } = await apiClient.get<PlatformFeeAnalytics[]>('/wallet/admin/platform-fees')
    return data
  },

  retryPayout: async (payoutId: number): Promise<void> => {
    if (MOCK) return delay(undefined)
    await apiClient.post(`/wallet/payouts/${payoutId}/retry`)
  },

  getPromoCodes: async (): Promise<PromoCode[]> => {
    if (MOCK) return delay(mock.mockPromoCodes)
    const { data } = await apiClient.get<PromoCode[]>('/wallet/admin/promo-codes')
    return data
  },

  createPromoCode: async (body: Partial<PromoCode>): Promise<PromoCode> => {
    if (MOCK) {
      return delay({
        id: Date.now(), code: body.code!, discountPercent: body.discountPercent!,
        usageCount: 0, maxUsage: body.maxUsage!, expiresAt: body.expiresAt!,
        active: true,
      })
    }
    const { data } = await apiClient.post<PromoCode>('/wallet/admin/promo-codes', body)
    return data
  },
}
