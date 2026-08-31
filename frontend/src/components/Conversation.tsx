import { useEffect, useRef } from 'react'
import type { ConversationEntry } from '../state/tutorStore'
import { Score } from './Score'

const VERDICT_LABEL: Record<string, string> = {
  CORRECT: 'correct',
  PARTIALLY_CORRECT: 'nearly',
  INCORRECT: 'not quite',
  SKIPPED: 'nothing played',
}

/**
 * The exercise is generated in Java and is the thing actually being marked, so it is
 * shown in its own right whenever the tutor's words do not already contain it. A model
 * that paraphrases the question away cannot leave the learner guessing.
 */
function showsSeparateQuestion(entry: ConversationEntry): boolean {
  if (entry.role !== 'tutor' || !entry.exercisePrompt) {
    return false
  }
  const normalise = (text: string) => text.toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim()
  return !normalise(entry.text).includes(normalise(entry.exercisePrompt))
}

export function Conversation({ entries, busy }: { entries: ConversationEntry[]; busy: boolean }) {
  const bottom = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: 'smooth' })
  }, [entries.length, busy])

  return (
    <div className="conversation">
      {entries.map((entry) => (
        <article
          key={entry.id}
          className={`turn turn-${entry.role}`}
          data-exercise-id={entry.exerciseId ?? undefined}
        >
          {entry.attempt && (
            <p
              className={`verdict verdict-${entry.attempt.outcome.result.toLowerCase()}`}
              title={entry.attempt.outcome.feedback ?? undefined}
            >
              {VERDICT_LABEL[entry.attempt.outcome.result] ?? entry.attempt.outcome.result}
              {entry.attempt.outcome.feedback ? ` — ${entry.attempt.outcome.feedback}` : ''}
            </p>
          )}
          <p className="turn-text">{entry.text}</p>
          {showsSeparateQuestion(entry) && (
            <div className="ask">
              <p className="ask-text">{entry.exercisePrompt}</p>
            </div>
          )}
          {entry.notationAbc && <Score abc={entry.notationAbc} />}
          {entry.role === 'tutor' && entry.action && (
            <p className="turn-meta" title={entry.rationale}>
              <span>{entry.action.toLowerCase().replace(/_/g, ' ')}</span>
              {entry.taskKind && <span className="turn-kind">{entry.taskKind}</span>}
            </p>
          )}
        </article>
      ))}
      {busy && <p className="thinking">…</p>}
      <div ref={bottom} />
    </div>
  )
}
