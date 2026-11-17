// Records real UI actions against the running local backend. No mock responses.
import { chromium, expect } from '@playwright/test'
import { mkdir, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const stamp = Date.now().toString()
const output = path.join(root, 'artifacts/demo-video', stamp)
await mkdir(path.join(output, 'raw'), { recursive: true })
const browser = await chromium.launch({ channel: 'chrome', headless: true, slowMo: 110 })
const context = await browser.newContext({
  viewport: { width: 1600, height: 800 },
  recordVideo: { dir: path.join(output, 'raw'), size: { width: 1600, height: 800 } },
  reducedMotion: 'reduce',
})
const page = await context.newPage()
const started = Date.now()
const scenes = []
const requests = []
const failures = []
const password = 'DemoPass123!'
const accounts = {
  client: { name: 'Video Client', email: `video.client.${stamp}@demo.freedance.local`, phone: '+20' + stamp.slice(-9) + '1' },
  freelancer: { name: 'Video Freelancer', email: `video.freelancer.${stamp}@demo.freedance.local`, phone: '+20' + stamp.slice(-9) + '2' },
}
page.on('pageerror', e => failures.push(e.message))
page.on('response', r => {
  const url = new URL(r.url())
  if (url.origin === 'http://localhost:3000' && url.pathname.startsWith('/api/')) {
    requests.push({ at: (Date.now() - started) / 1000, method: r.request().method(), path: url.pathname, status: r.status() })
    if (r.status() >= 400) failures.push(`${r.status()} ${url.pathname}`)
  }
})

// A presentation cursor helps viewers follow clicks. It never changes application data.
await context.addInitScript(() => {
  window.addEventListener('DOMContentLoaded', () => {
    const cursor = document.createElement('div')
    cursor.style.cssText = 'display:none;position:fixed;left:0;top:0;width:24px;height:24px;border:3px solid #6366f1;border-radius:50%;background:#6366f133;pointer-events:none;z-index:2147483647;transform:translate(-50%,-50%)'
    document.body.appendChild(cursor)
    document.addEventListener('mousemove', e => { cursor.style.display = 'block'; cursor.style.left = e.clientX + 'px'; cursor.style.top = e.clientY + 'px' })
  })
})

const pause = seconds => page.waitForTimeout(seconds * 1000)
function scene(title, caption) {
  scenes.push({ start: (Date.now() - started) / 1000, title, caption })
  console.log(title)
}
async function click(locator) {
  await locator.scrollIntoViewIfNeeded()
  const box = await locator.boundingBox()
  if (box) await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2, { steps: 15 })
  await pause(0.3)
  await locator.click()
}
async function type(locator, text) {
  await click(locator)
  await locator.fill('')
  await locator.pressSequentially(text, { delay: 26 })
}
async function logout() {
  await click(page.getByTitle('Log out'))
  await expect(page).toHaveURL('http://localhost:3000/login')
}
async function login(role) {
  await type(page.getByLabel('Email', { exact: true }), accounts[role].email)
  await type(page.getByLabel('Password', { exact: true }), password)
  await click(page.getByRole('button', { name: 'Sign in', exact: true }))
  await expect(page).toHaveURL(`http://localhost:3000/dashboard/${role}`)
  await pause(2)
}
async function register(role) {
  await page.goto('http://localhost:3000/register')
  await type(page.getByLabel('Full name'), accounts[role].name)
  await type(page.getByLabel('Phone', { exact: true }), accounts[role].phone)
  await type(page.getByLabel('Email', { exact: true }), accounts[role].email)
  await type(page.getByLabel('Password', { exact: true }), password)
  await page.locator('#role').selectOption(role.toUpperCase())
  await pause(1)
  await click(page.getByRole('button', { name: 'Create account', exact: true }))
  await expect(page).toHaveURL(`http://localhost:3000/dashboard/${role}`)
  await pause(4)
}

let jobId, proposalId, contractId, payoutId
try {
  scene('FREEDANCE / LIVE BACKEND DEMO', 'Five microservices, one API gateway, real local database records. No mock data.')
  await page.goto('http://localhost:3000/')
  await pause(6)

  scene('01 / USER SERVICE', 'Register a client. Authentication and profile data come from the backend.')
  await register('client')
  await logout()
  scene('01 / USER SERVICE', 'Register a freelancer with a separate account and role.')
  await register('freelancer')
  await logout()

  scene('02 / JOB SERVICE', 'Sign in as the client and publish a new web development job.')
  await login('client')
  await click(page.getByRole('link', { name: 'Jobs', exact: true }))
  await pause(2)
  await page.goto('http://localhost:3000/jobs/create')
  const title = 'Build a responsive marketplace dashboard'
  await type(page.getByLabel('Job Title'), title)
  await type(page.getByPlaceholder('Describe what needs to be done', { exact: false }), 'Create a responsive React dashboard connected to the marketplace APIs, with job listings, proposal tracking, and a payout summary.')
  await type(page.getByLabel('Budget (USD)'), '1200')
  await page.getByLabel('Category', { exact: true }).selectOption('WEB_DEV')
  await pause(2)
  await click(page.getByRole('button', { name: 'Post Job', exact: true }))
  await expect(page).toHaveURL(/\/jobs\/\d+$/)
  jobId = Number(new URL(page.url()).pathname.split('/').at(-1))
  await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible()
  await pause(5)
  await logout()

  scene('03 / PROPOSAL SERVICE', 'The freelancer opens the new job and submits a $1,000 proposal for seven days.')
  await login('freelancer')
  await click(page.getByRole('link', { name: 'Jobs', exact: true }))
  await pause(3)
  await page.goto(`http://localhost:3000/jobs/${jobId}`)
  await pause(3)
  await click(page.getByRole('button', { name: 'Submit Proposal', exact: true }))
  await type(page.getByPlaceholder('3000', { exact: true }), '1000')
  await type(page.getByPlaceholder('14', { exact: true }), '7')
  await type(page.getByPlaceholder('Explain your experience', { exact: false }), 'I will build a polished dashboard and verify the integration with all five backend microservices. Delivery includes responsive layouts and workflow testing.')
  const submitted = page.waitForResponse(r => r.url().endsWith('/api/proposals') && r.request().method() === 'POST')
  await pause(2)
  await click(page.getByRole('button', { name: 'Submit Proposal', exact: true }))
  const proposalResponse = await submitted
  expect(proposalResponse.ok()).toBeTruthy()
  proposalId = (await proposalResponse.json()).id
  await page.goto(`http://localhost:3000/proposals/${proposalId}`)
  await pause(5)
  await logout()

  scene('04 / CONTRACT SERVICE + RABBITMQ', 'Accept the proposal. A backend event creates the contract automatically.')
  await login('client')
  await page.goto(`http://localhost:3000/proposals/${proposalId}`)
  await pause(3)
  await click(page.getByRole('button', { name: 'Accept Proposal', exact: true }))
  await expect(page.getByText(/Contract #\d+ Created/, { exact: true })).toBeVisible({ timeout: 30_000 })
  await pause(4)
  await click(page.getByRole('link', { name: 'Live Workflow', exact: true }))
  await page.getByLabel('Choose a proposal').selectOption(String(proposalId))
  const contractLink = page.getByRole('link', { name: /Open contract #/ })
  await expect(contractLink).toBeVisible()
  contractId = Number((await contractLink.getAttribute('href')).split('/').at(-1))
  await pause(7)
  await click(contractLink)
  await pause(5)
  await logout()

  scene('05 / COMPLETION SAGA', 'The freelancer marks work complete. Contract and wallet services react to the event.')
  await login('freelancer')
  await page.goto(`http://localhost:3000/proposals/${proposalId}`)
  await pause(3)
  await click(page.getByRole('button', { name: 'Mark as Complete', exact: true }))
  await pause(5)
  await click(page.getByRole('link', { name: 'Live Workflow', exact: true }))
  await page.getByLabel('Choose a proposal').selectOption(String(proposalId))
  await expect(page.getByText('Completed', { exact: true })).toBeVisible()
  await pause(7)
  await logout()

  scene('06 / WALLET SERVICE', 'The client releases the pending demo payout. The backend simulates this bank transfer.')
  await login('client')
  await page.goto(`http://localhost:3000/contracts/${contractId}`)
  await expect(page.getByRole('button', { name: 'Process Demo Payout', exact: true })).toBeVisible()
  await pause(5)
  const released = page.waitForResponse(r => r.url().includes(`/api/payouts/contract/${contractId}`) && r.request().method() === 'POST')
  await click(page.getByRole('button', { name: 'Process Demo Payout', exact: true }))
  const payoutResponse = await released
  expect(payoutResponse.ok()).toBeTruthy()
  const payout = await payoutResponse.json()
  payoutId = payout.id
  expect(payout.status).toBe('COMPLETED')
  expect(payout.amount).toBe(1000)
  expect(payout.transactionDetails.platformFee).toBe(100)
  await pause(6)
  await logout()

  scene('07 / PERSISTENCE CHECK', 'Freelancer wallet: $1,000 gross, $100 platform fee, $900 net. Reload to verify persistence.')
  await login('freelancer')
  await click(page.getByRole('link', { name: 'Wallet', exact: true }))
  await expect(page.getByText('Completed', { exact: true })).toBeVisible()
  await pause(6)
  await page.reload()
  await expect(page.getByText('Completed', { exact: true })).toBeVisible()
  await pause(6)
  await page.screenshot({ path: path.join(output, 'wallet-proof.png') })

  scene('08 / ALL SERVICES VERIFIED', 'One connected workflow: proposal PAID, contract COMPLETED, payout COMPLETED. No real money moves.')
  await click(page.getByRole('link', { name: 'Live Workflow', exact: true }))
  await page.getByLabel('Choose a proposal').selectOption(String(proposalId))
  await expect(page.getByText('Paid', { exact: true })).toBeVisible()
  await pause(8)
  await page.screenshot({ path: path.join(output, 'workflow-proof.png') })

  expect(failures).toEqual([])
  const ids = { jobId, proposalId, contractId, payoutId }
  await writeFile(path.join(output, 'verification.json'), JSON.stringify({ accounts, ids, requests, failures, scenes, duration: (Date.now() - started) / 1000 }, null, 2))
  console.log('VERIFIED', JSON.stringify(ids))
} catch (error) {
  await page.screenshot({ path: path.join(output, 'recording-failure.png') }).catch(() => {})
  await writeFile(path.join(output, 'failure.json'), JSON.stringify({ error: String(error), failures, requests, scenes }, null, 2))
  throw error
} finally {
  await context.close()
  await browser.close()
}
await writeFile(path.join(root, 'artifacts/demo-video/latest.json'), JSON.stringify({ output, video: await page.video().path() }, null, 2))
console.log('OUTPUT', output)
