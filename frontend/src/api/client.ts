import { z } from 'zod'
import {
  availableModels,
  evidenceRow,
  exampleSet,
  ingestReport,
  knowledgeSource,
  knowledgeStatus,
  learnerSnapshot,
  lesson,
  settings,
  teachingDecision,
  tutorStatus,
  tutorTurn,
  type EvidenceRow,
  type ExampleSet,
  type IngestReport,
  type KnowledgeSource,
  type KnowledgeStatus,
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

  resetLearner: (): Promise<LearnerSnapshot> =>
    request('/learner', learnerSnapshot, { method: 'DELETE' }),

  knowledgeStatus: (): Promise<KnowledgeStatus> => request('/knowledge/status', knowledgeStatus),

  knowledgeSources: (): Promise<KnowledgeSource[]> =>
    request('/knowledge/sources', z.array(knowledgeSource)),

  // Reaches the network and takes minutes, so it is always something a person asked for,
  // never something that happens on its own at startup.
  ingestSource: (id: string): Promise<IngestReport> =>
    request(`/knowledge/sources/${id}/ingest`, ingestReport, { method: 'POST' }),

  // Real examples from annotated scores. An empty list is a real answer, and the UI says so
  // rather than hiding it: nothing is generated to fill the gap.
  examplesForConcept: (conceptId: string, limit = 2): Promise<ExampleSet> =>
    request(`/knowledge/examples/for-concept/${conceptId}?limit=${limit}`, exampleSet),
}
