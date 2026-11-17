import type { Job, User, Proposal, Payout, PromoCode } from './types'

export type BackendUser = Omit<User, 'active'> & { status: string; phone: string }
export type BackendJob = Omit<Job, 'budget' | 'requirements'> & {
  budgetMin: number; budgetMax: number; requirements?: Record<string, unknown>
}
export type BackendProposal = Proposal & { submittedAt: string; acceptedAt?: string; contractId?: number }
export type BackendPayout = Omit<Payout, 'platformFee'>
export type BackendPromo = PromoCode & {
  discountValue: number; discountType: string; currentUses: number; maxUses: number; expiryDate: string
}

export const normalizeUser = (u: BackendUser): User => ({ ...u, active: u.status === 'ACTIVE', bio: u.preferences?.bio as string | undefined })
export const normalizeJob = (j: BackendJob): Job => ({
  ...j, budget: j.budgetMax,
  requirements: j.requirements ? String(j.requirements.description ?? JSON.stringify(j.requirements)) : undefined,
})
export const normalizeProposal = (p: BackendProposal): Proposal => ({ ...p, createdAt: p.submittedAt })
export const normalizePayout = (p: BackendPayout): Payout => ({
  ...p, platformFee: Number(p.transactionDetails?.platformFee ?? 0),
  processedAt: p.transactionDetails?.processedAt as string | undefined,
})
export const normalizePromo = (p: BackendPromo): PromoCode => ({
  ...p, discountPercent: p.discountValue, discountType: p.discountType,
  usageCount: p.currentUses, maxUsage: p.maxUses, expiresAt: p.expiryDate,
})

export function jobPayload(body: Partial<Job>) {
  const { budget, requirements, ...fields } = body
  return {
    ...fields, budgetMin: budget, budgetMax: budget,
    ...(requirements ? { requirements: { description: requirements } } : {}),
  }
}
