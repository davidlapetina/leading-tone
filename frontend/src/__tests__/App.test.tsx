import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { api } from '../api/client'
import { useTutorStore } from '../state/tutorStore'
import type { LearnerSnapshot, TutorTurn } from '../api/types'

vi.mock('../api/client', () => ({
  api: {
    startSession: vi.fn(),
    sendMessage: vi.fn(),
    answerWithText: vi.fn(),
    answerWithMidi: vi.fn(),
    learner: vi.fn(),
    status: vi.fn(),
  },
  ApiError: class extends Error {},
}))

vi.mock('../components/Score', () => ({ Score: () => <div data-testid="score" /> }))

const opening: TutorTurn = {
  sessionId: 'session-1',
  interactionId: 'interaction-1',
  message: 'Before anything else, let me find out where you are.',
  action: 'DIAGNOSE',
  conceptId: 'note',
  conceptName: 'Notes and the keyboard',
  rationale: 'Nothing is known about this learner yet.',
  difficulty: 0.35,
  expectsAnswer: true,
  answerMode: 'TEXT',
  exerciseId: 'exercise-1',
  exercisePrompt: 'Write another name for F#.',
  taskKind: 'recognise it',
  notationAbc: null,
  attempt: null,
  narrator: 'template',
}

const snapshot: LearnerSnapshot = {
  learnerId: 'learner-1',
  displayName: 'Student',
  concepts: [
    {
      conceptId: 'note',
      name: 'Notes and the keyboard',
      category: 'FUNDAMENTALS',
      mastery: 0.42,
      confidence: 0.5,
      state: 'LEARNING',
      successfulEvidence: 2,
      failedEvidence: 1,
      lastPracticedAt: null,
      nextReviewAt: null,
    },
  ],
  dueForReview: [],
  openMisconceptions: [],
  preferences: { keyboardPreference: 0.5 },
  preferredAnswerMode: null,
}

function renderApp() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <App />
    </QueryClientProvider>,
  )
}

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useTutorStore.setState({
      sessionId: null,
      entries: [],
      current: null,
      busy: false,
      error: null,
    })
    vi.mocked(api.startSession).mockResolvedValue(opening)
    vi.mocked(api.learner).mockResolvedValue(snapshot)
    vi.mocked(api.status).mockResolvedValue({
      narrator: 'template',
      languageModelAvailable: false,
      conceptCount: 22,
    })
  })

  it('opens a session and shows the tutor and the learner model together', async () => {
    renderApp()

    expect(await screen.findByText(opening.message)).toBeInTheDocument()
    expect(await screen.findByText('Write another name for F#.')).toBeInTheDocument()
    expect(await screen.findByText('Notes and the keyboard')).toBeInTheDocument()
    expect(api.startSession).toHaveBeenCalledTimes(1)
  })

  it('sends a typed answer to the open exercise', async () => {
    vi.mocked(api.answerWithText).mockResolvedValue({ ...opening, message: 'Correct.' })
    renderApp()
    await screen.findByText(opening.message)

    await userEvent.type(screen.getByLabelText('Message the teacher'), 'Gb')
    await userEvent.click(screen.getByRole('button', { name: 'Send' }))

    await waitFor(() => expect(api.answerWithText).toHaveBeenCalledWith('exercise-1', 'Gb'))
    expect(await screen.findByText('Gb')).toBeInTheDocument()
  })

  it('reports a backend failure rather than showing an empty page', async () => {
    vi.mocked(api.startSession).mockRejectedValue(new Error('backend is down'))
    renderApp()
    expect(await screen.findByRole('alert')).toHaveTextContent('backend is down')
  })

  it('says when there is no keyboard attached', async () => {
    renderApp()
    expect(await screen.findByText(/MIDI unavailable|No keyboard/)).toBeInTheDocument()
  })
})
