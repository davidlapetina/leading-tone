import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { ConceptMastery } from '../api/types'
import { MasteryBar } from './MasteryBar'
import { PracticeMenu } from './PracticeMenu'

/** The order harmony is actually built in, so the panel reads as a route rather than a list. */
const CATEGORIES: { key: string; title: string }[] = [
  { key: 'FUNDAMENTALS', title: 'Fundamentals' },
  { key: 'SCALES', title: 'Scales and keys' },
  { key: 'CHORDS', title: 'Chords' },
  { key: 'HARMONY', title: 'Harmony' },
  { key: 'VOICE_LEADING', title: 'Voice leading' },
  { key: 'FORM', title: 'Form' },
]

const STATE_LABEL: Record<string, string> = {
  UNKNOWN: 'not met',
  INTRODUCED: 'just met',
  LEARNING: 'learning',
  PRACTICING: 'practising',
  RELIABLE: 'reliable',
  MASTERED: 'solid',
  NEEDS_REVIEW: 'due',
}

/**
 * What the tutor believes, and why it is asking what it is asking.
 *
 * <p>Grouped by the part of theory each concept belongs to, so the learner can see where
 * they are in the subject rather than reading a flat list of twenty-two names. It stays
 * secondary to the conversation: no numbers, no percentages, nothing to optimise.
 */
export function MasteryPanel({ version, rationale }: { version: number; rationale?: string }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['learner', version],
    queryFn: api.learner,
  })

  if (isLoading) {
    return <aside className="panel">Reading the learner model…</aside>
  }
  if (error || !data) {
    return <aside className="panel panel-error">The learner model is unavailable.</aside>
  }

  const met = data.concepts.filter((concept) => concept.state !== 'UNKNOWN')
  const byCategory = (key: string) => data.concepts.filter((c) => c.category === key)
  const started = (concepts: ConceptMastery[]) =>
    concepts.filter((concept) => concept.state !== 'UNKNOWN')

  return (
    <aside className="panel">
      <section className="panel-block">
        <h2>How to practise</h2>
        <PracticeMenu current={data.preferredAnswerMode} />
      </section>

      {rationale && (
        <section className="panel-block">
          <h2>Why this now</h2>
          <p className="rationale">{rationale}</p>
        </section>
      )}

      <section className="panel-block">
        <h2>Where you are</h2>
        {met.length === 0 && (
          <p className="muted">Nothing observed yet. Answer something and this fills in.</p>
        )}
        {CATEGORIES.map(({ key, title }) => {
          const all = byCategory(key)
          const seen = started(all)
          if (all.length === 0) {
            return null
          }
          return (
            <div className="category" key={key}>
              <h3>
                {title}
                <span className="count">
                  {seen.length}/{all.length}
                </span>
              </h3>
              <ul className="concepts">
                {all.map((concept) => (
                  <li key={concept.conceptId} className={`state-${concept.state.toLowerCase()}`}>
                    <span className="concept-name" title={concept.name}>
                      {concept.name}
                    </span>
                    <span className="concept-state">{STATE_LABEL[concept.state]}</span>
                    <MasteryBar value={concept.mastery} state={concept.state} />
                  </li>
                ))}
              </ul>
            </div>
          )
        })}
      </section>

      {data.dueForReview.length > 0 && (
        <section className="panel-block">
          <h2>Due again</h2>
          <ul className="plain">
            {data.dueForReview.map((concept) => (
              <li key={concept.conceptId}>{concept.name}</li>
            ))}
          </ul>
        </section>
      )}

      {data.openMisconceptions.length > 0 && (
        <section className="panel-block">
          <h2>Watching for</h2>
          <ul className="plain">
            {data.openMisconceptions.map((misconception) => (
              <li key={misconception.code}>
                {misconception.description}
                <span className="count">seen {misconception.occurrences}×</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </aside>
  )
}
