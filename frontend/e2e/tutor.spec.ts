import { expect, test, type Page } from '@playwright/test'

const API = 'http://localhost:8088/api'

/** Starts every scenario from an empty learner model, so the tutor really is starting cold. */
async function resetLearner(page: Page) {
  const response = await page.request.delete(`${API}/learner`)
  expect(response.ok(), 'the learner reset endpoint should be available in dev').toBeTruthy()
}

/** Starts cold and goes straight to the tutor, which now lives behind its own tab. */
async function openTutor(page: Page) {
  await resetLearner(page)
  await page.goto('/')
  await page.getByTitle('Work with the tutor').click()
  await expect(page.locator('.turn-tutor').first()).toBeVisible()
}

/** The exercise the tutor is currently waiting on, identified rather than read as prose. */
async function openExerciseId(page: Page): Promise<string | null> {
  return page.locator('.turn-tutor').last().getAttribute('data-exercise-id')
}

async function send(page: Page, text: string) {
  await page.getByLabel('Message the teacher').fill(text)
  await page.getByRole('button', { name: /^Send$/ }).click()
}

test.describe('the tutor', () => {
  test('opens by finding out what the learner knows, not by announcing a lesson', async ({ page }) => {
    await openTutor(page)

    const first = await page.locator('.turn-tutor').first().innerText()
    expect(first.toLowerCase()).not.toMatch(/chapter|module|curriculum|unit \d/)
    await expect(page.locator('.turn-meta').first()).toHaveText(/diagnose/i)
    await expect(page.getByLabel('Message the teacher')).toBeEnabled()
  })

  test('always leaves the learner something to answer', async ({ page }) => {
    await openTutor(page)

    // Six wrong answers in a row. The tutor must still be asking something at the end:
    // a turn with nothing to answer produces no evidence and the loop stops moving.
    for (let i = 0; i < 6; i += 1) {
      await send(page, 'not the answer')
      await expect(page.locator('.verdict').nth(i)).toBeVisible()
    }

    expect(await openExerciseId(page)).not.toBeNull()
    const evidence = await (await page.request.get(`${API}/learner/evidence`)).json()
    expect(evidence.length).toBeGreaterThanOrEqual(6)
  })

  test('marks a wrong answer wrong and says what was expected', async ({ page }) => {
    await openTutor(page)

    await send(page, 'completely wrong')

    await expect(page.locator('.verdict-incorrect').first()).toBeVisible()
    await expect(page.locator('.verdict-incorrect').first()).toContainText(/expected/i)
  })

  test('treats asking for help as a question, not as a wrong answer', async ({ page }) => {
    await openTutor(page)
    const question = await openExerciseId(page)
    expect(question).not.toBeNull()

    await send(page, 'explain')
    await expect(page.locator('.turn-tutor')).toHaveCount(2)

    // Nothing was graded, and the very same question is still on the table.
    await expect(page.locator('.verdict')).toHaveCount(0)
    expect(await openExerciseId(page)).toBe(question)

    const evidence = await (await page.request.get(`${API}/learner/evidence`)).json()
    expect(evidence).toHaveLength(0)
  })

  test('takes up what the learner asks about instead of ploughing on', async ({ page }) => {
    await openTutor(page)

    await send(page, 'what is a C major add 7 chord')
    await expect(page.locator('.turn-tutor')).toHaveCount(2)

    // The reply must show it heard the question, not silently carry on with its own plan.
    await expect(page.locator('.turn-tutor').last()).toContainText(/seventh chord/i)
  })

  test('shows the learner model filling in as evidence arrives', async ({ page }) => {
    await openTutor(page)
    await send(page, 'not the answer')
    await expect(page.locator('.verdict').first()).toBeVisible()

    // Progress is its own section now, and it fills in from the answers given.
    await page.getByTitle('What the tutor believes you know').click()
    await expect(page.getByRole('heading', { name: 'Progress' })).toBeVisible()
    await expect(page.locator('.progress-table').first()).toBeVisible()
  })

  test('can be answered on the on-screen piano when no instrument is connected', async ({ page }) => {
    await resetLearner(page)
    // Ask for keyboard practice, so the tutor sets a question that must be played.
    await page.request.put(`${API}/learner/practice-mode/play`)
    await page.goto('/')
    await page.getByTitle('Work with the tutor').click()
    await expect(page.locator('.turn-tutor').first()).toBeVisible()

    // With no MIDI device, the on-screen keys are offered without being asked for.
    await expect(page.locator('.virtual-keyboard')).toBeVisible()

    // "Play F3 on the keyboard." — answer it by clicking exactly that key.
    const asked = await page.locator('.turn-tutor').last().innerText()
    const note = asked.match(/\b([A-G](?:#|b)?[0-5])\b/)
    expect(note, `no note named in: ${asked}`).not.toBeNull()

    await page.getByLabel(note![1], { exact: true }).click()
    await expect(page.locator('.vk-notes')).toHaveText(note![1])
    await page.getByRole('button', { name: 'Play it' }).click()

    await expect(page.locator('.verdict').first()).toBeVisible()
    await expect(page.locator('.verdict-correct').first()).toBeVisible()

    const evidence = await (await page.request.get(`${API}/learner/evidence`)).json()
    expect(evidence[0].evidenceType).toContain('MIDI')
    expect(evidence[0].result).toBe('CORRECT')
  })

  test('says that answers can be played on screen when no instrument is attached', async ({ page }) => {
    await openTutor(page)
    await expect(page.locator('.pill').last()).toHaveText(/on-screen piano/i)
  })

  test('teaches a topic before asking about it', async ({ page }) => {
    await resetLearner(page)
    await page.goto('/')
    await expect(page.getByRole('heading', { name: 'Learn' })).toBeVisible()

    await page.getByRole('button', { name: 'Read' }).first().click()
    await expect(page.locator('.lesson-section').first()).toBeVisible()
    // A lesson explains and shows a worked example, before anything is asked.
    await expect(page.locator('.lesson-section li').first()).not.toBeEmpty();
    await expect(page.locator('.example').first()).toBeVisible()

    await page.getByRole('button', { name: 'Practise this' }).click()
    await expect(page.locator('.turn-tutor').first()).toBeVisible()
  })
})
