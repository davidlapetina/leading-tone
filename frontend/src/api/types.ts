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
  tradition: z.enum(['GENERAL', 'CLASSICAL', 'JAZZ']),
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
  knowledgeEnabled: z.boolean(),
  runtimeMode: z.enum(['NON_COMMERCIAL', 'COMMERCIAL']),
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

/**
 * A knowledge source and what has happened to it here. The licence fields are not
 * decoration: this application is MIT, and none of these sources are, so the terms travel
 * with the material and are shown at the point where someone chooses to bring it in.
 */
export const knowledgeSource = z.object({
  id: z.string(),
  name: z.string(),
  url: z.string().nullable(),
  ingestionMode: z.enum(['TEXT_RAG', 'STRUCTURED_HARMONY']),
  tradition: z.string().nullable(),
  enabled: z.boolean(),
  license: z.string(),
  licenseName: z.string(),
  licenseUrl: z.string().nullable(),
  licenseStatus: z.enum(['VERIFIED', 'RESTRICTED', 'UNKNOWN', 'REJECTED']),
  attributionRequired: z.boolean(),
  shareAlikeRequired: z.boolean(),
  commercialUseAllowed: z.boolean(),
  citation: z.string().nullable(),
  state: z.string(),
  retrievable: z.boolean(),
  documents: z.number(),
  chunks: z.number(),
  lastIngestedAt: z.string().nullable(),
  lastError: z.string().nullable(),
})

export const knowledgeStatus = z.object({
  indexOpen: z.boolean(),
  indexGeneration: z.number(),
  embeddingModel: z.string(),
  vectorSearch: z.boolean(),
  documents: z.number(),
  chunks: z.number(),
  harmonyEvents: z.number(),
  declaredSources: z.number(),
  activeSources: z.number(),
  unavailable: z.string().optional(),
})

export const ingestReport = z.object({
  sourceId: z.string(),
  state: z.string(),
  skipped: z.boolean(),
  documentsSeen: z.number(),
  documentsIngested: z.number(),
  documentsSkippedLicense: z.number(),
  documentsSkippedEmpty: z.number(),
  chunksWritten: z.number(),
  skippedForLicense: z.array(z.string()),
  message: z.string().nullable(),
  success: z.boolean(),
})

export type KnowledgeSource = z.infer<typeof knowledgeSource>
export type KnowledgeStatus = z.infer<typeof knowledgeStatus>
export type IngestReport = z.infer<typeof ingestReport>

/**
 * A musical example, and where it came from.
 *
 * <p>{@code origin} is the field that matters. A generated example is a good teaching
 * device; one presented as Beethoven's when it is not is a lie a learner cannot catch.
 */
export const musicalExample = z.object({
  eventId: z.string().nullable(),
  sourceId: z.string(),
  origin: z.enum(['VERIFIED_CORPUS', 'GENERATED', 'USER_PROVIDED']),
  composer: z.string().nullable(),
  work: z.string().nullable(),
  movement: z.string().nullable(),
  measure: z.number().nullable(),
  citation: z.string(),
  romanNumeral: z.string().nullable(),
  globalKey: z.string().nullable(),
  abc: z.string().nullable(),
  attribution: z.string().nullable(),
  licenseId: z.string().nullable(),
})

export const exampleSet = z.object({
  conceptId: z.string().nullable(),
  found: z.number(),
  examples: z.array(musicalExample),
  note: z.string().nullable(),
})

export const computedFact = z.object({
  operation: z.string(),
  statement: z.string(),
  answer: z.string().nullable(),
})

export const passage = z.object({
  chunkId: z.string(),
  citation: z.string(),
  attribution: z.string().nullable(),
  license: z.string().nullable(),
  url: z.string().nullable(),
  excerpt: z.string(),
})

/** An answer, and everything it was built from, so a reader can check it rather than trust it. */
export const askAnswer = z.object({
  question: z.string(),
  answer: z.string(),
  conversationId: z.string(),
  answeredWithoutAModel: z.boolean(),
  computed: z.array(computedFact),
  passages: z.array(passage),
  examples: z.array(musicalExample),
  sources: z.array(z.object({
    sourceId: z.string().nullable().optional(),
    name: z.string().nullable().optional(),
    citation: z.string().nullable().optional(),
    licenseName: z.string().nullable().optional(),
    licenseUrl: z.string().nullable().optional(),
    url: z.string().nullable().optional(),
  }).passthrough()),
  corpusSearchedAndEmpty: z.boolean(),
})

export type ComputedFact = z.infer<typeof computedFact>
export type Passage = z.infer<typeof passage>
export type AskAnswer = z.infer<typeof askAnswer>
export type MusicalExample = z.infer<typeof musicalExample>
export type ExampleSet = z.infer<typeof exampleSet>
