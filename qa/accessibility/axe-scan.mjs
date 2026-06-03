/**
 * REQ-JP-06 — axe-core accessibility scan
 *
 * Usage (from repo root):
 *   cd qa/accessibility && npm install
 *   cd ../../frontend/frontend && npm run build && npm run preview &
 *   cd ../../qa/accessibility && node axe-scan.mjs
 *
 * Requires: Google Chrome installed (uses system Chrome, no separate browser download).
 * Pages scanned: /jobs  /careers/:slug  /careers/:slug/apply
 * API calls are intercepted with mock data so pages render fully without a running backend.
 */

import { chromium } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { writeFileSync, mkdirSync } from 'fs';
import { fileURLToPath } from 'url';
import path from 'path';

const __dirname  = path.dirname(fileURLToPath(import.meta.url));
const RESULTS_DIR = path.join(__dirname, 'scan-results');
const BASE_URL    = 'http://localhost:4173';
const SLUG        = 'senior-java-developer-bangalore';

// ── Mock job (matches frontend Job interface — snake_case) ────────────────────
const MOCK_JOB = {
  id: 1,
  demand_id: 42,
  title: 'Senior Java Developer',
  slug: SLUG,
  description: 'Build the next generation of AI-powered workforce intelligence.',
  requirements: 'Java, Spring Boot, PostgreSQL, 5+ years experience',
  responsibilities: 'Lead backend architecture\nMentor junior engineers\nDesign APIs',
  benefits: 'Health insurance\nFlexible hours\nRemote-friendly',
  employment_type: 'FULL_TIME',
  experience_level: 'SENIOR',
  work_mode: 'HYBRID',
  location_city: 'Bangalore',
  location_state: 'KA',
  location_country: 'IN',
  department: 'Engineering',
  job_category: 'Backend Development',
  salary_min: 1800000,
  salary_max: 2500000,
  currency: 'INR',
  show_salary: true,
  posting_status: 'PUBLISHED',
  meta_title: 'Senior Java Developer | Forge AI Careers',
  meta_description: 'Join the Forge AI engineering team.',
  published_at: '2026-01-15T09:00:00Z',
  expires_at: '2027-06-30T23:59:59Z',
  closed_at: null,
  created_at: '2026-01-10T08:00:00Z',
  updated_at: '2026-01-15T09:00:00Z',
};

// ── Intercept all API calls ───────────────────────────────────────────────────
async function mockApis(page) {
  // Single job by slug
  await page.route(/\/api\/public\/jobs\/[^?/]+(\?.*)?$/, async route => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(MOCK_JOB) });
  });
  // Jobs list
  await page.route(/\/api\/public\/jobs(\?.*)?$/, async route => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify([MOCK_JOB]) });
  });
  // Analytics + anything else — swallow silently
  await page.route(/\/api\/.*/, async route => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
  });
}

// ── Run axe on one page ───────────────────────────────────────────────────────
async function scanPage(browser, { name, url, setup }, label = '') {
  const context = await browser.newContext();

  // Pre-populate auth token so protected pages don't redirect to /login
  await context.addInitScript(() => {
    window.localStorage.setItem('forge_token', 'mock-token');
    window.localStorage.setItem('forge_user', JSON.stringify({
      userId: '1', email: 'candidate@forge.ai',
      name: 'Guest Candidate', role: 'CANDIDATE', token: 'mock-token',
    }));
  });

  const page = await context.newPage();
  await mockApis(page);
  await page.goto(url, { waitUntil: 'networkidle', timeout: 20_000 });
  if (setup) await setup(page);
  await page.waitForTimeout(600); // let React finish any deferred renders

  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa'])
    .analyze();

  mkdirSync(RESULTS_DIR, { recursive: true });
  const filename = label ? `${name}-${label}.json` : `${name}.json`;
  writeFileSync(path.join(RESULTS_DIR, filename), JSON.stringify(results, null, 2));

  const byImpact = level => results.violations.filter(v => v.impact === level);
  const critical = byImpact('critical');
  const serious  = byImpact('serious');
  const moderate = byImpact('moderate');
  const minor    = byImpact('minor');

  console.log(`\n── ${name}  ${url}`);
  console.log(`   violations  : ${results.violations.length}  (critical ${critical.length}  serious ${serious.length}  moderate ${moderate.length}  minor ${minor.length})`);
  for (const v of results.violations) {
    const nodes = v.nodes.map(n => n.target.join(' ')).slice(0, 2).join(', ');
    console.log(`   [${v.impact.toUpperCase().padEnd(8)}] ${v.id}: ${v.description}`);
    console.log(`              nodes (${v.nodes.length}): ${nodes}${v.nodes.length > 2 ? ' …' : ''}`);
  }
  if (results.violations.length === 0) console.log('   ✓ zero violations');

  await context.close();
  return results;
}

// ── Main ──────────────────────────────────────────────────────────────────────
async function main() {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });

  const pages = [
    {
      name: 'jobs',
      url:  `${BASE_URL}/jobs`,
    },
    {
      name: 'careers-detail',
      url:  `${BASE_URL}/careers/${SLUG}`,
    },
    {
      name: 'careers-apply',
      url:  `${BASE_URL}/careers/${SLUG}/apply`,
      setup: async page => {
        // Advance from Step 0 (Resume) → Step 1 (Personal Info) so Field inputs are in the DOM
        const next = page.getByRole('button', { name: /next/i });
        if (await next.isVisible({ timeout: 3000 }).catch(() => false)) {
          await next.click();
          await page.waitForTimeout(400);
        }
      },
    },
  ];

  const label = process.argv[2] ?? 'postfix';
  const allResults = {};
  for (const p of pages) {
    allResults[p.name] = await scanPage(browser, p, label);
  }

  await browser.close();

  const totalCritical = Object.values(allResults)
    .reduce((n, r) => n + r.violations.filter(v => v.impact === 'critical').length, 0);
  const totalAll = Object.values(allResults)
    .reduce((n, r) => n + r.violations.length, 0);

  console.log('\n═══════════════════════════════════════════════');
  console.log(`TOTAL violations  : ${totalAll}`);
  console.log(`TOTAL critical    : ${totalCritical}`);
  console.log(`Results saved to  : ${RESULTS_DIR}`);
  console.log('═══════════════════════════════════════════════\n');

  process.exit(totalCritical > 0 ? 1 : 0);
}

main().catch(e => { console.error(e); process.exit(1); });
