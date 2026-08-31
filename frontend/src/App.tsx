import { useCallback, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from './api/client'
import { Composer } from './components/Composer'
import { Conversation } from './components/Conversation'
import { MasteryPanel } from './components/MasteryPanel'
import { MidiIndicator } from './components/MidiIndicator'
import { useMidi } from './midi/useMidi'
import { useTutorStore } from './state/tutorStore'

export default function App() {
  const { sessionId, entries, current, busy, error, start, send, play, clearError } =
    useTutorStore()

  // The panel refetches when the conversation moves on, rather than polling.
  const modelVersion = entries.length

  const { data: status } = useQuery({ queryKey: ['status'], queryFn: api.status })

  const awaitingKeyboard = Boolean(
    current?.expectsAnswer && current.answerMode === 'MIDI' && current.exerciseId,
  )

  const onPerformance = useCallback(
    (notes: number[]) => {
      // Ignore stray playing when nothing is waiting for it.
      if (awaitingKeyboard) {
        void play(notes)
      }
    },
    [awaitingKeyboard, play],
  )

  const midi = useMidi({ onPerformance })

  useEffect(() => {
    if (!sessionId) {
      void start()
    }
  }, [sessionId, start])

  const onSend = (text: string) => {
    void send(text)
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>
          Leading Tone<span className="wordmark-note">♮</span>
        </h1>
        <div className="header-right">
          {status && (
            <span className="narrator" title={status.narrator}>
              {status.languageModelAvailable ? 'model' : 'deterministic'}
            </span>
          )}
          <MidiIndicator devices={midi.devices} supported={midi.supported} error={midi.error} />
        </div>
      </header>

      {error && (
        <div className="banner" role="alert">
          {error}
          <button type="button" onClick={clearError}>
            dismiss
          </button>
        </div>
      )}

      <main className="app-main">
        <section className="column-conversation">
          <Conversation entries={entries} busy={busy} />
          <Composer
            onSend={onSend}
            onSubmitPlaying={midi.submitNow}
            onPlayNotes={(notes) => void play(notes)}
            disabled={busy || !sessionId}
            awaitingKeyboard={awaitingKeyboard}
            hasDevice={midi.connected}
            activeNotes={midi.activeNotes}
          />
        </section>
        <MasteryPanel version={modelVersion} rationale={current?.rationale} />
      </main>
    </div>
  )
}
