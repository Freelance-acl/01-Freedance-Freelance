// ─── Auth ────────────────────────────────────────────────────────────────────

export type UserRole = 'ADMIN' | 'CLIENT' | 'FREELANCER'

export interface AuthUser {
  id: number
  name: string
  email: string
  role: UserRole
}

export interface LoginRequest  { email: string; password: string }
export interface RegisterRequest { name: string; email: string; password: string; role: UserRole }
export interface AuthResponse  { token: string; user: AuthUser }

// ─── User ────────────────────────────────────────────────────────────────────

export interface User {
  id: number
  name: string
  email: string
  role: UserRole
  bio?: string
  skills?: string[]
  rating?: number
  active: boolean
  preferences?: Record<string, unknown>
  createdAt: string
}

export interface UserContractSummary {
  userId: number
  name: string | null
  totalContracts: number
  completedContracts: number
  terminatedContracts: number
  totalEarnings: number
  averageContractValue: number
}

export interface TopFreelancer {
  freelancerId: number
  name: string
  totalEarnings: number
  completedContracts: number
  completionRate: number
  rating: number
}

// ─── Job ─────────────────────────────────────────────────────────────────────

export type JobStatus = 'OPEN' | 'IN_PROGRESS' | 'CLOSED' | 'CANCELLED'

export interface Job {
  id: number
  title: string
  description: string
  budget: number
  status: JobStatus
  clientId: number
  category: string
  requirements?: string
  createdAt: string
  updatedAt: string
  rating?: number
}

export interface JobAttachment {
  id: number
  jobId: number
  fileName: string
  fileUrl: string
  verified: boolean
  expiresAt: string
}

export interface JobProposalSummary {
  jobId: number
  jobTitle: string
  totalProposals: number
  acceptedProposals: number
  averageBid: number
  minBid: number
  maxBid: number
}

export interface JobDashboard {
  jobId: number
  title: string
  status: JobStatus
  totalProposals: number
  acceptedProposals: number
  averageBid: number
  rating: number
  activeAttachments: number
}

// ─── Proposal ────────────────────────────────────────────────────────────────

export type ProposalStatus =
  | 'SUBMITTED'
  | 'SHORTLISTED'
  | 'ACCEPTED'
  | 'COMPLETING'
  | 'PAYMENT_PENDING'
  | 'PAID'
  | 'PAYMENT_FAILED'
  | 'REFUNDED'
  | 'WITHDRAWN'
  | 'REJECTED'

export interface Proposal {
  id: number
  jobId: number
  freelancerId: number
  bidAmount: number
  status: ProposalStatus
  coverLetter: string
  estimatedDays: number
  milestones?: ProposalMilestone[]
  createdAt: string
  updatedAt?: string
}

export interface ProposalMilestone {
  id: number
  proposalId: number
  title: string
  description: string
  amount: number
  dueDate: string
  status: 'PENDING' | 'COMPLETED'
}

export interface ProposalAnalytics {
  jobId: number
  timePeriod: string
  totalProposals: number
  statusBreakdown: Record<ProposalStatus, number>
  averageBid: number
}

// ─── Contract ────────────────────────────────────────────────────────────────

export type ContractStatus = 'ACTIVE' | 'COMPLETED' | 'TERMINATED'
export type MilestoneStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'APPROVED'

export interface Contract {
  id: number
  jobId: number
  freelancerId: number
  clientId: number
  proposalId: number
  agreedAmount: number
  status: ContractStatus
  startDate: string
  endDate: string | null
  metadata: Record<string, unknown> | null
  createdAt: string
}

export interface ContractMilestone {
  timestamp: string
  milestoneOrder: number
  status: MilestoneStatus
  recordedBy: string | null
  notes: string | null
}

export interface ContractAnalytics {
  totalContracts: number
  avgValue: number
  completionRate: number
  avgDurationDays: number
  byStatus: Record<string, number>
}

export interface FreelancerPerformance {
  freelancerId: number
  totalContracts: number
  averageContractValue: number
  completionRate: number
  averageDurationDays: number
  totalEarnings: number
}

export interface StalledContract {
  contractId: number
  freelancerName: string | null
  jobTitle: string | null
  agreedAmount: number
  progressPercentage: number | null
  daysSinceLastActivity: number
}

// ─── Wallet / Payout ─────────────────────────────────────────────────────────

export type PayoutStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED'

export interface Payout {
  id: number
  contractId: number
  freelancerId: number
  amount: number
  status: PayoutStatus
  platformFee: number
  transactionDetails?: Record<string, unknown>
  createdAt: string
  processedAt?: string
}

export interface FreelancerPayoutSummary {
  freelancerId: number
  totalPaid: number
  totalPending: number
  totalFailed: number
  totalRefunded: number
  totalPlatformFees: number
  payoutCount: number
}

export interface PlatformFeeAnalytics {
  category: string
  totalFees: number
  totalVolume: number
  payoutCount: number
  averageFeeRate: number
}

export interface PromoCode {
  id: number
  code: string
  discountPercent: number
  usageCount: number
  maxUsage: number
  expiresAt: string
  active: boolean
}

// ─── Saga ────────────────────────────────────────────────────────────────────

export type SagaEventType =
  | 'PROPOSAL_SUBMITTED'
  | 'PROPOSAL_ACCEPTED'
  | 'CONTRACT_CREATED'
  | 'PROPOSAL_COMPLETING'
  | 'PAYMENT_INITIATED'
  | 'PAYMENT_COMPLETED'
  | 'PAYMENT_FAILED'
  | 'CONTRACT_TERMINATED'
  | 'REFUND_ISSUED'

export interface SagaStep {
  id: string
  event: SagaEventType
  label: string
  description: string
  timestamp: string
  status: 'completed' | 'active' | 'failed' | 'pending'
  metadata?: Record<string, unknown>
}

// ─── Dashboard ───────────────────────────────────────────────────────────────

export interface AdminDashboard {
  totalUsers: number
  totalJobs: number
  totalContracts: number
  totalRevenue: number
  activeContracts: number
  openJobs: number
  pendingPayouts: number
  platformFeeCollected: number
}

export interface ClientDashboard {
  totalJobs: number
  openJobs: number
  totalProposals: number
  activeContracts: number
  totalSpent: number
}

export interface FreelancerDashboard {
  activeProposals: number
  acceptedProposals: number
  activeContracts: number
  totalEarnings: number
  completionRate: number
}

// ─── API Wrappers ─────────────────────────────────────────────────────────────

export interface PagedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface ApiError {
  status: number
  message: string
  timestamp: string
}
