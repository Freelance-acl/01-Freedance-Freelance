'use client'

import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { useAuthContext } from '@/providers/AuthProvider'
import { jobsApi } from '@/lib/api'

const schema = z.object({
  title:       z.string().min(5, 'Title must be at least 5 characters'),
  description: z.string().min(20, 'Description must be at least 20 characters'),
  budget:      z.coerce.number().positive('Budget must be positive'),
  category:    z.string().min(1, 'Select a category'),
  requirements:z.string().optional(),
})
type FormData = z.infer<typeof schema>

const CATEGORIES = [
  'Frontend', 'Backend', 'Mobile', 'Design', 'DevOps', 'Data Science', 'QA', 'Other',
].map((c) => ({ value: c, label: c }))

export default function CreateJobPage() {
  const { user }  = useAuthContext()
  const router    = useRouter()
  const qc        = useQueryClient()

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const mutation = useMutation({
    mutationFn: (data: FormData) => jobsApi.create({ ...data, clientId: user!.id }),
    onSuccess: (job) => {
      qc.invalidateQueries({ queryKey: ['jobs'] })
      toast.success('Job posted successfully!')
      router.push(`/jobs/${job.id}`)
    },
    onError: () => toast.error('Failed to post job. Try again.'),
  })

  return (
    <DashboardLayout title="Post a Job">
      <div className="mx-auto max-w-2xl">
        <Card>
          <CardHeader>
            <CardTitle>Job Details</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-5">
              <Input
                id="title" label="Job Title" placeholder="e.g. Build a React Dashboard"
                error={errors.title?.message} {...register('title')}
              />
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Description</label>
                <textarea
                  className="min-h-32 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-800 dark:text-white"
                  placeholder="Describe what needs to be done, deliverables, and timeline expectations…"
                  {...register('description')}
                />
                {errors.description && <p className="text-xs text-red-500">{errors.description.message}</p>}
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <Input
                  id="budget" label="Budget (USD)" type="number" placeholder="3000"
                  error={errors.budget?.message} {...register('budget')}
                />
                <Select
                  id="category" label="Category" placeholder="Select category"
                  options={CATEGORIES}
                  error={errors.category?.message} {...register('category')}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Requirements (optional)</label>
                <textarea
                  className="min-h-20 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-800 dark:text-white"
                  placeholder="e.g. React 18+, TypeScript, Tailwind CSS"
                  {...register('requirements')}
                />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <Button type="button" variant="outline" onClick={() => router.back()}>Cancel</Button>
                <Button type="submit" isLoading={mutation.isPending || isSubmitting}>Post Job</Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  )
}
