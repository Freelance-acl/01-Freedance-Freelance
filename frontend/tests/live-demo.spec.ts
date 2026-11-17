import { test, expect, Page } from '@playwright/test'

const password = 'DemoPass123!'

test('registration uses the backend and the authenticated profile survives reload', async ({ page }) => {
  const suffix = Date.now().toString()
  await page.goto('/register')
  await page.getByLabel('Full name').fill('Demo Recording Client')
  await page.getByLabel('Email', { exact: true }).fill(`recording.${suffix}@demo.freedance.local`)
  await page.getByLabel('Phone', { exact: true }).fill('+20' + suffix.slice(-10))
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Create account', exact: true }).click()
  await expect(page).toHaveURL('/dashboard/client')
  await expect(page.getByText('Demo Recording Client').first()).toBeVisible()
  await page.reload()
  await expect(page.getByText('Demo Recording Client').first()).toBeVisible()
  await expect(page.getByText('Some backend data could not be loaded', { exact: false })).toHaveCount(0)
})

async function login(page: Page, role: string) {
  await page.goto('/login')
  await page.getByLabel('Email', { exact: true }).fill(`${role}@demo.freedance.local`)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()
  await expect(page).toHaveURL(`/dashboard/${role}`)
}

test('live UI: post job, propose, accept, complete, release payout, reload persisted state', async ({ browser }) => {
  const client = await browser.newContext()
  const freelancer = await browser.newContext()
  const cp = await client.newPage()
  const fp = await freelancer.newPage()
  const errors: string[] = []
  for (const page of [cp, fp]) {
    page.on('pageerror', e => errors.push(e.message))
    page.on('response', r => { if (r.url().includes('/api/') && r.status() >= 400) errors.push(`${r.status()} ${r.url()}`) })
  }
  await login(cp, 'client')
  await login(fp, 'freelancer')
  await cp.goto('/jobs/create')
  const title = `Video demo: Live marketplace ${Date.now()}`
  await cp.getByLabel('Job Title').fill(title)
  await cp.getByPlaceholder('Describe what needs to be done', { exact: false }).fill('Create a responsive marketplace dashboard using the live backend services.')
  await cp.getByLabel('Budget (USD)').fill('1200')
  await cp.getByLabel('Category', { exact: true }).selectOption('WEB_DEV')
  await cp.getByRole('button', { name: 'Post Job', exact: true }).click()
  await expect(cp).toHaveURL(/\/jobs\/\d+$/)
  const jobPath = new URL(cp.url()).pathname
  await expect(cp.getByRole('heading', { name: title, exact: true })).toBeVisible()
  await fp.goto(jobPath)
  await fp.getByRole('button', { name: 'Submit Proposal', exact: true }).click()
  await fp.getByPlaceholder('3000', { exact: true }).fill('1000')
  await fp.getByPlaceholder('14', { exact: true }).fill('7')
  await fp.getByPlaceholder('Explain your experience', { exact: false }).fill('I will build this dashboard and verify its live backend integration.')
  const submitted = fp.waitForResponse(r => r.url().endsWith('/api/proposals') && r.request().method() === 'POST')
  await fp.getByRole('button', { name: 'Submit Proposal', exact: true }).click()
  const response = await submitted
  expect(response.ok()).toBeTruthy()
  const proposal = await response.json()
  await cp.goto(`/proposals/${proposal.id}`)
  await cp.getByRole('button', { name: 'Accept Proposal', exact: true }).click()
  await expect.poll(async () => {
    const token = await cp.evaluate(() => localStorage.getItem('freelance_token'))
    const result = await cp.request.get('/api/contracts', { headers: { Authorization: `Bearer ${token}` } })
    return (await result.json()).find((c: { proposalId: number }) => c.proposalId === proposal.id)?.id
  }).toBeTruthy()
  await fp.goto(`/proposals/${proposal.id}`)
  await fp.getByRole('button', { name: 'Mark as Complete', exact: true }).click()
  await cp.goto('/saga')
  await cp.getByLabel('Choose a proposal').selectOption(String(proposal.id))
  await expect(cp.getByText('Completed', { exact: true })).toBeVisible()
  await cp.getByRole('link', { name: /Open contract #/ }).click()
  await cp.getByRole('button', { name: 'Process Demo Payout', exact: true }).click()
  await expect(cp.getByText('Demo payout processed in the local ledger.')).toBeVisible()
  await fp.goto('/wallet')
  await expect(fp.getByText('Completed', { exact: true }).first()).toBeVisible()
  await fp.reload()
  await expect(fp.getByText('Completed', { exact: true }).first()).toBeVisible()
  await fp.goto(`/proposals/${proposal.id}`)
  await expect(fp.getByText('Paid', { exact: true })).toBeVisible()
  expect(errors).toEqual([])
  await client.close()
  await freelancer.close()
})
