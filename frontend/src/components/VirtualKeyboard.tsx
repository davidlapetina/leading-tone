import { useState } from 'react'
import { play, strike } from '../midi/audio'
import { noteName } from '../state/tutorStore'

interface VirtualKeyboardProps {
  onSubmit: (notes: number[]) => void
  disabled: boolean
  /** MIDI number of the lowest key shown. 36 is C2. */
  from?: number
  octaves?: number
}

const BLACK_OFFSETS = new Set([1, 3, 6, 8, 10])

/**
 * An on-screen piano, so the keyboard questions are answerable without an instrument.
 *
 * It submits the same thing a real keyboard does — MIDI note numbers, in the order they
 * were struck — so evaluation is identical and nothing downstream knows the difference.
 * Order is kept because a scale is judged as a sequence; the lowest note still decides an
 * inversion, so a chord can be clicked in any order.
 */
export function VirtualKeyboard({ onSubmit, disabled, from = 36, octaves = 4 }: VirtualKeyboardProps) {
  const [struck, setStruck] = useState<number[]>([])

  const keys = Array.from({ length: octaves * 12 + 1 }, (_, index) => from + index)
  const whites = keys.filter((midi) => !BLACK_OFFSETS.has(midi % 12))

  const toggle = (midi: number) => {
    strike(midi)
    setStruck((current) =>
      current.includes(midi) ? current.filter((note) => note !== midi) : [...current, midi],
    )
  }

  const send = () => {
    if (struck.length > 0) {
      onSubmit(struck)
      setStruck([])
    }
  }

  const keyFor = (midi: number) => {
    const black = BLACK_OFFSETS.has(midi % 12)
    const name = noteName(midi)
    const isC = midi % 12 === 0
    return (
      <button
        key={midi}
        type="button"
        className={`key ${black ? 'key-black' : 'key-white'}${struck.includes(midi) ? ' key-struck' : ''}`}
        style={
          black
            ? { left: `calc(${whites.filter((w) => w < midi).length} * var(--key-w) - var(--key-w) * 0.32)` }
            : undefined
        }
        aria-label={name}
        aria-pressed={struck.includes(midi)}
        disabled={disabled}
        onClick={() => toggle(midi)}
      >
        {!black && <span className={`key-name${isC ? ' key-name-c' : ''}`}>{isC ? name : ''}</span>}
      </button>
    )
  }

  return (
    <div className="virtual-keyboard">
      <div className="vk-readout">
        <span className="vk-notes">
          {struck.length > 0 ? struck.map(noteName).join('  ') : 'Click the keys you would play'}
        </span>
        <button
          type="button"
          onClick={() => play(struck, 0)}
          disabled={disabled || struck.length === 0}
          title="Hear the notes together"
        >
          Hear it
        </button>
        <button type="button" onClick={() => setStruck([])} disabled={disabled || struck.length === 0}>
          Clear
        </button>
        <button type="button" className="vk-send" onClick={send} disabled={disabled || struck.length === 0}>
          Play it
        </button>
      </div>

      <div className="vk-keys" style={{ ['--key-count' as string]: whites.length }}>
        <div className="vk-whites">{whites.map(keyFor)}</div>
        <div className="vk-blacks">{keys.filter((midi) => BLACK_OFFSETS.has(midi % 12)).map(keyFor)}</div>
      </div>
    </div>
  )
}
