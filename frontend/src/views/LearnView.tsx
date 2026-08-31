import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { ConceptMastery } from '../api/types'

const AREAS: { key: string; title: string; blurb: string }[] = [
  { key: 'FUNDAMENTALS', title: 'Fundamentals', blurb: 'Notes, the keyboard, and how far apart things are' },
  { key: 'SCALES', title: 'Scales & keys', blurb: 'The patterns everything else is built from' },
  { key: 'CHORDS', title: 'Chords', blurb: 'Stacking thirds, and what comes of it' },
  { key: 'HARMONY', title: 'Harmony', blurb: 'What chords do to each other, from I-IV-V to altered dominants' },
  { key: 'VOICE_LEADING', title: 'Voice leading', blurb: 'Moving between chords, and writing two lines at once' },
  { key: 'FORM', title: 'Form', blurb: 'Cadences, the blues, and changing key' },
]

const STATE_LABEL: Record<string, string> = {
  UNKNOWN: 'Not started',
  INTRODUCED: 'Just started',
  LEARNING: 'Learning',
  PRACTICING: 'Practising',
  RELIABLE: 'Reliable',
  MASTERED: 'Solid',
  NEEDS_REVIEW: 'Due for review',
}

interface LearnViewProps {
  onOpen: (conceptId: string) => void
  onPractise: (conceptId: string) => void
}

/** The catalogue: what there is to learn, where you are in it, and what to read next. */
export function LearnView({ onOpen, onPractise }: LearnViewProps) {
  const learner = useQuery({ queryKey: ['learner'], queryFn: api.learner })
  const next = useQuery({ queryKey: ['next-action'], queryFn: api.nextAction })

  if (learner.isLoading) {
    return <div className="view">Loading…</div>
  }
  if (learner.error || !learner.data) {
    return <div className="view view-error">The learner model is unavailable.</div>
  }

  const concepts = learner.data.concepts
  const started = concepts.filter((concept) => concept.state !== 'UNKNOWN')
  const percent = Math.round((started.length / Math.max(concepts.length, 1)) * 100)

  return (
    <div className="view">
      <header className="view-head">
        <div>
          <h1>Learn</h1>
          <p className="view-sub">
            Read a topic, then work on it with the tutor. Nothing is locked — the tutor will
            tell you if something needs groundwork first.
          </p>
        </div>
        <div className="overall">
          <span className="overall-figure">
            {started.length}
            <span className="overall-of">/{concepts.length}</span>
          </span>
          <span className="overall-label">topics begun</span>
          <div className="bar-track">
            <div className="bar-fill" style={{ width: `${percent}%` }} />
          </div>
        </div>
      </header>

      {next.data && (
        <section className="continue-card">
          <div>
            <span className="tag tag-accent">Suggested next</span>
            <h2>{next.data.conceptName}</h2>
            <p>{next.data.rationale}</p>
          </div>
          <div className="continue-actions">
            <button type="button" className="btn-ghost" onClick={() => onOpen(next.data.conceptId)}>
              Read it
            </button>
            <button type="button" className="btn-primary" onClick={() => onPractise(next.data.conceptId)}>
              Continue
            </button>
          </div>
        </section>
      )}

      {AREAS.map(({ key, title, blurb }) => {
        const inArea = concepts.filter((concept) => concept.category === key)
        if (inArea.length === 0) {
          return null
        }
        return (
          <section className="area-block" key={key}>
            <div className="area-title">
              <h2>{title}</h2>
              <p>{blurb}</p>
            </div>
            <div className="card-grid">
              {inArea.map((concept) => (
                <ConceptCard
                  key={concept.conceptId}
                  concept={concept}
                  onOpen={() => onOpen(concept.conceptId)}
                  onPractise={() => onPractise(concept.conceptId)}
                />
              ))}
            </div>
          </section>
        )
      })}
    </div>
  )
}

function ConceptCard({
  concept,
  onOpen,
  onPractise,
}: {
  concept: ConceptMastery
  onOpen: () => void
  onPractise: () => void
}) {
  const percent = Math.round(concept.mastery * 100)
  return (
    <article className={`concept-card state-${concept.state.toLowerCase()}`}>
      <div className="concept-card-top">
        <span className={`tag tag-${concept.state.toLowerCase()}`}>{STATE_LABEL[concept.state]}</span>
        {concept.successfulEvidence + concept.failedEvidence > 0 && (
          <span className="answered">{concept.successfulEvidence + concept.failedEvidence} answered</span>
        )}
      </div>
      <h3>{concept.name}</h3>
      <div className="bar-track">
        <div className="bar-fill" style={{ width: `${percent}%` }} />
      </div>
      <div className="concept-card-actions">
        <button type="button" className="btn-ghost" onClick={onOpen}>
          Read
        </button>
        <button type="button" className="btn-primary" onClick={onPractise}>
          Practise
        </button>
      </div>
    </article>
  )
}
