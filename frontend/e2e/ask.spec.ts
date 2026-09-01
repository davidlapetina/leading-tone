import { expect, test } from '@playwright/test'

/**
 * The Ask view: a question, and the material the answer was built from.
 *
 * <p>The material is what makes this different from a chat box, so that is what is asserted:
 * a passage without its source, or an example without its citation, would be a claim nobody
 * can check.
 */
const API = 'http://localhost:8088/api'

test.describe('asking a question', () => {
  // With the model off, as the rest of the suite runs: these test that a question is
  // answered and attributed, not a model's wording, and the application is meant to answer
  // one with no model at all. It is also the difference between a second and a minute.
  test.beforeAll(async ({ playwright }) => {
    const request = await playwright.request.newContext()
    await request.put(`${API}/settings`, { data: { llmEnabled: false } })
    await request.dispose()
  })

  test.afterAll(async ({ playwright }) => {
    const request = await playwright.request.newContext()
    await request.put(`${API}/settings`, { data: { llmEnabled: true } })
    await request.dispose()
  })

  test('answers, and shows what the answer was built from', async ({ page }) => {
    await page.goto('/')
    await page.locator('.nav-item', { hasText: 'Ask' }).click()

    await page.getByRole('button', { name: 'What is V7/V in C major?' }).click()
    await expect(page.locator('.ask-entry')).toBeVisible({ timeout: 30000 })

    // The computed answer needs neither an index nor a network, so it is always there.
    await expect(page.locator('.ask-computed').first()).toContainText('D')
    await expect(page.locator('.ask-answer').first()).not.toBeEmpty()

    // Every passage names its source, and every example its citation.
    for (const passage of await page.locator('.ask-passage').all()) {
      await expect(passage.locator('figcaption')).not.toBeEmpty()
    }
    for (const example of await page.locator('.ask-entry .example-card').all()) {
      await expect(example.locator('.example-cite')).not.toBeEmpty()
    }
  })

  test('a typed question can be asked, and the thread can be cleared', async ({ page }) => {
    await page.goto('/')
    await page.locator('.nav-item', { hasText: 'Ask' }).click()

    await page.getByLabel('Your question').fill('What is a tritone?')
    await page.locator('.ask-form button[type=submit]').click()
    await expect(page.locator('.ask-entry')).toHaveCount(1, { timeout: 30000 })

    await page.getByRole('button', { name: 'Start again' }).click()
    await expect(page.locator('.ask-entry')).toHaveCount(0)
  })
})
