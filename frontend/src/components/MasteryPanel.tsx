import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { ConceptMastery, LearnerSnapshot } from '../api/types'
import { MasteryBar } from './MasteryBar'
import { PracticeMenu } from './PracticeMenu'

/** The order harmony is actually built in, so the panel reads as a route rather than a list. */
const AREAS: { key: string; title: string }[] = [
  { key: 'FUNDAMENTALS', title: 'Fundamentals' },
  { key: 'SCALES', title: 'Scales & keys' },
  { key: 'CHORDS', title: 'Chords' },
  { key: 'HARMONY', title: 'Harmony' },
  { key: 'VOICE_LEADING', title: 'Voice leading' },
  { key: 'FORM', title: 'Form' },
]

const STATE_LABEL: Record<string, string> = {
  UNKNOWN: 'new',
  INTRODUCED: 'just met',
  LEARNING: 'learning',
  PRACTICING: 'practising',
  RELIABLE: 'reliable',
  MASTERED: 'solid',
  NEEDS_REVIEW: 'due',
}

/**
 * What the tutor believes, why it is asking what it is asking, and what to work on.
 *
 * Every area and every concept is a control: click one and the tutor works on it. That is
 * free mode. It never lets the learner mark their own work — only choose the subject.
 */
export function MasteryPanel({ version, rationale }: { version: number; rationale?: string }) {
  const queryClient = useQueryClient()
  const { data, isLoading, error } = useQuery({ queryKey: ['learner', version], queryFn: api.learner })

  const refresh = { onSuccess: () => queryClient.invalidateQueries({ queryKey: ['learner'] }) }
  const pickConcept = useMutation({ mutationFn: api.focusConcept, ...refresh })
  const pickArea = useMutation({ mutationFn: api.focusCategory, ...refresh })
  const guided = useMutation({ mutationFn: api.clearFocus, ...refresh })

  if (isLoading) {
    return <aside className="panel">Reading the learner model…</aside>
  }
  if (error || !data) {
    return <aside className="panel panel-error">The learner model is unavailable.</aside>
  }

  const snapshot: LearnerSnapshot = data
  const free = Boolean(snapshot.focusConceptId ?? snapshot.focusCategory)
  const inArea = (key: string) => snapshot.concepts.filter((concept) => concept.category === key)
  const met = (concepts: ConceptMastery[]) => concepts.filter((c) => c.state !== 'UNKNOWN')

  return (
    <aside className="panel">
      <section className="card">
        <h2>What to work on</h2>
        <div className="mode-row">
          <button
            type="button"
            className={`chip${free ? '' : ' chip-on'}`}
            aria-pressed={!free}
            onClick={() => guided.mutate()}
          >
            Guided
          </button>
          <span className="chip-note">
            {free ? 'You picked the topic' : 'The tutor picks, from what you know'}
          </span>
        </div>
        {free && (
          <p className="focus-now">
            Working on{' '}
            <strong>
              {snapshot.focusConceptId
                ? (snapshot.concepts.find((c) => c.conceptId === snapshot.focusConceptId)?.name ??
                  snapshot.focusConceptId)
                : (AREAS.find((a) => a.key === snapshot.focusCategory)?.title ?? snapshot.focusCategory)}
            </strong>
          </p>
        )}
      </section>

      <section className="card">
        <h2>How to practise</h2>
        <PracticeMenu current={snapshot.preferredAnswerMode} />
      </section>

      {rationale && (
        <section className="card card-quiet">
          <h2>Why this now</h2>
          <p className="rationale">{rationale}</p>
        </section>
      )}

      <section className="card">
        <h2>Where you are</h2>
        <p className="hint">Click any topic to work on it.</p>
        {AREAS.map(({ key, title }) => {
          const concepts = inArea(key)
          if (concepts.length === 0) {
            return null
          }
          const chosenArea = snapshot.focusCategory === key
          return (
            <div className="area" key={key}>
              <button
                type="button"
                className={`area-head${chosenArea ? ' area-chosen' : ''}`}
                aria-pressed={chosenArea}
                onClick={() => pickArea.mutate(key)}
              >
                <span>{title}</span>
                <span className="count">
                  {met(concepts).length}/{concepts.length}
                </span>
              </button>
              <ul className="concepts">
                {concepts.map((concept) => (
                  <li key={concept.conceptId}>
                    <button
                      type="button"
                      className={`concept-row state-${concept.state.toLowerCase()}${
                        snapshot.focusConceptId === concept.conceptId ? ' concept-chosen' : ''
                      }`}
                      aria-pressed={snapshot.focusConceptId === concept.conceptId}
                      onClick={() => pickConcept.mutate(concept.conceptId)}
                    >
                      <span className="concept-name">{concept.name}</span>
                      <span className="concept-state">{STATE_LABEL[concept.state]}</span>
                      <MasteryBar value={concept.mastery} state={concept.state} />
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )
        })}
      </section>

      {snapshot.dueForReview.length > 0 && (
        <section className="card">
          <h2>Due again</h2>
          <ul className="plain">
            {snapshot.dueForReview.map((concept) => (
              <li key={concept.conceptId}>{concept.name}</li>
            ))}
          </ul>
        </section>
      )}

      {snapshot.openMisconceptions.length > 0 && (
        <section className="card">
          <h2>Watching for</h2>
          <ul className="plain">
            {snapshot.openMisconceptions.map((misconception) => (
              <li key={misconception.code}>
                {misconception.description}
                <span className="count">seen {misconception.occurrences}×</span>
              </li>
            ))}
          </ul>
        </section>
      )}

      <p className="saved">Progress is saved to PostgreSQL as you go.</p>
    </aside>
  )
}
