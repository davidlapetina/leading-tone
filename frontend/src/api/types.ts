import { z } from 'zod'

/**
 * The API's shapes, validated at the boundary. The learner model is the point of this
 * application, so a silently-changed field should fail loudly rather than render as NaN.
 */

export const evaluationOutcome = z.object({
  result: z.enum(['CORRECT', 'PARTIALLY_CORRECT', 'INCORRECT', 'SKIPPED']),
  feedback: z.string().nullable(),
  detail: z.string().nullable(),
  misconceptionCode: z.string().nullable(),
  misconceptionDescription: z.string().nullable(),
  confidence: z.number(),
  requiresModelJudgement: z.boolean(),
})

export const attemptResult = z.object({
  outcome: evaluationOutcome,
  conceptId: z.string(),
  masteryBefore: z.number(),
  masteryAfter: z.number(),
  state: z.string(),
  evidenceRecorded: z.boolean(),
})

export const tutorTurn = z.object({
  sessionId: z.string(),
  interactionId: z.string(),
  message: z.string(),
  action: z.string(),
  conceptId: z.string(),
  conceptName: z.string(),
  rationale: z.string(),
  difficulty: z.number(),
  expectsAnswer: z.boolean(),
  answerMode: z.enum(['TEXT', 'MIDI', 'MULTIPLE_CHOICE', 'NONE']),
  exerciseId: z.string().nullable(),
  exercisePrompt: z.string().nullable(),
  taskKind: z.string().nullable(),
  notationAbc: z.string().nullable(),
  attempt: attemptResult.nullable(),
  narrator: z.string(),
})

export const conceptMastery = z.object({
  conceptId: z.string(),
  name: z.string(),
  category: z.string(),
  mastery: z.number(),
  confidence: z.number(),
  state: z.enum([
    'UNKNOWN',
    'INTRODUCED',
    'LEARNING',
    'PRACTICING',
    'RELIABLE',
    'MASTERED',
    'NEEDS_REVIEW',
  ]),
  successfulEvidence: z.number(),
  failedEvidence: z.number(),
  lastPracticedAt: z.string().nullable(),
  nextReviewAt: z.string().nullable(),
})

export const misconceptionView = z.object({
  conceptId: z.string(),
  code: z.string(),
  description: z.string(),
  occurrences: z.number(),
  lastSeenAt: z.string(),
})

export const learnerSnapshot = z.object({
  learnerId: z.string(),
  displayName: z.string(),
  concepts: z.array(conceptMastery),
  dueForReview: z.array(conceptMastery),
  openMisconceptions: z.array(misconceptionView),
  preferences: z.record(z.string(), z.number()),
  preferredAnswerMode: z.enum(['TEXT', 'MIDI']).nullable(),
})

export const tutorStatus = z.object({
  narrator: z.string(),
  languageModelAvailable: z.boolean(),
  conceptCount: z.number(),
})

export type EvaluationOutcome = z.infer<typeof evaluationOutcome>
export type AttemptResult = z.infer<typeof attemptResult>
export type TutorTurn = z.infer<typeof tutorTurn>
export type ConceptMastery = z.infer<typeof conceptMastery>
export type MisconceptionView = z.infer<typeof misconceptionView>
export type LearnerSnapshot = z.infer<typeof learnerSnapshot>
export type TutorStatus = z.infer<typeof tutorStatus>
