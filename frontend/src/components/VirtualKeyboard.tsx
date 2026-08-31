import { useState } from 'react'
import { noteName } from '../state/tutorStore'

interface VirtualKeyboardProps {
  onSubmit: (notes: number[]) => void
  disabled: boolean
  /** MIDI number of the lowest key shown. 48 is C3. */
  from?: number
  octaves?: number
}

const BLACK_OFFSETS = new Set([1, 3, 6, 8, 10])

/**
 * An on-screen piano, so the keyboard exercises are answerable without a MIDI instrument.
 *
 * <p>It submits the same thing a real keyboard does — MIDI note numbers, in the order they
 * were struck — so evaluation is identical and nothing downstream knows the difference.
 * Order is kept because a scale is judged as a sequence; the lowest note still decides an
 * inversion, so chords can be clicked in any order.
 */
export function VirtualKeyboard({ onSubmit, disabled, from = 48, octaves = 2 }: VirtualKeyboardProps) {
  const [struck, setStruck] = useState<number[]>([])

  const keys = Array.from({ length: octaves * 12 + 1 }, (_, index) => from + index)
  const whites = keys.filter((midi) => !BLACK_OFFSETS.has(midi % 12))

  const toggle = (midi: number) =>
    setStruck((current) =>
      current.includes(midi) ? current.filter((note) => note !== midi) : [...current, midi],
    )

  const send = () => {
    if (struck.length > 0) {
      onSubmit(struck)
      setStruck([])
    }
  }

  const keyFor = (midi: number) => {
    const black = BLACK_OFFSETS.has(midi % 12)
    const name = noteName(midi)
    return (
      <button
        key={midi}
        type="button"
        className={`key ${black ? 'key-black' : 'key-white'}${struck.includes(midi) ? ' key-struck' : ''}`}
        style={black ? { left: `calc(${whites.filter((w) => w < midi).length} * var(--key-w) - var(--key-w) * 0.3)` } : undefined}
        aria-label={name}
        aria-pressed={struck.includes(midi)}
        disabled={disabled}
        onClick={() => toggle(midi)}
      >
        {!black && <span className="key-name">{name}</span>}
      </button>
    )
  }

  return (
    <div className="virtual-keyboard">
      <div className="vk-readout">
        <span className="vk-label">On-screen keyboard</span>
        <span className="vk-notes">
          {struck.length > 0 ? struck.map(noteName).join(' ') : 'click the keys you would play'}
        </span>
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
