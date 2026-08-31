import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from '../../api/client'
import { noteName, useTutorStore } from '../tutorStore'
import type { TutorTurn } from '../../api/types'

vi.mock('../../api/client', () => ({
  api: {
    startSession: vi.fn(),
    sendMessage: vi.fn(),
    answerWithText: vi.fn(),
    answerWithMidi: vi.fn(),
  },
  ApiError: class extends Error {},
}))

const turn = (overrides: Partial<TutorTurn> = {}): TutorTurn => ({
  sessionId: 'session-1',
  interactionId: 'interaction-1',
  message: 'Play the dominant seventh of D major.',
  action: 'PRACTICE',
  conceptId: 'dominant-seventh',
  conceptName: 'The dominant seventh',
  rationale: 'because',
  difficulty: 0.6,
  expectsAnswer: true,
  answerMode: 'MIDI',
  exerciseId: 'exercise-1',
  exercisePrompt: 'Play the dominant seventh of D major.',
  taskKind: 'produce it',
  notationAbc: null,
  attempt: null,
  narrator: 'template',
  ...overrides,
})

describe('tutor store', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useTutorStore.setState({
      sessionId: null,
      entries: [],
      current: null,
      busy: false,
      error: null,
    })
  })

  it('starts a session and shows the opening turn', async () => {
    vi.mocked(api.startSession).mockResolvedValue(turn())
    await useTutorStore.getState().start()

    const state = useTutorStore.getState()
    expect(state.sessionId).toBe('session-1')
    expect(state.entries).toHaveLength(1)
    expect(state.entries[0].role).toBe('tutor')
    expect(state.busy).toBe(false)
  })

  it('routes a typed reply to the open exercise, not to the chat endpoint', async () => {
    vi.mocked(api.startSession).mockResolvedValue(turn())
    vi.mocked(api.answerWithText).mockResolvedValue(turn({ message: 'next' }))
    await useTutorStore.getState().start()

    await useTutorStore.getState().send('A7')

    expect(api.answerWithText).toHaveBeenCalledWith('exercise-1', 'A7')
    expect(api.sendMessage).not.toHaveBeenCalled()
  })

  it('routes a question to the chat endpoint when nothing is being asked', async () => {
    vi.mocked(api.startSession).mockResolvedValue(
      turn({ expectsAnswer: false, exerciseId: null, answerMode: 'NONE' }),
    )
    vi.mocked(api.sendMessage).mockResolvedValue(turn())
    await useTutorStore.getState().start()

    await useTutorStore.getState().send('why does it resolve?')

    expect(api.sendMessage).toHaveBeenCalledWith('session-1', 'why does it resolve?')
    expect(api.answerWithText).not.toHaveBeenCalled()
  })

  it('records what was played in the transcript', async () => {
    vi.mocked(api.startSession).mockResolvedValue(turn())
    vi.mocked(api.answerWithMidi).mockResolvedValue(turn({ message: 'Correct.' }))
    await useTutorStore.getState().start()

    await useTutorStore.getState().play([57, 61, 64, 67])

    expect(api.answerWithMidi).toHaveBeenCalledWith('exercise-1', [57, 61, 64, 67])
    const learnerEntry = useTutorStore.getState().entries.find((entry) => entry.role === 'learner')
    expect(learnerEntry?.text).toBe('played A3 C#4 E4 G4')
  })

  it('surfaces a failure instead of losing the turn silently', async () => {
    vi.mocked(api.startSession).mockRejectedValue(new Error('backend is down'))
    await useTutorStore.getState().start()

    expect(useTutorStore.getState().error).toBe('backend is down')
    expect(useTutorStore.getState().busy).toBe(false)
  })

  it('names notes the way a musician would read them', () => {
    expect(noteName(60)).toBe('C4')
    expect(noteName(69)).toBe('A4')
    expect(noteName(61)).toBe('C#4')
  })
})
