// UI smoke check against a running demo stack (backend :8080 with the demo profile, frontend :5173).
// Usage: node e2e-check.mjs    (uses the installed Microsoft Edge via Playwright; screenshots go to ../docs/screenshots)
import { chromium } from 'playwright'
import { mkdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const BASE = 'http://localhost:5173'
const SHOTS = fileURLToPath(new URL('../docs/screenshots/', import.meta.url))
mkdirSync(SHOTS, { recursive: true })

const results = []
const consoleErrors = []
const record = (name, ok, detail = '') => {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${name}${detail ? ' | ' + detail : ''}`)
}

const browser = await chromium.launch({ channel: 'msedge', headless: true })

async function session(label, personaButton) {
  const ctx = await browser.newContext({ viewport: { width: 1360, height: 860 } })
  const page = await ctx.newPage()
  page.on('console', (m) => {
    if (m.type() === 'error') consoleErrors.push(`[${label}] ${m.text()}`)
  })
  page.on('pageerror', (e) => consoleErrors.push(`[${label}] pageerror: ${e.message}`))
  await page.goto(BASE + '/login')
  if (personaButton) {
    await page.getByRole('button', { name: personaButton }).click()
    await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 15000 })
  }
  return { ctx, page }
}
const shot = (page, name) => page.screenshot({ path: SHOTS + name + '.png', fullPage: true })
const has = async (page, text, timeout = 8000) => {
  try {
    await page.getByText(text, { exact: false }).first().waitFor({ timeout })
    return true
  } catch {
    return false
  }
}

try {
  // ---- login page
  {
    const ctx = await browser.newContext({ viewport: { width: 1360, height: 860 } })
    const page = await ctx.newPage()
    page.on('console', (m) => m.type() === 'error' && consoleErrors.push(`[login] ${m.text()}`))
    await page.goto(BASE + '/login')
    const names = ['Employee (Asha)', 'New joiner (Ravi)', 'Manager (Priya)', 'HR (Meena)']
    let all = true
    for (const n of names) all = all && (await page.getByRole('button', { name: n }).waitFor({ timeout: 8000 }).then(() => true).catch(() => false))
    await shot(page, '01-login')
    record('login: persona cards visible', all)
    await ctx.close()
  }

  // ---- employee (Ravi, the new joiner)
  {
    const { ctx, page } = await session('ravi', 'New joiner (Ravi)')
    record('login: one-click login works (lands off /login)', !page.url().includes('/login'), page.url())

    await page.goto(BASE + '/balances').catch(() => {})
    await page.getByText('Annual Leave').first().waitFor()
    const pr = await has(page, '6/12')
    await shot(page, '02-employee-balances')
    record('employee dashboard/balances: pro-rata formula shown', pr)

    await page.getByRole('link', { name: 'Apply for leave' }).click()
    await page.locator('#from').fill('2026-11-04')
    await page.locator('#to').fill('2026-11-05')
    const preview = await has(page, 'Working days')
    const banner = await has(page, 'Team coverage conflict')
    const enabled = await page.getByRole('button', { name: 'Submit request' }).isEnabled()
    await shot(page, '03-apply-conflict')
    record('apply: live preview shows', preview)
    record('apply: conflict banner shows and Submit stays enabled', banner && enabled, `banner=${banner} submitEnabled=${enabled}`)

    await page.getByRole('link', { name: 'My requests' }).click()
    await page.locator('tbody tr').first().click()
    const drawer = await has(page, 'Audit timeline')
    await shot(page, '04-my-requests-timeline')
    record('my requests: timeline drawer opens', drawer)

    await page.getByRole('button', { name: 'Close' }).click()
    await page.getByRole('link', { name: 'Change password' }).click()
    const cp = await has(page, 'Current password')
    await shot(page, '08-change-password')
    record('change-password page', cp)
    await ctx.close()
  }

  // ---- manager (Priya)
  {
    const { ctx, page } = await session('priya', 'Manager (Priya)')
    await page.getByText('Waiting for your decision').waitFor()
    await shot(page, '05-manager-inbox')
    record('manager inbox loads with waiting requests', await has(page, 'Ravi Kumar'))
    await page.locator('tbody tr', { hasText: 'Ravi Kumar' }).first().click()
    const sim = page.getByRole('button', { name: /Simulate timeout/ })
    const simVisible = await sim.waitFor({ timeout: 8000 }).then(() => true).catch(() => false)
    record('manager inbox: Simulate Timeout button visible', simVisible)
    if (simVisible) {
      await sim.click()
      const esc = await has(page, 'escalated to HR', 8000)
      await shot(page, '06-simulate-timeout')
      record('simulate timeout escalates the request', esc)
    }
    await ctx.close()
  }

  // ---- HR (Meena)
  {
    const { ctx, page } = await session('meena', 'HR (Meena)')
    await page.getByRole('heading', { name: 'HR queue' }).waitFor()
    await shot(page, '07-hr-queue')
    record('HR queue loads (escalated request visible)', await has(page, 'Escalated'))
    await page.getByRole('link', { name: 'People & teams' }).click()
    const people = await has(page, 'Priya Sharma')
    await shot(page, '09-people-teams')
    record('People & teams page lists people', people)
    await ctx.close()
  }
} catch (e) {
  record('script error', false, e.message.split('\n')[0])
}

await browser.close()
console.log('\nconsole errors:', consoleErrors.length ? '\n  ' + [...new Set(consoleErrors)].join('\n  ') : 'none')
console.log(`${results.filter((r) => r.ok).length}/${results.length} checks passed`)
