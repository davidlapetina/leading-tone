import { create } from 'zustand'
import { api } from '../api/client'
import type { AttemptResult, TutorTurn } from '../api/types'

export interface ConversationEntry {
  id: string
  role: 'tutor' | 'learner'
  text: string
  notationAbc?: string | null
  attempt?: AttemptResult | null
  action?: string
  rationale?: string
  /** The generated question, which is authoritative even if the model paraphrased it. */
  exercisePrompt?: string | null
  /** The open exercise this turn is waiting on, if any. */
  exerciseId?: string | null
  /** What the learner is being asked to do: recognise, produce, explain. */
  taskKind?: string | null
}

interface TutorState {
  sessionId: string | null
  entries: ConversationEntry[]
  current: TutorTurn | null
  busy: boolean
  error: string | null

  start: () => Promise<void>
  send: (text: string) => Promise<void>
  play: (notes: number[]) => Promise<void>
  clearError: () => void
}

let counter = 0
const nextId = () => `entry-${counter++}`

function noteName(midi: number): string {
  const names = ['C', 'C#', 'D', 'Eb', 'E', 'F', 'F#', 'G', 'Ab', 'A', 'Bb', 'B']
  return `${names[((midi % 12) + 12) % 12]}${Math.floor(midi / 12) - 1}`
}

export const useTutorStore = create<TutorState>((set, get) => {
  /** Appends the tutor's turn, and the verdict on the answer that led to it. */
  const acceptTurn = (turn: TutorTurn) => {
    set((state) => ({
      current: turn,
      busy: false,
      entries: [
        ...state.entries,
        {
          id: nextId(),
          role: 'tutor',
          text: turn.message,
          notationAbc: turn.notationAbc,
          attempt: turn.attempt,
          action: turn.action,
          rationale: turn.rationale,
          exercisePrompt: turn.exercisePrompt,
          exerciseId: turn.exerciseId,
          taskKind: turn.taskKind,
        },
      ],
    }))
  }

  const fail = (error: unknown) =>
    set({ busy: false, error: error instanceof Error ? error.message : String(error) })

  return {
    sessionId: null,
    entries: [],
    current: null,
    busy: false,
    error: null,

    start: async () => {
      set({ busy: true, error: null })
      try {
        const turn = await api.startSession()
        set({ sessionId: turn.sessionId, entries: [] })
        acceptTurn(turn)
      } catch (error) {
        fail(error)
      }
    },

    send: async (text: string) => {
      const { sessionId, current } = get()
      if (!sessionId || !text.trim()) {
        return
      }
      set((state) => ({
        busy: true,
        error: null,
        entries: [...state.entries, { id: nextId(), role: 'learner', text }],
      }))
      try {
        // A typed reply answers the open exercise; anything else is just a question.
        const turn =
          current?.expectsAnswer && current.exerciseId
            ? await api.answerWithText(current.exerciseId, text)
            : await api.sendMessage(sessionId, text)
        acceptTurn(turn)
      } catch (error) {
        fail(error)
      }
    },

    play: async (notes: number[]) => {
      const { current } = get()
      if (!current?.exerciseId || notes.length === 0) {
        return
      }
      set((state) => ({
        busy: true,
        error: null,
        entries: [
          ...state.entries,
          { id: nextId(), role: 'learner', text: `played ${notes.map(noteName).join(' ')}` },
        ],
      }))
      try {
        acceptTurn(await api.answerWithMidi(current.exerciseId, notes))
      } catch (error) {
        fail(error)
      }
    },

    clearError: () => set({ error: null }),
  }
})

export { noteName }
