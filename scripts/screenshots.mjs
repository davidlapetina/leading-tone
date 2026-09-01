/**
 * Regenerates the screenshots in the README.
 *
 * Run against a running stack (`make backend` and `make frontend`) with sources already
 * brought in, otherwise the corpus example has nothing to draw:
 *
 *     node scripts/screenshots.mjs
 *
 * They are in the repository rather than generated on demand, so they go stale quietly when
 * the interface changes. This exists so refreshing them is one command rather than an
 * afternoon of lining windows up by hand.
 */
import { chromium } from '../frontend/node_modules/@playwright/test/index.mjs'
import { mkdirSync } from 'node:fs'

const BASE = process.env.BASE ?? 'http://localhost:5173'
const OUT = new URL('../docs/images/', import.meta.url).pathname
mkdirSync(OUT, { recursive: true })

const browser = await chromium.launch()
const page = await browser.newPage({
  viewport: { width: 1280, height: 980 },
  deviceScaleFactor: 2,
})
page.on('pageerror', (error) => console.log('  page error:', error.message))

const shot = async (name, target) => {
  await (target ?? page).screenshot({ path: `${OUT}${name}.png` })
  console.log(`  docs/images/${name}.png`)
}

await page.goto(BASE, { waitUntil: 'networkidle' })
await page.waitForTimeout(1200)
await shot('learn')

// The jazz route, which is its own section of the same page.
const jazz = page.locator('.jazz-path')
await jazz.scrollIntoViewIfNeeded()
await shot('jazz', jazz)

// A lesson, and under it the same harmony in real music. Secondary dominants because the
// numeral is long enough to read above the staff, which is the whole point of the picture.
await page.locator('.concept-card', { hasText: 'Secondary dominants' }).first()
  .getByRole('button', { name: 'Read' }).click()
await page.waitForTimeout(2500)
const example = page.locator('.example-card').first()
if (await example.count()) {
  await example.scrollIntoViewIfNeeded()
  await shot('example', example)
} else {
  console.log('  no corpus example on the page: bring the sources in first')
}

// The lesson itself, above the example.
await page.evaluate(() => window.scrollTo(0, 0))
await page.waitForTimeout(400)
await shot('lesson')

// The tutor. Shorter, because an opening turn is one message and the rest of a tall
// viewport is empty waiting area.
await page.getByRole('button', { name: /practise/i }).first().click()
await page.waitForTimeout(2500)
await page.setViewportSize({ width: 1280, height: 620 })
await page.waitForTimeout(400)
await shot('practise')
await page.setViewportSize({ width: 1280, height: 980 })

// Where sources are brought in, each showing its licence.
await page.getByRole('button', { name: /settings/i }).first().click()
await page.waitForTimeout(1200)
const sources = page.locator('.panel-card').filter({ hasText: 'Published sources' })
if (await sources.count()) {
  await sources.scrollIntoViewIfNeeded()
  await shot('sources', sources)
}

await browser.close()
