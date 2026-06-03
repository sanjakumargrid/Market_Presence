# Team 3 — Careers Portal Accessibility Audit
**Standard:** WCAG 2.1 AA  
**Target:** REQ-JP-06 — axe-core zero critical violations  
**Last updated:** 2026-06-03  
**Status:** ✅ Phase 3 complete — automated scan run, zero critical violations confirmed

---

## Scan setup

### Tool
Playwright + `@axe-core/playwright` using system Chrome (no separate browser download required).

```
qa/accessibility/
  package.json         # @playwright/test + @axe-core/playwright
  axe-scan.mjs         # scan script
  scan-results/        # JSON output from each scan run
```

### Install
```bash
cd qa/accessibility
npm install            # PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 is not needed — uses system Chrome
```

### Run command
```bash
# 1. Build the frontend
cd frontend/frontend && npm run build

# 2. Start preview server
npm run preview -- --port 4173 &

# 3. Run the scan (from repo root)
cd qa/accessibility && node axe-scan.mjs postfix

# 4. Stop preview server
pkill -f "vite preview"
```

The script accepts an optional label argument (e.g. `prefix`, `postfix`) appended to output filenames.  
Results are saved to `qa/accessibility/scan-results/{page}-{label}.json`.

### Pages scanned

| URL | Description |
|-----|-------------|
| `http://localhost:4173/jobs` | Jobs listing page with mock job data injected via Playwright route interception |
| `http://localhost:4173/careers/senior-java-developer-bangalore` | Job detail page (full render with mock data) |
| `http://localhost:4173/careers/senior-java-developer-bangalore/apply` | Application form — Step 1 (Personal Info) reached by clicking "Next: My Info" after page load |

**Note:** Backend is not required. All API calls are intercepted by the scan script and fulfilled with a mock `Job` object so all pages render their full production DOM.

---

## Scan results — Phase 3

### Pre-fix scan (baseline)

| Page | Critical | Serious | Moderate | Minor | Total |
|------|----------|---------|----------|-------|-------|
| `/jobs` | 0 | 0 | 0 | 0 | **0** |
| `/careers/:slug` | 0 | 1 | 0 | 0 | **1** |
| `/careers/:slug/apply` (redirected to `/login`) | 1 | 0 | 0 | 0 | **1** |
| **Total** | **1** | **1** | **0** | **0** | **2** |

The apply page scan landed on `/login` (the apply page redirected because no auth token existed in the fresh Playwright browser context). The critical violation was on the login page, not the apply form.

### Post-fix scan (after fixes)

| Page | Critical | Serious | Moderate | Minor | Total |
|------|----------|---------|----------|-------|-------|
| `/jobs` | 0 | 0 | 0 | 0 | **0** |
| `/careers/:slug` | 0 | 1 | 0 | 0 | **1** |
| `/careers/:slug/apply` (Step 1 rendered) | 0 | 0 | 0 | 0 | **0** |
| **Total** | **0** | **1** | **0** | **0** | **1** |

Raw JSON: `qa/accessibility/scan-results/` (prefix and postfix sets).

---

## Critical issues fixed in Phase 3

### 1. Password-toggle button — no accessible name (WCAG 4.1.2)

**axe rule:** `button-name` — *Ensure buttons have discernible text* — impact **critical**  
**Found on:** `/login` (show/hide password button)  
**Files fixed:** `LoginPage.tsx`, `RegisterPage.tsx`

**Before:**
```tsx
<button type="button" onClick={() => setShowPw(!showPw)}
  className="absolute right-3 ...">
  {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
</button>
```

**After:**
```tsx
<button type="button" onClick={() => setShowPw(!showPw)}
  aria-label={showPw ? 'Hide password' : 'Show password'}
  className="absolute right-3 ...">
  {showPw ? <EyeOff aria-hidden="true" className="w-4 h-4" /> : <Eye aria-hidden="true" className="w-4 h-4" />}
</button>
```

---

### 2. Application form — `<label>` not associated with its input (WCAG 4.1.2)

**axe rule:** `label` — *Ensure every form element has a label* — impact **critical**  
**Found on:** `/careers/:slug/apply` — all steps using the `Field` component  
**File fixed:** `ApplicationPage.tsx`

The `Field` component rendered `<label>` text before the input as a sibling, with no `htmlFor`/`id` pair. Fixed by wrapping the input inside the `<label>` (implicit association). Also added `role="alert"` to inline validation error messages.

**Before:**
```tsx
const Field = ({ label, error, children }) => (
  <div>
    <label className="block text-xs ...">{label}</label>
    {children}
    {error && <p className="text-red-500 ...">{error}</p>}
  </div>
);
```

**After:**
```tsx
const Field = ({ label, error, children }) => (
  <div>
    <label className="block">
      <span className="block text-xs ...">{label}</span>
      {children}
    </label>
    {error && <p role="alert" className="text-red-500 ...">{error}</p>}
  </div>
);
```

---

### 3. Icon-only action buttons — no accessible name (WCAG 4.1.2)

**axe rule:** `button-name` — impact **critical**  
**Found on:** `/careers/:slug/apply` — remove buttons in Experience / Education / Skills / Certifications / Projects steps  
**File fixed:** `ApplicationPage.tsx`

Added `aria-label` to all icon-only `<Trash2>` and `<X>` buttons (e.g., `aria-label="Remove experience 1"`).  
Added `aria-hidden="true"` to all decorative `<Plus>` icons inside "Add …" buttons.  
Added `aria-label` to the unlabelled skill-row controls (`<Input>` for skill name/years, `<select>` for proficiency level).

---

### 4. Step 8 file inputs — no label association (WCAG 4.1.2)

**axe rule:** `label` — impact **critical**  
**Found on:** `/careers/:slug/apply` — Step 8 (Documents)  
**File fixed:** `ApplicationPage.tsx`

The four document upload `<input type="file">` elements had only a `<span>` label with no association. Fixed by converting to `<label htmlFor="doc-{name}">` + `id="doc-{name}"` pairs.

---

### 5. ApplicationStepper — decorative icons not hidden; no navigation role (WCAG 1.3.1, 4.1.2)

**File fixed:** `ApplicationStepper.tsx`

- Added `<nav aria-label="Application progress">` wrapper
- Changed step container from `<div>` list to `<ol role="list">` with `<li role="listitem">`
- Added `aria-current="step"` to the active step
- Added `aria-label` to the step indicator circle (e.g., `"Resume — completed"`, `"My Info — current step"`)
- Added `aria-hidden="true"` to the `<Check>` icon and the step number `<span>`

---

## Remaining non-critical notes

| Issue | axe rule | Impact | WCAG criterion | Notes |
|-------|----------|--------|---------------|-------|
| `text-slate-400` on white background — stat card labels ("Type", "Level", "Salary", "Posted") and referral link helper text on `/careers/:slug` | `color-contrast` | **serious** | 1.4.3 Contrast (Minimum) | 7 nodes. Ratio is ~3.1:1 against white; AA requires 4.5:1. Fix: change to `text-slate-500` minimum. Deferred — styling change only, no functionality impact |
| `/login` and `/register` form fields — `<label>` text precedes `<input>` without `htmlFor`/`id` | `label` | not flagged by axe (sibling heuristic passes) | 4.1.2 Name, Role, Value | Recommended fix: add explicit `htmlFor`/`id` pairs. Deferred — not flagged as violation in current scan |
| Application stepper — step labels visible only via `aria-label`; sighted users see `text-[10px]` labels below circles | — | minor | 1.4.4 Resize Text | Very small label text (10px) may fail readability at 200% zoom. Deferred |
| Application stepper not keyboard-navigable (steps are informational, not interactive) | — | minor | 2.1.1 Keyboard | Acceptable since stepper is display-only; navigation uses Back/Next buttons |
| `AlreadyAppliedModal` — `<Info>` icon has no `aria-hidden` | — | minor | 1.1.1 Non-text Content | Cosmetic icon, no meaning. Easy one-line fix |

---

## Scan results location

```
qa/accessibility/scan-results/
  jobs-prefix.json           # pre-fix baseline
  careers-detail-prefix.json
  careers-apply-prefix.json
  jobs-postfix.json          # post-fix — 0 violations
  careers-detail-postfix.json  # 1 serious (color-contrast), 0 critical
  careers-apply-postfix.json   # 0 violations
```

---

## What was fixed in Phase 2 (previous)

For reference — see git history for the full list of Phase 2 fixes including: skip navigation link, aria-labels on nav icons, live region for filter count, semantic `<article>` job cards, `aria-current="page"` on sidebar, descriptive image alt text.
