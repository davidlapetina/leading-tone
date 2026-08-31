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
  consecutiveFailures: z.number(),
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
  focusConceptId: z.string().nullable(),
  focusCategory: z.string().nullable(),
})

export const lessonSection = z.object({
  heading: z.string(),
  points: z.array(z.string()),
  abc: z.string().nullable(),
  caption: z.string().nullable(),
})

export const conceptLink = z.object({
  conceptId: z.string(),
  name: z.string(),
  mastery: z.number(),
  known: z.boolean(),
})

export const lesson = z.object({
  conceptId: z.string(),
  name: z.string(),
  summary: z.string(),
  category: z.string(),
  sections: z.array(lessonSection),
  restsOn: z.array(conceptLink),
  opensUp: z.array(conceptLink),
  mastery: z.number(),
  state: z.string(),
  ready: z.boolean(),
})

export const teachingDecision = z.object({
  action: z.string(),
  conceptId: z.string(),
  conceptName: z.string(),
  rationale: z.string(),
  difficulty: z.number(),
})

export const evidenceRow = z.object({
  id: z.string(),
  conceptId: z.string(),
  evidenceType: z.string(),
  result: z.string(),
  difficulty: z.number(),
  weight: z.number(),
  masteryBefore: z.number(),
  masteryAfter: z.number(),
  source: z.string().nullable(),
  createdAt: z.string(),
})

export const settings = z.object({
  llmEnabled: z.boolean(),
  toolsEnabled: z.boolean(),
  model: z.string(),
  baseUrl: z.string(),
  temperature: z.number(),
  numCtx: z.number(),
  think: z.boolean(),
  timeoutSeconds: z.number(),
  cooldownSeconds: z.number(),
  memoryMessages: z.number(),
  learnerName: z.string(),
})

export const availableModels = z.object({
  reachable: z.boolean(),
  models: z.array(z.string()),
})

export const tutorStatus = z.object({
  narrator: z.string(),
  languageModelAvailable: z.boolean(),
  model: z.string(),
  toolsEnabled: z.boolean(),
  conceptCount: z.number(),
})

export type EvaluationOutcome = z.infer<typeof evaluationOutcome>
export type AttemptResult = z.infer<typeof attemptResult>
export type TutorTurn = z.infer<typeof tutorTurn>
export type ConceptMastery = z.infer<typeof conceptMastery>
export type MisconceptionView = z.infer<typeof misconceptionView>
export type LearnerSnapshot = z.infer<typeof learnerSnapshot>
export type TutorStatus = z.infer<typeof tutorStatus>
export type Lesson = z.infer<typeof lesson>
export type LessonSection = z.infer<typeof lessonSection>
export type TeachingDecision = z.infer<typeof teachingDecision>
export type EvidenceRow = z.infer<typeof evidenceRow>
export type Settings = z.infer<typeof settings>
