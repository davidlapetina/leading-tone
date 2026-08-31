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

/**
 * The jazz path, in the order it is usually learned rather than by category.
 *
 * <p>Jazz shares most of its theory with everything else, so its concepts live in the same
 * graph and appear in the areas above too. What is different is the route through them, and
 * that route is worth showing on its own to somebody who came here for jazz.
 */
const JAZZ_THEMES: { title: string; blurb: string; concepts: string[] }[] = [
  {
    title: 'Reading the language',
    blurb: 'A lead sheet names chords and leaves the rest to you.',
    concepts: ['chord-symbol', 'extended-chord', 'jazz-voicing'],
  },
  {
    title: 'The progression everything is built on',
    blurb: 'Two-five-one, and the turnarounds that carry you back to the top.',
    concepts: ['two-five-one', 'turnaround', 'altered-dominant', 'tritone-substitution'],
  },
  {
    title: 'Playing over changes',
    blurb: 'Choosing notes once you know what the chord is.',
    concepts: ['chord-scale-theory', 'blues-form', 'blues-scale'],
  },
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

      <JazzPath concepts={concepts} onOpen={onOpen} onPractise={onPractise} />

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

/**
 * A route through jazz harmony, for somebody who came here for that.
 *
 * <p>These concepts are not separate from the rest of the graph — jazz shares almost all of
 * its theory with everything else, and pretending otherwise would teach it badly. What is
 * genuinely different is the order and the emphasis, so this offers the route while the
 * concepts stay where they are in the areas below.
 */
function JazzPath({
  concepts,
  onOpen,
  onPractise,
}: {
  concepts: ConceptMastery[]
  onOpen: (conceptId: string) => void
  onPractise: (conceptId: string) => void
}) {
  const byId = new Map(concepts.map((concept) => [concept.conceptId, concept]))
  const jazz = concepts.filter((concept) => concept.tradition === 'JAZZ')
  if (jazz.length === 0) {
    return null
  }
  const done = jazz.filter((concept) => concept.mastery >= 0.45).length
  const percent = Math.round((done / jazz.length) * 100)

  return (
    <section className="area-block jazz-path">
      <div className="area-title">
        <h2>Jazz harmony</h2>
        <p>
          A route through the same theory, in the order a jazz musician meets it. {done} of{' '}
          {jazz.length} solid.
        </p>
        <div className="jazz-progress" role="img" aria-label={`${percent}% of the jazz path solid`}>
          <span style={{ width: `${percent}%` }} />
        </div>
      </div>

      {JAZZ_THEMES.map((theme) => {
        const inTheme = theme.concepts
          .map((id) => byId.get(id))
          .filter((concept): concept is ConceptMastery => concept !== undefined)
        if (inTheme.length === 0) {
          return null
        }
        return (
          <div className="jazz-theme" key={theme.title}>
            <h3>{theme.title}</h3>
            <p className="jazz-theme-blurb">{theme.blurb}</p>
            <div className="card-grid">
              {inTheme.map((concept) => (
                <ConceptCard
                  key={concept.conceptId}
                  concept={concept}
                  onOpen={() => onOpen(concept.conceptId)}
                  onPractise={() => onPractise(concept.conceptId)}
                />
              ))}
            </div>
          </div>
        )
      })}
    </section>
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
