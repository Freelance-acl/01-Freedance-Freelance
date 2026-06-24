'use client'

import Link from 'next/link'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { useAuthContext } from '@/providers/AuthProvider'

const schema = z.object({
  name:     z.string().min(2, 'Name must be at least 2 characters'),
  email:    z.string().email('Invalid email'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
  role:     z.enum(['CLIENT', 'FREELANCER']),
})
type FormData = z.infer<typeof schema>

export default function RegisterPage() {
  const { register: registerUser } = useAuthContext()
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { role: 'CLIENT' },
  })

  const onSubmit = async (data: FormData) => {
    try {
      await registerUser(data)
      toast.success('Account created! Welcome to Freedance.')
    } catch {
      toast.error('Registration failed. Please try again.')
    }
  }

  return (
    <Card className="w-full max-w-md shadow-xl">
      <CardHeader className="text-center">
        <div className="mx-auto mb-2 flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600 text-white font-bold text-lg">F</div>
        <CardTitle>Create your account</CardTitle>
        <CardDescription>Join Freedance as a client or freelancer</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input
            id="name" label="Full name" placeholder="John Smith"
            error={errors.name?.message} {...register('name')}
          />
          <Input
            id="email" label="Email" type="email" placeholder="you@example.com"
            error={errors.email?.message} {...register('email')}
          />
          <Input
            id="password" label="Password" type="password" placeholder="At least 6 characters"
            error={errors.password?.message} {...register('password')}
          />
          <Select
            id="role" label="I want to…"
            options={[
              { value: 'CLIENT',     label: 'Hire freelancers (Client)' },
              { value: 'FREELANCER', label: 'Find work (Freelancer)' },
            ]}
            error={errors.role?.message} {...register('role')}
          />
          <Button type="submit" className="w-full" isLoading={isSubmitting}>
            Create account
          </Button>
        </form>

        <p className="mt-4 text-center text-sm text-gray-500">
          Already have an account?{' '}
          <Link href="/login" className="font-medium text-brand-600 hover:underline">
            Sign in
          </Link>
        </p>
      </CardContent>
    </Card>
  )
}
