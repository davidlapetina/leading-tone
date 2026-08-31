import { z } from 'zod'
import {
  availableModels,
  evidenceRow,
  learnerSnapshot,
  lesson,
  settings,
  teachingDecision,
  tutorStatus,
  tutorTurn,
  type EvidenceRow,
  type LearnerSnapshot,
  type Settings,
  type Lesson,
  type TeachingDecision,
  type TutorStatus,
  type TutorTurn,
} from './types'
import type { ZodType } from 'zod'

const BASE = import.meta.env.VITE_API_BASE ?? '/api'

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, schema: ZodType<T>, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE}${path}`, {
    ...init,
    headers: init?.body ? { 'content-type': 'application/json', ...init?.headers } : init?.headers,
  })

  if (!response.ok) {
    const detail = await response.text().catch(() => '')
    throw new ApiError(detail || `${response.status} ${response.statusText}`, response.status)
  }

  const parsed = schema.safeParse(await response.json())
  if (!parsed.success) {
    throw new ApiError(`Unexpected response from ${path}: ${parsed.error.message}`, 500)
  }
  return parsed.data
}

export const api = {
  startSession: () => request('/session', tutorTurn, { method: 'POST' }),

  sendMessage: (sessionId: string, message: string): Promise<TutorTurn> =>
    request(`/session/${sessionId}/message`, tutorTurn, {
      method: 'POST',
      body: JSON.stringify({ message }),
    }),

  answerWithText: (exerciseId: string, answer: string): Promise<TutorTurn> =>
    request(`/exercises/${exerciseId}/answer`, tutorTurn, {
      method: 'POST',
      body: JSON.stringify({ answer }),
    }),

  answerWithMidi: (exerciseId: string, notes: number[]): Promise<TutorTurn> =>
    request(`/exercises/${exerciseId}/midi`, tutorTurn, {
      method: 'POST',
      body: JSON.stringify({ notes }),
    }),

  learner: (): Promise<LearnerSnapshot> => request('/learner', learnerSnapshot),

  setPracticeMode: (mode: 'play' | 'write' | 'auto'): Promise<LearnerSnapshot> =>
    request(`/learner/practice-mode/${mode}`, learnerSnapshot, { method: 'PUT' }),

  focusConcept: (conceptId: string): Promise<LearnerSnapshot> =>
    request(`/learner/focus/concept/${conceptId}`, learnerSnapshot, { method: 'PUT' }),

  focusCategory: (category: string): Promise<LearnerSnapshot> =>
    request(`/learner/focus/category/${category}`, learnerSnapshot, { method: 'PUT' }),

  clearFocus: (): Promise<LearnerSnapshot> =>
    request('/learner/focus', learnerSnapshot, { method: 'DELETE' }),

  status: (): Promise<TutorStatus> => request('/session/status', tutorStatus),

  lesson: (conceptId: string): Promise<Lesson> => request(`/concepts/${conceptId}/lesson`, lesson),

  nextAction: (): Promise<TeachingDecision> => request('/session/next-action', teachingDecision),

  evidence: (limit = 25): Promise<EvidenceRow[]> =>
    request(`/learner/evidence?limit=${limit}`, z.array(evidenceRow)),

  settings: (): Promise<Settings> => request('/settings', settings),

  saveSettings: (change: Partial<Settings>): Promise<Settings> =>
    request('/settings', settings, { method: 'PUT', body: JSON.stringify(change) }),

  resetSettings: (): Promise<Settings> =>
    request('/settings/reset', settings, { method: 'POST', body: '{}' }),

  availableModels: () => request('/settings/models', availableModels),
}
