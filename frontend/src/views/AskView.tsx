import { useRef, useState } from 'react'
import { api } from '../api/client'
import { ExampleCard } from '../components/CorpusExample'
import type { AskAnswer } from '../api/types'

const SUGGESTIONS = [
  'What is V7/V in C major?',
  'What is a Neapolitan sixth?',
  'Give me a Beethoven example of V/V',
  'Spell the C blues scale',
]

/**
 * Asking a question directly, and being shown what the answer was built from.
 *
 * <p>The tutor decides what to teach next; this does not. It is for the question you have
 * right now, and its point is the material underneath the answer: what the theory engine
 * computed, which published passages were used, and which real bars were found. An answer
 * you can check is worth more than one you have to trust.
 */
export function AskView() {
  const [question, setQuestion] = useState('')
  const [thread, setThread] = useState<AskAnswer[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Kept so a follow-up still means something: "and in F?" needs what came before.
  const conversation = useRef<string | undefined>(undefined)

  const submit = async (asked: string) => {
    const text = asked.trim()
    if (!text || busy) {
      return
    }
    setBusy(true)
    setError(null)
    try {
      const answer = await api.ask(text, conversation.current)
      conversation.current = answer.conversationId
      setThread((previous) => [...previous, answer])
      setQuestion('')
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'That question could not be answered.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="view ask-view">
      <header className="view-head">
        <div>
          <h1>Ask</h1>
          <p className="view-sub">
            A question, answered from the theory engine and the sources you have brought in.
            Everything the answer was built from is shown under it.
          </p>
        </div>
        {thread.length > 0 && (
          <button
            type="button"
            className="btn-ghost"
            onClick={() => {
              setThread([])
              conversation.current = undefined
            }}
          >
            Start again
          </button>
        )}
      </header>

      {thread.length === 0 && (
        <div className="ask-suggestions">
          {SUGGESTIONS.map((suggestion) => (
            <button key={suggestion} type="button" className="ask-chip" onClick={() => void submit(suggestion)}>
              {suggestion}
            </button>
          ))}
        </div>
      )}

      <div className="ask-thread">
        {thread.map((entry, index) => (
          <AnswerBlock key={`${entry.conversationId}-${index}`} entry={entry} />
        ))}
        {busy && <p className="ask-waiting">Looking it up…</p>}
        {error && (
          <p className="view-error" role="alert">
            {error}
          </p>
        )}
      </div>

      <form
        className="ask-form"
        onSubmit={(event) => {
          event.preventDefault()
          void submit(question)
        }}
      >
        <input
          value={question}
          onChange={(event) => setQuestion(event.target.value)}
          placeholder="Ask about a chord, a scale, a progression, or for a real example…"
          aria-label="Your question"
          maxLength={2000}
        />
        <button type="submit" className="btn-primary" disabled={busy || !question.trim()}>
          Ask
        </button>
      </form>
    </div>
  )
}

function AnswerBlock({ entry }: { entry: AskAnswer }) {
  return (
    <article className="ask-entry">
      <p className="ask-question">{entry.question}</p>
      <div className="ask-answer">
        <p>{entry.answer}</p>
        {entry.answeredWithoutAModel && (
          <p className="ask-note">
            Written from the material below rather than by a language model.
          </p>
        )}
      </div>

      {entry.computed.length > 0 && (
        <section className="ask-block">
          <h3>
            Computed <span className="ask-block-why">worked out here, not looked up</span>
          </h3>
          {entry.computed.map((fact) => (
            <p key={fact.operation} className="ask-computed">
              {fact.statement}
            </p>
          ))}
        </section>
      )}

      {entry.examples.length > 0 && (
        <section className="ask-block">
          <h3>
            In real music <span className="ask-block-why">bars from annotated scores</span>
          </h3>
          <div className="example-set">
            {entry.examples.map((example, index) => (
              <ExampleCard key={`${example.citation}-${index}`} example={example} />
            ))}
          </div>
        </section>
      )}

      {entry.corpusSearchedAndEmpty && (
        <p className="example-none">
          No verified example of that was found in the scores currently loaded. Nothing is
          invented to fill the gap.
        </p>
      )}

      {entry.passages.length > 0 && (
        <section className="ask-block">
          <h3>
            From the sources <span className="ask-block-why">quoted, not paraphrased</span>
          </h3>
          {entry.passages.map((passage) => (
            <figure key={passage.chunkId} className="ask-passage">
              <blockquote>{passage.excerpt}</blockquote>
              <figcaption>
                {passage.url ? (
                  <a href={passage.url} target="_blank" rel="noreferrer">
                    {passage.citation}
                  </a>
                ) : (
                  passage.citation
                )}
                {passage.license && <span className="ask-licence">{passage.license}</span>}
              </figcaption>
            </figure>
          ))}
        </section>
      )}
    </article>
  )
}
