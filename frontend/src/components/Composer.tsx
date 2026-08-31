import { useState, type FormEvent } from 'react'
import { noteName } from '../state/tutorStore'
import { VirtualKeyboard } from './VirtualKeyboard'

interface ComposerProps {
  onSend: (text: string) => void
  onSubmitPlaying: () => void
  onPlayNotes: (notes: number[]) => void
  disabled: boolean
  awaitingKeyboard: boolean
  hasDevice: boolean
  activeNotes: number[]
}

export function Composer({
  onSend,
  onSubmitPlaying,
  onPlayNotes,
  disabled,
  awaitingKeyboard,
  hasDevice,
  activeNotes,
}: ComposerProps) {
  const [text, setText] = useState('')
  // null until the learner says either way, so the default can follow whether a question
  // is actually waiting to be played rather than being frozen at first render.
  const [keysOpen, setKeysOpen] = useState<boolean | null>(null)
  const showKeys = keysOpen ?? (awaitingKeyboard && !hasDevice)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!text.trim() || disabled) {
      return
    }
    onSend(text)
    setText('')
  }

  return (
    <form className="composer" onSubmit={submit}>
      {awaitingKeyboard && (
        <div className="keyboard-prompt">
          <span className="piano">
            {hasDevice ? 'Waiting for the piano…' : 'This one is played'}
          </span>
          {hasDevice && (
            <>
              <span className="played">
                {activeNotes.length > 0 ? activeNotes.map(noteName).join(' ') : '—'}
              </span>
              <button type="button" onClick={onSubmitPlaying} disabled={disabled}>
                Send what I played
              </button>
            </>
          )}
        </div>
      )}

      {showKeys && <VirtualKeyboard onSubmit={onPlayNotes} disabled={disabled} />}

      <div className="composer-row">
        {/* Always reachable, so the piano is never something you have to discover. */}
        <button
          type="button"
          className={`keys-toggle${showKeys ? ' keys-toggle-on' : ''}`}
          aria-pressed={showKeys}
          title={showKeys ? 'Hide the on-screen piano' : 'Show the on-screen piano'}
          onClick={() => setKeysOpen(!showKeys)}
        >
          Piano
        </button>
        <input
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder={awaitingKeyboard ? 'Or type the answer…' : 'Answer, or ask about anything…'}
          disabled={disabled}
          aria-label="Message the teacher"
        />
        <button type="submit" disabled={disabled || !text.trim()}>
          Send
        </button>
      </div>
    </form>
  )
}
