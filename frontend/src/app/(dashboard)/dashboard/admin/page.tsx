'use client'

import { useQuery } from '@tanstack/react-query'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { MetricCard } from '@/components/MetricCard'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { contractsApi } from '@/lib/api'
import { walletApi } from '@/lib/api'
import { formatCurrency } from '@/lib/utils'
import { mockRevenueChart, mockTopFreelancers } from '@/lib/mock-data'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts'
import { Users, Briefcase, FileSignature, DollarSign } from 'lucide-react'
import { mockProposalStatusData } from '@/lib/mock-data'

export default function AdminDashboardPage() {
  const { data: dashboard, isLoading } = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn:  contractsApi.getAdminDashboard,
  })
  const { data: fees } = useQuery({
    queryKey: ['platform-fees'],
    queryFn:  walletApi.getPlatformFees,
  })

  return (
    <DashboardLayout title="Admin Overview">
      <div className="space-y-6">
        {/* Metrics row */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <MetricCard isLoading={isLoading} label="Total Users"        value={dashboard?.totalUsers ?? 0}                     icon={Users}         color="brand"   trend={{ value: 12, label: 'vs last month' }} />
          <MetricCard isLoading={isLoading} label="Total Jobs"         value={dashboard?.totalJobs ?? 0}                      icon={Briefcase}     color="emerald" trend={{ value: 8,  label: 'vs last month' }} />
          <MetricCard isLoading={isLoading} label="Active Contracts"   value={dashboard?.activeContracts ?? 0}                icon={FileSignature} color="amber" />
          <MetricCard isLoading={isLoading} label="Platform Revenue"   value={formatCurrency(dashboard?.totalRevenue ?? 0)}   icon={DollarSign}    color="emerald" trend={{ value: 22, label: 'vs last month' }} />
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          {/* Revenue Chart */}
          <Card>
            <CardHeader><CardTitle>Monthly Revenue</CardTitle></CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={mockRevenueChart}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                  <XAxis dataKey="month" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} tickFormatter={(v) => `$${(v / 1000).toFixed(0)}k`} />
                  <Tooltip formatter={(v: number) => formatCurrency(v)} />
                  <Bar dataKey="revenue" fill="#6366f1" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          {/* Proposal status pie */}
          <Card>
            <CardHeader><CardTitle>Proposal Status Breakdown</CardTitle></CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie data={mockProposalStatusData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label>
                    {mockProposalStatusData.map((entry, i) => (
                      <Cell key={i} fill={entry.fill} />
                    ))}
                  </Pie>
                  <Legend />
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </div>

        {/* Top Freelancers */}
        <Card>
          <CardHeader><CardTitle>Top Freelancers</CardTitle></CardHeader>
          <CardContent className="p-0">
            <table className="w-full text-sm">
              <thead className="border-b border-gray-100 bg-gray-50 dark:border-gray-700 dark:bg-gray-800/50">
                <tr>
                  {['Freelancer', 'Contracts', 'Completion', 'Total Earned'].map((h) => (
                    <th key={h} className="px-6 py-3 text-left text-xs font-semibold uppercase text-gray-500">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                {mockTopFreelancers.map((f) => (
                  <tr key={f.freelancerId} className="hover:bg-gray-50 dark:hover:bg-gray-800/30">
                    <td className="px-6 py-4 font-medium text-gray-900 dark:text-white">{f.name}</td>
                    <td className="px-6 py-4 text-gray-500">{f.completedContracts}</td>
                    <td className="px-6 py-4 text-gray-500">{(f.completionRate * 100).toFixed(0)}%</td>
                    <td className="px-6 py-4 font-medium text-emerald-600">{formatCurrency(f.totalEarnings)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>

        {/* Fee breakdown */}
        {fees && (
          <Card>
            <CardHeader><CardTitle>Platform Fees by Category</CardTitle></CardHeader>
            <CardContent className="p-0">
              <table className="w-full text-sm">
                <thead className="border-b border-gray-100 bg-gray-50 dark:border-gray-700 dark:bg-gray-800/50">
                  <tr>
                    {['Category', 'Payouts', 'Total Volume', 'Fees Collected', 'Avg Rate'].map((h) => (
                      <th key={h} className="px-6 py-3 text-left text-xs font-semibold uppercase text-gray-500">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                  {fees.map((f) => (
                    <tr key={f.category} className="hover:bg-gray-50 dark:hover:bg-gray-800/30">
                      <td className="px-6 py-4 font-medium text-gray-900 dark:text-white">{f.category}</td>
                      <td className="px-6 py-4 text-gray-500">{f.payoutCount}</td>
                      <td className="px-6 py-4 text-gray-500">{formatCurrency(f.totalVolume)}</td>
                      <td className="px-6 py-4 font-medium text-brand-600">{formatCurrency(f.totalFees)}</td>
                      <td className="px-6 py-4 text-gray-500">{(f.averageFeeRate * 100).toFixed(0)}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </CardContent>
          </Card>
        )}
      </div>
    </DashboardLayout>
  )
}
