'use client'

import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useAuthContext } from '@/providers/AuthProvider'
import { usersApi } from '@/lib/api'

type ProfileForm = { name: string; bio: string }

export default function SettingsPage() {
  const { user, logout } = useAuthContext()

  const { register, handleSubmit, formState: { isSubmitting } } = useForm<ProfileForm>({
    defaultValues: { name: user?.name ?? '', bio: '' },
  })

  const onSave = async (data: ProfileForm) => {
    try {
      await usersApi.updateProfile(user!.id, data)
      toast.success('Profile updated.')
    } catch {
      toast.error('Failed to update profile.')
    }
  }

  return (
    <DashboardLayout title="Settings">
      <div className="mx-auto max-w-2xl space-y-6">
        {/* Profile */}
        <Card>
          <CardHeader>
            <CardTitle>Profile</CardTitle>
            <CardDescription>Update your display name and bio.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSave)} className="space-y-4">
              <Input id="name" label="Display Name" {...register('name')} />
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Bio</label>
                <textarea
                  className="min-h-24 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-800 dark:text-white"
                  placeholder="Tell others about yourself…"
                  {...register('bio')}
                />
              </div>
              <Button type="submit" isLoading={isSubmitting}>Save Changes</Button>
            </form>
          </CardContent>
        </Card>

        {/* Account info */}
        <Card>
          <CardHeader>
            <CardTitle>Account</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {[
              { label: 'Email',  value: user?.email },
              { label: 'Role',   value: user?.role },
              { label: 'User ID',value: String(user?.id) },
            ].map(({ label, value }) => (
              <div key={label} className="flex items-center justify-between border-b border-gray-100 pb-3 last:border-0 dark:border-gray-700">
                <span className="text-sm text-gray-500">{label}</span>
                <span className="text-sm font-medium text-gray-900 dark:text-white">{value}</span>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Danger zone */}
        <Card className="border-red-200 dark:border-red-900/50">
          <CardHeader>
            <CardTitle className="text-red-600">Danger Zone</CardTitle>
          </CardHeader>
          <CardContent>
            <Button variant="danger" onClick={() => { logout(); toast.success('Logged out.') }}>
              Sign Out
            </Button>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  )
}
