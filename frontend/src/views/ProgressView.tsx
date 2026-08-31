import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'

const AREAS = [
  ['FUNDAMENTALS', 'Fundamentals'],
  ['SCALES', 'Scales & keys'],
  ['CHORDS', 'Chords'],
  ['HARMONY', 'Harmony'],
  ['VOICE_LEADING', 'Voice leading'],
  ['FORM', 'Form'],
] as const

const STATE_LABEL: Record<string, string> = {
  UNKNOWN: 'not started',
  INTRODUCED: 'just started',
  LEARNING: 'learning',
  PRACTICING: 'practising',
  RELIABLE: 'reliable',
  MASTERED: 'solid',
  NEEDS_REVIEW: 'due',
}

/**
 * What the tutor believes, and why. Every figure traces back to answers you actually gave,
 * which is the point of keeping the evidence rather than only the score.
 */
export function ProgressView({ onOpen }: { onOpen: (conceptId: string) => void }) {
  const learner = useQuery({ queryKey: ['learner'], queryFn: api.learner })
  const evidence = useQuery({ queryKey: ['evidence'], queryFn: () => api.evidence(25) })

  if (learner.isLoading || !learner.data) {
    return <div className="view">Loading…</div>
  }
  const snapshot = learner.data

  return (
    <div className="view">
      <header className="view-head">
        <div>
          <h1>Progress</h1>
          <p className="view-sub">
            Nothing here is a score you can raise by guessing. Playing a chord counts for more than
            naming it, and explaining it counts for more again.
          </p>
        </div>
        <a className="btn-ghost" href="/api/learner/export" download="leading-tone.json">
          Export everything
        </a>
      </header>

      {snapshot.openMisconceptions.length > 0 && (
        <section className="panel-card">
          <h2>Mistakes it is watching for</h2>
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

      {AREAS.map(([key, title]) => {
        const concepts = snapshot.concepts.filter((concept) => concept.category === key)
        if (concepts.length === 0) {
          return null
        }
        return (
          <section className="panel-card" key={key}>
            <h2>{title}</h2>
            <table className="progress-table">
              <tbody>
                {concepts.map((concept) => (
                  <tr key={concept.conceptId} className={`state-${concept.state.toLowerCase()}`}>
                    <td>
                      <button type="button" className="link" onClick={() => onOpen(concept.conceptId)}>
                        {concept.name}
                      </button>
                    </td>
                    <td className="num">{STATE_LABEL[concept.state]}</td>
                    <td className="num">
                      {concept.successfulEvidence}✓ {concept.failedEvidence}✗
                    </td>
                    <td className="bar-cell">
                      <div className="bar-track">
                        <div className="bar-fill" style={{ width: `${Math.round(concept.mastery * 100)}%` }} />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        )
      })}

      <section className="panel-card">
        <h2>Why it believes that</h2>
        <p className="view-sub">The last few things you did, and what each one was worth.</p>
        <table className="progress-table">
          <tbody>
            {(evidence.data ?? []).map((row) => (
              <tr key={row.id}>
                <td>{row.conceptId}</td>
                <td className="num">{row.evidenceType.toLowerCase().replace(/_/g, ' ')}</td>
                <td className={`num ${row.result === 'CORRECT' ? 'good' : 'bad'}`}>
                  {row.result.toLowerCase().replace(/_/g, ' ')}
                </td>
                <td className="num">
                  {row.masteryBefore.toFixed(2)} → {row.masteryAfter.toFixed(2)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  )
}
