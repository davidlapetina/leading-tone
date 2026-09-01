import { expect, test } from '@playwright/test'

const API = 'http://localhost:8088/api'

/**
 * What a lesson shows under it when the corpora are in.
 *
 * These assert the contract rather than the contents, because a machine that has not
 * brought any sources in has no examples to show and that is a correct state, not a
 * failure. What must never happen is an example that renders as nothing: a citation that
 * stopped being serialised once left the card blank with no error anywhere, and only a
 * screenshot caught it.
 */
test.describe('corpus examples', () => {
  test('an example is always attributed, and never renders as nothing', async ({ page }) => {
    await page.goto('/')
    await page.locator('.concept-card', { hasText: 'Secondary dominants' }).first()
      .getByRole('button', { name: 'Read' }).click()

    const set = page.locator('.example-set, .example-none').first()
    await expect(set).toBeVisible()

    const cards = page.locator('.example-card')
    const count = await cards.count()
    if (count === 0) {
      // No sources brought in on this machine. The page has to say so rather than sit empty.
      await expect(page.locator('.example-none')).not.toBeEmpty()
      return
    }

    for (let i = 0; i < count; i += 1) {
      const card = cards.nth(i)
      await expect(card.locator('.example-cite'), 'every example names its source').not.toBeEmpty()
      await expect(card.locator('.origin-badge'), 'a reader must see where it came from').toBeVisible()
      // Either engraved music or an explicit absence — never an empty frame.
      const drawn = await card.locator('svg').count()
      if (drawn === 0) {
        expect(await card.innerText(), 'a card with no score must say why').not.toHaveLength(0)
      }
    }
  })

  test('what came from a score is marked differently from what was generated', async ({ page }) => {
    const response = await page.request.get(`${API}/knowledge/examples?romanNumeral=V7&limit=3`)
    expect(response.ok()).toBeTruthy()
    const body = await response.json()

    for (const example of body.examples ?? []) {
      expect(example.origin, 'origin is what tells a learner it is real').toBeTruthy()
      if (example.origin === 'VERIFIED_CORPUS') {
        expect(example.citation, 'a corpus example carries its citation').toBeTruthy()
        expect(example.licenseId, 'and its licence, which is not ours to drop').toBeTruthy()
      }
    }
  })
})
