import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import { Composer } from '../components/Composer'
import { Conversation } from '../components/Conversation'
import { PracticeMenu } from '../components/PracticeMenu'
import type { ConversationEntry } from '../state/tutorStore'
import type { MidiDevice } from '../midi/MidiService'

interface PractiseViewProps {
  entries: ConversationEntry[]
  busy: boolean
  rationale?: string
  conceptName?: string
  onSend: (text: string) => void
  onPlayNotes: (notes: number[]) => void
  onSubmitPlaying: () => void
  awaitingKeyboard: boolean
  midi: { devices: MidiDevice[]; connected: boolean; activeNotes: number[] }
  onLeaveTopic: () => void
}

/** The tutor itself: one question at a time, with the piano to hand. */
export function PractiseView({
  entries,
  busy,
  rationale,
  conceptName,
  onSend,
  onPlayNotes,
  onSubmitPlaying,
  awaitingKeyboard,
  midi,
  onLeaveTopic,
}: PractiseViewProps) {
  const learner = useQuery({ queryKey: ['learner'], queryFn: api.learner })
  const focused = Boolean(learner.data?.focusConceptId ?? learner.data?.focusCategory)

  return (
    <div className="view view-practise">
      <header className="practise-head">
        <div>
          <h1>{conceptName ?? 'Practise'}</h1>
          {rationale && <p className="view-sub">{rationale}</p>}
        </div>
        <div className="practise-controls">
          <PracticeMenu current={learner.data?.preferredAnswerMode ?? null} />
          {focused && (
            <button type="button" className="btn-ghost" onClick={onLeaveTopic}>
              Let the tutor choose
            </button>
          )}
        </div>
      </header>

      <div className="practise-body">
        <Conversation entries={entries} busy={busy} />
        <Composer
          onSend={onSend}
          onSubmitPlaying={onSubmitPlaying}
          onPlayNotes={onPlayNotes}
          disabled={busy}
          awaitingKeyboard={awaitingKeyboard}
          hasDevice={midi.connected}
          activeNotes={midi.activeNotes}
        />
      </div>
    </div>
  )
}
