'use client'

import Link from 'next/link'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { useAuthContext } from '@/providers/AuthProvider'

const schema = z.object({
  email:    z.string().email('Invalid email'),
  password: z.string().min(1, 'Required'),
})
type FormData = z.infer<typeof schema>

const DEMO_ACCOUNTS = [
  { label: 'Admin',      email: 'admin@freelance.com', role: 'ADMIN' },
  { label: 'Client',     email: 'alice@example.com',   role: 'CLIENT' },
  { label: 'Freelancer', email: 'bob@example.com',     role: 'FREELANCER' },
]

export default function LoginPage() {
  const { login } = useAuthContext()
  const { register, handleSubmit, setValue, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const onSubmit = async (data: FormData) => {
    try {
      await login(data)
      toast.success('Welcome back!')
    } catch {
      toast.error('Invalid credentials. Try a demo account below.')
    }
  }

  return (
    <Card className="w-full max-w-md shadow-xl">
      <CardHeader className="text-center">
        <div className="mx-auto mb-2 flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600 text-white font-bold text-lg">F</div>
        <CardTitle>Sign in to Freedance</CardTitle>
        <CardDescription>Enter your credentials or pick a demo account</CardDescription>
      </CardHeader>
      <CardContent>
        {/* Demo shortcuts */}
        <div className="mb-6 grid grid-cols-3 gap-2">
          {DEMO_ACCOUNTS.map((acc) => (
            <button
              key={acc.email}
              type="button"
              onClick={() => { setValue('email', acc.email); setValue('password', 'demo1234') }}
              className="rounded-lg border border-gray-200 p-2 text-center text-xs hover:border-brand-400 hover:bg-brand-50 dark:border-gray-700 dark:hover:bg-brand-900/20 transition-colors"
            >
              <span className="block font-semibold text-gray-700 dark:text-gray-300">{acc.label}</span>
              <span className="text-gray-400">{acc.role}</span>
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input
            id="email" label="Email" type="email" placeholder="you@example.com"
            error={errors.email?.message} {...register('email')}
          />
          <Input
            id="password" label="Password" type="password" placeholder="••••••••"
            error={errors.password?.message} {...register('password')}
          />
          <Button type="submit" className="w-full" isLoading={isSubmitting}>
            Sign in
          </Button>
        </form>

        <p className="mt-4 text-center text-sm text-gray-500">
          No account?{' '}
          <Link href="/register" className="font-medium text-brand-600 hover:underline">
            Create one
          </Link>
        </p>
      </CardContent>
    </Card>
  )
}
