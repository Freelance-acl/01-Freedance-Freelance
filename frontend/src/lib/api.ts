import axios from 'axios'
import { getToken } from './auth'
import { jwtDecode } from 'jwt-decode'
import { normalizeUser, normalizeJob, normalizeProposal, normalizePayout, normalizePromo, jobPayload } from './backend'
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
  if (token && !config.headers.Authorization) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (r) => {
    // Adapt the backend wire models once, including list and detail responses.
    const path = r.config.url?.split('?')[0] ?? ''
    const adapt = <T,>(fn: (_value: T) => unknown) => {
      if (Array.isArray(r.data)) r.data = r.data.map(value => fn(value as T))
      else if (r.data && typeof r.data === 'object' && 'id' in r.data) r.data = fn(r.data as T)
    }
    if (/^\/users(?:\/\d+)?$/.test(path)) adapt(normalizeUser)
    if (/^\/jobs(?:\/\d+)?$/.test(path)) adapt(normalizeJob)
    if (/^\/proposals(?:\/\d+(?:\/(?:accept|complete|withdraw))?)?$/.test(path)) adapt(normalizeProposal)
    if (/^\/payouts(?:\/\d+(?:\/retry)?|\/contract\/\d+)?$/.test(path)) adapt(normalizePayout)
    if (/^\/promo-codes(?:\/\d+)?$/.test(path)) adapt(normalizePromo)
    return r
  },
  (err) => {
    if (err.response?.status === 401 && typeof window !== 'undefined') {
      window.dispatchEvent(new Event('auth:expired'))
    }
    return Promise.reject(err)
  },
)

// ─── Auth ─────────────────────────────────────────────────────────────────────

async function authenticate(path: string, body: LoginRequest | RegisterRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<{ token: string; expiresIn: number }>(path, body)
  const { uid } = jwtDecode<{ uid: number }>(data.token)
  const profile = await apiClient.get<User>(`/users/${uid}`, {
    headers: { Authorization: `Bearer ${data.token}` },
  })
  return { token: data.token, user: profile.data }
}

export const authApi = {
  login: async (req: LoginRequest): Promise<AuthResponse> => {
    if (MOCK) {
      const user = mock.mockUsers.find(
        (u) => u.email === req.email && u.active,
      )
      if (!user) throw new Error('Invalid credentials')
      return delay({ token: 'mock.jwt.token', user })
    }
    return authenticate('/auth/login', req)
  },

  register: async (req: RegisterRequest): Promise<AuthResponse> => {
    if (MOCK) {
      const user: User = {
        id: Date.now(), name: req.name, email: req.email,
        role: req.role, active: true, createdAt: new Date().toISOString(),
      }
      return delay({ token: 'mock.jwt.token', user })
    }
    return authenticate('/auth/register', req)
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
    const { bio, ...fields } = body
    const current = bio === undefined ? undefined : await usersApi.getById(id)
    const { data } = await apiClient.put<User>(`/users/${id}`, { ...fields,
      ...(bio !== undefined ? { preferences: { ...current?.preferences, bio } } : {}) })
    return data
  },

  deactivate: async (id: number): Promise<void> => {
    if (MOCK) return delay(undefined)
    await apiClient.put(`/users/${id}/deactivate`)
  },

  getTopFreelancers: async (limit = 5): Promise<TopFreelancer[]> => {
    if (MOCK) return delay(mock.mockTopFreelancers.slice(0, limit))
    const [users, contracts, payouts] = await Promise.all([usersApi.getAll(), contractsApi.getAll(), walletApi.getPayouts()])
    return users.filter(u => u.role === 'FREELANCER').map(u => {
      const own = contracts.filter(c => c.freelancerId === u.id)
      const completed = own.filter(c => c.status === 'COMPLETED').length
      return { freelancerId: u.id, name: u.name, completedContracts: completed, completionRate: own.length ? completed / own.length : 0,
        totalEarnings: payouts.filter(p => p.freelancerId === u.id && p.status === 'COMPLETED').reduce((sum, p) => sum + p.amount - p.platformFee, 0), rating: u.rating ?? 0 }
    }).sort((a, b) => b.totalEarnings - a.totalEarnings).slice(0, limit)
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
    const { data } = await apiClient.get<Job[]>('/jobs')
    return status ? data.filter(j => j.status === status) : data
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
    const { data } = await apiClient.post<Job>('/jobs', jobPayload(body))
    return data
  },

  update: async (id: number, body: Partial<Job>): Promise<Job> => {
    if (MOCK) {
      const j = mock.mockJobs.find((j) => j.id === id)!
      return delay({ ...j, ...body })
    }
    const { data } = await apiClient.put<Job>(`/jobs/${id}`, jobPayload(body))
    return data
  },

  close: async (id: number): Promise<void> => {
    if (MOCK) return delay(undefined)
    await apiClient.put(`/jobs/${id}/close`, { status: 'CLOSED' })
  },

  getByClient: async (clientId: number): Promise<Job[]> => {
    if (MOCK) return delay(mock.mockJobs.filter((j) => j.clientId === clientId))
    const { data } = await apiClient.get<Job[]>('/jobs')
    return data.filter(j => j.clientId === clientId)
  },

  search: async (keyword: string): Promise<Job[]> => {
    if (MOCK) {
      const kw = keyword.toLowerCase()
      return delay(mock.mockJobs.filter(
        (j) => j.title.toLowerCase().includes(kw) || j.description.toLowerCase().includes(kw),
      ))
    }
    const { data } = await apiClient.get<{ content: { id: number }[] }>(`/jobs/search/full-text?query=${encodeURIComponent(keyword)}&size=100`)
    return Promise.all(data.content.map(j => jobsApi.getById(j.id)))
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
    const { data } = await apiClient.get<Proposal[]>('/proposals')
    return data.filter(p => p.jobId === jobId)
  },

  getByFreelancer: async (freelancerId: number): Promise<Proposal[]> => {
    if (MOCK) return delay(mock.mockProposals.filter((p) => p.freelancerId === freelancerId))
    const { data } = await apiClient.get<Proposal[]>('/proposals')
    return data.filter(p => p.freelancerId === freelancerId)
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
    const { data } = await apiClient.put<Proposal>(`/proposals/${id}/accept`)
    return data
  },

  reject: async (id: number): Promise<void> => {
    if (MOCK) return delay(undefined)
    await apiClient.put(`/proposals/${id}`, { status: 'REJECTED' })
  },

  markCompleting: async (id: number): Promise<Proposal> => {
    if (MOCK) {
      const p = mock.mockProposals.find((p) => p.id === id)!
      return delay({ ...p, status: 'COMPLETING' })
    }
    const { data } = await apiClient.put<Proposal>(`/proposals/${id}/complete`)
    return data
  },

  withdraw: async (id: number): Promise<void> => {
    if (MOCK) return delay(undefined)
    await apiClient.put(`/proposals/${id}/withdraw`)
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
    const { data } = await apiClient.get<Contract[]>('/contracts')
    return data.filter(c => c.freelancerId === freelancerId)
  },

  getByClient: async (clientId: number): Promise<Contract[]> => {
    if (MOCK) return delay(mock.mockContracts.filter((c) => c.clientId === clientId))
    const { data } = await apiClient.get<Contract[]>('/contracts')
    return data.filter(c => c.clientId === clientId)
  },

  terminate: async (id: number, reason: string): Promise<void> => {
    if (MOCK) return delay(undefined)
    const contract = await contractsApi.getById(id)
    await apiClient.put(`/contracts/${id}`, { status: 'TERMINATED', metadata: { ...contract.metadata, terminationReason: reason } })
  },

  getAnalytics: async (): Promise<ContractAnalytics> => {
    if (MOCK) return delay(mock.mockContractAnalytics)
    const { data } = await apiClient.get<ContractAnalytics>('/contracts/analytics')
    return data
  },

  getAdminDashboard: async (): Promise<AdminDashboard> => {
    if (MOCK) return delay(mock.mockAdminDashboard)
    const [users, jobs, contracts, payouts] = await Promise.all([
      usersApi.getAll(), jobsApi.getAll(), contractsApi.getAll(), walletApi.getPayouts(),
    ])
    const fees = payouts.filter(p => p.status === 'COMPLETED').reduce((sum, p) => sum + p.platformFee, 0)
    return { totalUsers: users.length, totalJobs: jobs.length, totalContracts: contracts.length,
      totalRevenue: fees, platformFeeCollected: fees, activeContracts: contracts.filter(c => c.status === 'ACTIVE').length,
      openJobs: jobs.filter(j => j.status === 'OPEN').length, pendingPayouts: payouts.filter(p => p.status === 'PENDING').length }
  },
}

// ─── Wallet ───────────────────────────────────────────────────────────────────

export const walletApi = {
  getPayouts: async (): Promise<Payout[]> => {
    if (MOCK) return delay(mock.mockPayouts)
    const { data } = await apiClient.get<Payout[]>('/payouts')
    return data
  },

  getPayoutById: async (id: number): Promise<Payout> => {
    if (MOCK) {
      const p = mock.mockPayouts.find((p) => p.id === id)!
      return delay(p)
    }
    const { data } = await apiClient.get<Payout>(`/payouts/${id}`)
    return data
  },

  getFreelancerSummary: async (freelancerId: number): Promise<FreelancerPayoutSummary> => {
    if (MOCK) return delay({ ...mock.mockPayoutSummary, freelancerId })
    const payouts = (await walletApi.getPayouts()).filter(p => p.freelancerId === freelancerId)
    const total = (status: Payout['status']) => payouts.filter(p => p.status === status).reduce((sum, p) => sum + p.amount, 0)
    return { freelancerId, totalPaid: total('COMPLETED') - payouts.filter(p => p.status === 'COMPLETED').reduce((sum, p) => sum + p.platformFee, 0), totalPending: total('PENDING'), totalFailed: total('FAILED'),
      totalRefunded: total('REFUNDED'), payoutCount: payouts.length,
      totalPlatformFees: payouts.filter(p => p.status === 'COMPLETED').reduce((sum, p) => sum + p.platformFee, 0) }
  },

  getPlatformFees: async (): Promise<PlatformFeeAnalytics[]> => {
    if (MOCK) return delay(mock.mockPlatformFees)
    const { data } = await apiClient.get<{ category: string; platformFeeRevenue: number; totalRevenue: number; payoutCount: number }[]>(
      `/payouts/analytics/category?startDate=2000-01-01&endDate=${new Date().toISOString().slice(0, 10)}`)
    return data.map(f => ({ category: f.category, totalFees: f.platformFeeRevenue, totalVolume: f.totalRevenue,
      payoutCount: f.payoutCount, averageFeeRate: f.totalRevenue ? f.platformFeeRevenue / f.totalRevenue : 0 }))
  },

  retryPayout: async (payoutId: number): Promise<void> => {
    if (MOCK) return delay(undefined)
    await apiClient.put(`/payouts/${payoutId}/retry`)
  },

  processContract: async (contractId: number): Promise<Payout> => {
    const { data } = await apiClient.post<Payout>(`/payouts/contract/${contractId}`, { method: 'BANK_TRANSFER', accountLastFour: '0000' })
    return data
  },

  getPromoCodes: async (): Promise<PromoCode[]> => {
    if (MOCK) return delay(mock.mockPromoCodes)
    const { data } = await apiClient.get<PromoCode[]>('/promo-codes')
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
    const { data } = await apiClient.post<PromoCode>('/promo-codes', { code: body.code, discountType: 'PERCENTAGE', discountValue: body.discountPercent, maxUses: body.maxUsage, expiryDate: body.expiresAt, active: body.active ?? true })
    return data
  },
}
