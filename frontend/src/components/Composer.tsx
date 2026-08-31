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
  // null until the learner says either way, so the default can follow whether an
  // instrument is actually connected rather than being frozen at first render.
  const [keysOpen, setKeysOpen] = useState<boolean | null>(null)
  const showKeys = keysOpen ?? !hasDevice

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
        <>
          <div className="keyboard-prompt">
            <span className="piano">
              {hasDevice ? 'Waiting for the piano…' : 'No instrument connected'}
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
            <button type="button" onClick={() => setKeysOpen(!showKeys)}>
              {showKeys ? 'Hide keys' : 'Use on-screen keys'}
            </button>
          </div>
          {showKeys && <VirtualKeyboard onSubmit={onPlayNotes} disabled={disabled} />}
        </>
      )}
      <div className="composer-row">
        <input
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder={awaitingKeyboard ? 'Or type the answer…' : 'Ask something…'}
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
